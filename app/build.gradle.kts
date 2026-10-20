import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import javax.xml.parsers.DocumentBuilderFactory
import java.util.zip.ZipFile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val signingPassword = providers.environmentVariable("XGIMI_SIGNING_PASSPHRASE").orNull
val signingKeystore = rootProject.file("build/signing/release.p12")

android {
    namespace = "dev.projectortv.app"
    compileSdk = 35

    signingConfigs {
        if (!signingPassword.isNullOrBlank() && signingKeystore.isFile) {
            create("secureRelease") {
                storeFile = signingKeystore
                storePassword = signingPassword
                keyAlias = "projector-release"
                keyPassword = signingPassword
                storeType = "PKCS12"
            }
        }
    }

    defaultConfig {
        applicationId = "dev.projectortv.client"
        minSdk = 28
        targetSdk = 35
        versionCode = 6
        versionName = "0.6.0-device-check"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = false
            if (!signingPassword.isNullOrBlank() && signingKeystore.isFile) {
                signingConfig = signingConfigs.getByName("secureRelease")
            }
        }
        debug { isDebuggable = true }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isReturnDefaultValues = true }
    lint { abortOnError = true; checkReleaseBuilds = true }
    sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/licenseAssets"))
}

dependencies {
    implementation("androidx.leanback:leanback:1.2.0")
    implementation("org.jsoup:jsoup:1.18.3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
}

// Audit actual resolved runtime dependencies, including transitive modules.
val collectRuntimeLicenses by tasks.registering {
    val output = layout.buildDirectory.dir("generated/licenseAssets/licenses")
    outputs.dir(output)
    outputs.upToDateWhen { false }
    doLast {
        val runtime = configurations.getByName("releaseRuntimeClasspath")
        val ids = runtime.incoming.resolutionResult.allComponents.mapNotNull { it.id as? ModuleComponentIdentifier }
        val result = dependencies.createArtifactResolutionQuery().forComponents(ids)
            .withArtifacts(MavenModule::class.java, MavenPomArtifact::class.java).execute()
        val directory = output.get().asFile.apply { mkdirs() }
        val report = StringBuilder("Resolved releaseRuntimeClasspath – original POM license declarations\n\n")
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }
        check(result.components.size == result.resolvedComponents.size) { "Dependency license metadata could not be resolved" }
        result.resolvedComponents.sortedBy { it.id.displayName }.forEach { component ->
            val poms = component.getArtifacts(MavenPomArtifact::class.java).filterIsInstance<ResolvedArtifactResult>()
            check(poms.isNotEmpty()) { "Missing POM: ${component.id}" }
            poms.forEach { artifact ->
                val document = factory.newDocumentBuilder().parse(artifact.file)
                val licenses = document.getElementsByTagName("license")
                check(licenses.length > 0) { "Missing license declaration: ${component.id}" }
                report.append(component.id.displayName).append('\n')
                for (index in 0 until licenses.length) {
                    val declared = licenses.item(index).textContent.trim().replace(Regex("\\s+"), " ")
                    check(declared.contains("Apache", true) || declared.contains("MIT", true) || declared.contains("BSD", true)) {
                        "Review additional runtime license before distribution: ${component.id}: $declared"
                    }
                    report.append("  ").append(declared).append('\n')
                }
            }
        }
        runtime.resolvedConfiguration.resolvedArtifacts.sortedBy { it.moduleVersion.id.toString() }.forEach { artifact ->
            if (artifact.file.extension in setOf("jar", "aar")) ZipFile(artifact.file).use { zip ->
                zip.entries().asSequence().filter { !it.isDirectory &&
                    (it.name.contains("license",true) || it.name.contains("notice",true)) && it.size > 0 && it.size < 200000
                }.forEach { entry ->
                    report.append("\nEmbedded notice: ${artifact.moduleVersion.id}/${entry.name}\n")
                    report.append(zip.getInputStream(entry).bufferedReader().use { it.readText() }).append('\n')
                }
            }
        }
        directory.resolve("DEPENDENCIES.txt").writeText(report.toString())
        println("Runtime licenses checked for ${ids.size} resolved modules")
    }
}
tasks.named("preBuild") { dependsOn(collectRuntimeLicenses) }
