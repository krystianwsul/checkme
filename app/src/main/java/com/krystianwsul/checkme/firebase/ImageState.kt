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
            Uploader.getPath(this)?.let {
                Glide.with(imageView)
                        .load(it)
                        .into(imageView)
            } ?: Remote.load(imageView, uuid)
        }

        override fun hashCode() = uuid.hashCode()

        override fun equals(other: Any?): Boolean {
            if (other === this)
                return true

            return if (other is ImageState) {
                when (other) {
                    is Local -> uuid == other.uuid
                    is Remote -> uuid == other.uuid
                    is Uploading -> false
                }
            } else {
                false
            }
        }
    }

    data class Remote(val uuid: String) : ImageState() {

        companion object {

            fun load(imageView: ImageView, uuid: String) {
                GlideApp.with(imageView)
                        .load(Uploader.getReference(uuid))
                        .into(imageView)
            }
        }

        override fun load(imageView: ImageView) = load(imageView, uuid)

        override fun hashCode() = uuid.hashCode()

        override fun equals(other: Any?): Boolean {
            if (other === this)
                return true

            return if (other is ImageState) {
                when (other) {
                    is Local -> uuid == other.uuid
                    is Remote -> uuid == other.uuid
                    is Uploading -> false
                }
            } else {
                false
            }
        }
    }

    object Uploading : ImageState() {

        override fun load(imageView: ImageView) = Unit
    }
}