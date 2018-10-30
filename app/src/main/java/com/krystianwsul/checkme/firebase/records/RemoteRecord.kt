package com.krystianwsul.checkme.firebase.records

import android.util.Log



abstract class RemoteRecord(protected val create: Boolean) {

    protected var delete = false
    protected var deleted = false

    protected var created = false

    protected val update = if (create) null else mutableMapOf<String, Any?>()
    protected var updated = false

    abstract val key: String

    abstract val createObject: Any

    open fun getValues(values: MutableMap<String, Any?>) {
        check(!deleted)
        check(!created)
        check(!updated)

        if (delete) {
            Log.e("asdf", "RemoteRecord.getValues deleting " + this)

            check(!create)
            check(update != null)

            deleted = true
            values[key] = null
        } else if (create) {
            Log.e("asdf", "RemoteRecord.getValues creating " + this)

            check(update == null)

            created = true
            values[key] = createObject
        } else {
            if (update!!.isNotEmpty()) {
                Log.e("asdf", "RemoteRecord.getValues updating " + this)

                updated = true
                values.putAll(update)
            }
        }
    }

    protected fun addValue(key: String, obj: Any?) {
        check(!delete)
        check(!deleted)
        check(!created)
        check(!updated)

        if (create) {
            check(update == null)
        } else {
            update!![key] = obj
        }
    }

    fun delete() {
        check(!deleted)
        check(!updated)
        check(!created)
        check(!delete)
        check(!create)
        check(update != null)

        delete = true
    }
}
