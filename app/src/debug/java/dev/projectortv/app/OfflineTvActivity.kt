package dev.projectortv.app

/** Instrumentation host. Excluded from release APK; never connects to BingeBang. */
class OfflineTvActivity : TvFacadeActivity() {
    override val loadWebsiteOnStart = false
}
