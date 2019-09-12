package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.common.firebase.models.ImageState

fun ImageState.toImageLoader() = ImageLoader(this)