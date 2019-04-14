package com.krystianwsul.checkme.firebase

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.krystianwsul.checkme.GlideApp
import com.krystianwsul.checkme.upload.Uploader
import java.io.Serializable

sealed class ImageState : Serializable {

    abstract fun load(imageView: ImageView)

    class Local(val uuid: String) : ImageState() {

        override fun load(imageView: ImageView) {
            Glide.with(imageView)
                    .load(Uploader.getPath(this))
                    .into(imageView)
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

        override fun load(imageView: ImageView) {
            GlideApp.with(imageView)
                    .load(Uploader.getReference(this))
                    .into(imageView)
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

    object Uploading : ImageState() {

        override fun load(imageView: ImageView) = Unit
    }
}