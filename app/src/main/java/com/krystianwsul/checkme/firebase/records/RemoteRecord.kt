package com.krystianwsul.checkme.firebase.records

import android.util.Log


abstract class RemoteRecord(create: Boolean) {

    protected var delete = false

    protected var update = if (create) null else mutableMapOf<String, Any?>()

    abstract val key: String

    abstract val createObject: Any

    open fun getValues(values: MutableMap<String, Any?>) {
        if (delete) {
            Log.e("asdf", "RemoteRecord.getValues deleting " + this)

            check(update != null)

            values[key] = null
            delete = false
        } else {
            if (update == null) {
                Log.e("asdf", "RemoteRecord.getValues creating " + this)

                check(update == null)

                values[key] = createObject
            } else if (update!!.isNotEmpty()) {
                Log.e("asdf", "RemoteRecord.getValues updating " + this)

                values.putAll(update!!)
            }

            update = mutableMapOf()
        }
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
