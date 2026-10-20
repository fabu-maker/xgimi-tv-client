/* Adapted from SmartTube VideoCardPresenter.java.
 * Copyright (c) 2020-present yuliskov. MIT; see assets/licenses/SmartTube-MIT.txt.
 * Source commit b0b2b5f4113bbf9ab2fab252d287f0b711ff280f.
 * Changes: Kotlin, first-party SiteTitle, ImageCardView, existing bounded image loader;
 * removed YouTube badges, previews, ClickbaitRemover, settings and Glide dependencies.
 */
package dev.projectortv.tv

import android.graphics.Color
import android.view.ViewGroup
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import dev.projectortv.app.SitePageBridge

class TvCardPresenter(private val posters: PosterLoader) : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val card = object : ImageCardView(parent.context) {
            override fun setSelected(selected: Boolean) {
                setInfoAreaBackgroundColor(if (selected) Color.rgb(14, 91, 110) else Color.rgb(23, 37, 54))
                super.setSelected(selected)
            }
        }
        card.isFocusable = true
        card.isFocusableInTouchMode = true
        card.setBackgroundColor(Color.rgb(23, 37, 54))
        card.setInfoAreaBackgroundColor(Color.rgb(23, 37, 54))
        val density = parent.resources.displayMetrics.density
        card.setMainImageDimensions((146 * density).toInt(), (215 * density).toInt())
        return ViewHolder(card)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val title = item as SitePageBridge.SiteTitle
        val card = viewHolder.view as ImageCardView
        card.titleText = title.name
        card.contentText = title.kind
        card.contentDescription = "${title.kind} ${title.name}"
        posters.into(card.mainImageView, title.poster)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val card = viewHolder.view as ImageCardView
        card.mainImageView.tag = null // reject a late download for a recycled card
        card.badgeImage = null
        card.mainImage = null
    }
}
