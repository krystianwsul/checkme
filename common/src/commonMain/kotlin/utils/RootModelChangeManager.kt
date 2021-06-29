package com.krystianwsul.common.utils

import com.krystianwsul.common.firebase.models.cache.InvalidatableManager

class RootModelChangeManager {

    val invalidatableManager = InvalidatableManager()

    fun invalidateExistingInstances() = invalidatableManager.invalidate()

    fun invalidateRootModels() = invalidateExistingInstances()
}