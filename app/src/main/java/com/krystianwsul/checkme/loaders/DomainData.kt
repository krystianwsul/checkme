package com.krystianwsul.checkme.loaders

abstract class DomainData {

    companion object {

        private var sDataId = 1

        private val nextId get() = sDataId++
    }

    val dataId: Int

    init {
        dataId = nextId
    }
}