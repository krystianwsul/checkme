package com.krystianwsul.checkme.viewmodels

abstract class DomainData {

    companion object {

        private var sDataId = 1

        private val nextId get() = sDataId++
    }

    val dataId = nextId
}