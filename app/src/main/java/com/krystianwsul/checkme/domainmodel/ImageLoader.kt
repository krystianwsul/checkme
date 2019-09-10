package com.krystianwsul.checkme.domainmodel

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.krystianwsul.checkme.GlideApp
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.firebase.models.ImageState
import com.krystianwsul.checkme.upload.Uploader
import com.krystianwsul.checkme.utils.circle

class ImageLoader(val imageState: ImageState) {

    companion object {

        fun load(imageView: ImageView, uuid: String, circle: Boolean = false) {
            GlideApp.with(imageView)
                    .load(Uploader.getReference(uuid))
                    .circle(circle)
                    .into(imageView)
        }
    }

    fun load(imageView: ImageView, circle: Boolean = false) {
        when (imageState) {
            is ImageState.Local -> {
                Uploader.getPath(imageState)?.let {
                    Glide.with(imageView)
                            .load(it)
                            .circle(circle)
                            .into(imageView)
                } ?: load(imageView, imageState.uuid)
            }
            is ImageState.Remote -> load(imageView, imageState.uuid, circle)
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