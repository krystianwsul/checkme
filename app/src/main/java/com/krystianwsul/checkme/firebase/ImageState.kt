package com.krystianwsul.checkme.firebase

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.krystianwsul.checkme.GlideApp
import com.krystianwsul.checkme.upload.Uploader
import java.io.Serializable

sealed class ImageState : Serializable {

    abstract fun load(imageView: ImageView)

    data class Local(val uuid: String) : ImageState() {

        override fun load(imageView: ImageView) {
            Glide.with(imageView)
                    .load(Uploader.getPath(this))
                    .into(imageView)
        }
    }

    data class Remote(val uuid: String) : ImageState() {

        override fun load(imageView: ImageView) {
            GlideApp.with(imageView)
                    .load(Uploader.getReference(this))
                    .into(imageView)
        }
    }

    object Uploading : ImageState() {

        override fun load(imageView: ImageView) = Unit
    }
}