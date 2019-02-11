package com.krystianwsul.checkme.firebase.records

import android.util.Log


abstract class RemoteRecord(create: Boolean) {

    protected var delete = false

    var update = if (create) null else mutableMapOf<String, Any?>()

    abstract val key: String

    abstract val createObject: Any

    open val children = listOf<RemoteRecord>()

    protected abstract fun deleteFromParent()

    fun getValues(values: MutableMap<String, Any?>): Boolean {
        if (delete) {
            Log.e("asdf", "RemoteRecord.getValues deleting $this")

            check(update != null)

            values[key] = null
            delete = false

            deleteFromParent()

            return true
        } else {
            if (update == null) {
                Log.e("asdf", "RemoteRecord.getValues creating $this")

                values[key] = createObject

                setCreated()
            } else {
                if (update!!.isNotEmpty()) {
                    Log.e("asdf", "RemoteRecord.getValues updating $this")

                    values.putAll(update!!)

                    update = mutableMapOf()
                }

                children.forEach { it.getValues(values) }
            }

            return false
        }
    }

    private fun setCreated() {
        check(update == null)

        update = mutableMapOf()

        children.forEach { it.setCreated() }
    }

    protected fun addValue(key: String, obj: Any?) {
        check(!delete)

        update?.put(key, obj)
    }

    fun delete() {
        check(!delete)
        check(update != null)

        delete = true
    }
}
