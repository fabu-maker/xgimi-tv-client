import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import javax.xml.parsers.DocumentBuilderFactory
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

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
        fun readLicenses(pom: java.io.File, depth: Int = 0): List<String> {
            check(depth < 8) { "POM parent chain too long" }
            val document = factory.newDocumentBuilder().parse(pom)
            val declared = document.getElementsByTagName("license")
            if (declared.length > 0) return (0 until declared.length).map {
                declared.item(it).textContent.trim().replace(Regex("\\s+"), " ")
            }
            val parent = document.getElementsByTagName("parent").item(0) as? org.w3c.dom.Element
                ?: error("No license or parent in ${pom.name}")
            fun field(name: String) = parent.getElementsByTagName(name).item(0)?.textContent?.trim()
                ?: error("Missing parent $name in ${pom.name}")
            val coordinate = "${field("groupId")}:${field("artifactId")}:${field("version")}"
            val configuration = configurations.detachedConfiguration(dependencies.create("$coordinate@pom"))
            configuration.isTransitive = false
            return readLicenses(configuration.singleFile, depth + 1).map { "$it [inherited from $coordinate]" }
        }
        result.resolvedComponents.sortedBy { it.id.displayName }.forEach { component ->
            val poms = component.getArtifacts(MavenPomArtifact::class.java).filterIsInstance<ResolvedArtifactResult>()
            check(poms.isNotEmpty()) { "Missing POM: ${component.id}" }
            poms.forEach { artifact ->
                val licenses = readLicenses(artifact.file)
                report.append(component.id.displayName).append('\n')
                for (declared in licenses) {
                    check(declared.contains("Apache", true) || declared.contains("MIT", true) || declared.contains("BSD", true)) {
                        "Review additional runtime license before distribution: ${component.id}: $declared"
                    }
                    report.append("  ").append(declared).append('\n')
                }
            }
        }
        fun isNotice(name: String) = Regex("(?i)^(license|notice|copying|copyright)([._-].*)?$")
            .matches(name.substringAfterLast('/'))
        fun appendNotice(label: String, bytes: ByteArray) {
            check(bytes.size <= 200000) { "Review unusually large license notice: $label" }
            report.append("\nEmbedded notice: $label\n").append(bytes.toString(Charsets.UTF_8)).append('\n')
        }
        runtime.resolvedConfiguration.resolvedArtifacts.sortedBy { it.moduleVersion.id.toString() }.forEach { artifact ->
            if (artifact.file.extension in setOf("jar", "aar")) ZipFile(artifact.file).use { zip ->
                zip.entries().asSequence().filter { !it.isDirectory }.forEach { entry ->
                    val label="${artifact.moduleVersion.id}/${entry.name}"
                    if (isNotice(entry.name)) zip.getInputStream(entry).use { appendNotice(label,it.readNBytes(200001)) }
                    // Android AARs also carry notices inside classes.jar and libs/*.jar.
                    if (artifact.file.extension == "aar" && entry.name.endsWith(".jar")) {
                        ZipInputStream(zip.getInputStream(entry)).use { nested ->
                            var child=nested.nextEntry
                            while(child != null) {
                                if(!child.isDirectory && isNotice(child.name)) appendNotice("$label/${child.name}",nested.readNBytes(200001))
                                nested.closeEntry()
                                child=nested.nextEntry
                            }
                        }
                    }
                }
            }
        }
        directory.resolve("DEPENDENCIES.txt").writeText(report.toString())
        println("Runtime licenses checked for ${ids.size} resolved modules")
    }
}
tasks.named("preBuild") { dependsOn(collectRuntimeLicenses) }
