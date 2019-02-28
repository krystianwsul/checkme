package com.krystianwsul.checkme.viewmodels

abstract class DomainData {

    companion object {

        private var dataId = 1

        private val nextId get() = dataId++
    }

    val dataId = nextId
}