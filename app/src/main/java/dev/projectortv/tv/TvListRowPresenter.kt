/* Adapted from SmartTube CustomListRowPresenter.java and ViewUtil.java.
 * Copyright (c) 2020-present yuliskov. MIT; see assets/licenses/SmartTube-MIT.txt.
 * Source commit b0b2b5f4113bbf9ab2fab252d287f0b711ff280f.
 * Changes: Kotlin, inline the three TV focus constants, official AndroidX Leanback.
 */
package dev.projectortv.tv

import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.ListRowPresenter

class TvListRowPresenter : ListRowPresenter(FocusHighlight.ZOOM_FACTOR_SMALL, false) {
    init { setSelectEffectEnabled(false) }
}
