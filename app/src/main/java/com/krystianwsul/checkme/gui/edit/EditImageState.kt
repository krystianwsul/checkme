package com.krystianwsul.checkme.gui.edit

import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.krystianwsul.checkme.domainmodel.toImageLoader
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import java.io.File
import java.io.Serializable

sealed class EditImageState : Serializable {

    open val dontOverwrite = false

    open val loader: ((ImageView) -> Any)? = null

    open val writeImagePath: NullableWrapper<Pair<String, Uri>>? = null

    object None : EditImageState()

    data class Existing(val imageState: com.krystianwsul.common.firebase.models.ImageState) : EditImageState() {

        override val loader: (ImageView) -> Unit get() = { imageState.toImageLoader().load(it, false) }
    }

    object Removed : EditImageState() {

        override val dontOverwrite = true

        override val writeImagePath = NullableWrapper<Pair<String, Uri>>(null)
    }

    data class Selected(val path: String, val uri: String) : EditImageState() {

        constructor(file: File) : this(file.absolutePath, file.toURI().toString())

        override val dontOverwrite = true

        override val loader
            get() = { imageView: ImageView ->
                Glide.with(imageView)
                        .load(path)
                        .into(imageView)
            }

        override val writeImagePath get() = NullableWrapper(Pair(path, Uri.parse(uri)))
    }
}