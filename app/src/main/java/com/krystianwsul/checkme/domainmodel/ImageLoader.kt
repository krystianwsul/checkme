package com.krystianwsul.checkme.domainmodel

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.krystianwsul.checkme.GlideApp
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.upload.Uploader
import com.krystianwsul.checkme.utils.circle
import com.krystianwsul.common.firebase.models.ImageState

class ImageLoader(val imageState: ImageState) {

    companion object {

        fun loadFromFirebase(
                imageView: ImageView,
                uuid: String,
                circle: Boolean = false,
                applyListener: RequestBuilder<Drawable>.() -> RequestBuilder<Drawable> = { this },
        ) {
            GlideApp.with(imageView)
                    .load(Uploader.getReference(uuid))
                    .circle(circle)
                    .applyListener()
                    .into(imageView)
        }
    }

    fun load(imageView: ImageView, circle: Boolean = false, onSuccess: (() -> Unit)? = null) {
        val applyListener: RequestBuilder<Drawable>.() -> RequestBuilder<Drawable> = {
            onSuccess?.let {
                listener(object : RequestListener<Drawable> {

                    override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean,
                    ): Boolean {
                        onSuccess.invoke()

                        return false
                    }

                    override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean,
                    ) = false
                })
            }
                    ?: this
        }

        when (imageState) {
            is ImageState.Local -> {
                Uploader.getPath(imageState)?.let {
                    Glide.with(imageView)
                            .load(it)
                            .circle(circle)
                            .applyListener()
                            .into(imageView)
                } ?: loadFromFirebase(imageView, imageState.uuid, circle, applyListener)
            }
            is ImageState.Remote -> loadFromFirebase(imageView, imageState.uuid, circle, applyListener)
            ImageState.Uploading -> Unit
        }
    }

    val requestBuilder
        get() = when (imageState) {
            is ImageState.Local -> Uploader.getPath(imageState)?.let {
                Glide.with(MyApplication.instance)
                        .asBitmap()
                        .load(it)
            }
            is ImageState.Remote -> GlideApp.with(MyApplication.instance)
                    .asBitmap()
                    .load(Uploader.getReference(imageState.uuid))
            is ImageState.Uploading -> null
        }
}