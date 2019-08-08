package com.krystianwsul.checkme.firebase

import android.graphics.Bitmap
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.krystianwsul.checkme.GlideApp
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.upload.Uploader
import com.krystianwsul.checkme.utils.circle
import java.io.Serializable

sealed class ImageState : Serializable {

    abstract fun load(imageView: ImageView, circle: Boolean = false)

    abstract val requestBuilder: RequestBuilder<Bitmap>?

    abstract val uuid: String?

    data class Local(override val uuid: String) : ImageState() {

        override fun load(imageView: ImageView, circle: Boolean) {
            Uploader.getPath(this)?.let {
                Glide.with(imageView)
                        .load(it)
                        .circle(circle)
                        .into(imageView)
            } ?: Remote.load(imageView, uuid)
        }

        override val requestBuilder
            get() = Uploader.getPath(this)?.let {
                Glide.with(MyApplication.instance)
                        .asBitmap()
                        .load(it)
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

    data class Remote(override val uuid: String) : ImageState() {

        companion object {

            fun load(imageView: ImageView, uuid: String, circle: Boolean = false) {
                GlideApp.with(imageView)
                        .load(Uploader.getReference(uuid))
                        .circle(circle)
                        .into(imageView)
            }
        }

        override fun load(imageView: ImageView, circle: Boolean) = load(imageView, uuid, circle)

        override val requestBuilder
            get() = GlideApp.with(MyApplication.instance)
                    .asBitmap()
                    .load(Uploader.getReference(uuid))

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

        override fun load(imageView: ImageView, circle: Boolean) = Unit

        override val requestBuilder: RequestBuilder<Bitmap>? get() = null

        override val uuid: String? = null
    }
}