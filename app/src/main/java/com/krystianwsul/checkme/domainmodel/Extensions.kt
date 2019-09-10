package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.firebase.models.ImageState

fun ImageState.toImageLoader() = ImageLoader(this)