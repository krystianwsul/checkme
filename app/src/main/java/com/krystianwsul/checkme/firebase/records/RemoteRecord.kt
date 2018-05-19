package com.krystianwsul.checkme.firebase.records

import android.util.Log

import junit.framework.Assert

abstract class RemoteRecord(protected val create: Boolean) {

    protected var delete = false
    protected var deleted = false

    protected var created = false

    protected val update = if (create) null else mutableMapOf<String, Any?>()
    protected var updated = false

    protected abstract val key: String

    protected abstract val createObject: Any

    open fun getValues(values: MutableMap<String, Any?>) {
        Assert.assertTrue(!deleted)
        Assert.assertTrue(!created)
        Assert.assertTrue(!updated)

        if (delete) {
            Log.e("asdf", "RemoteRecord.getValues deleting " + this)

            Assert.assertTrue(!create)
            Assert.assertTrue(update != null)

            deleted = true
            values[key] = null
        } else if (create) {
            Log.e("asdf", "RemoteRecord.getValues creating " + this)

            Assert.assertTrue(update == null)

            created = true
            values[key] = createObject
        } else {
            Assert.assertTrue(update != null)

            if (!update!!.isEmpty()) {
                Log.e("asdf", "RemoteRecord.getValues updating " + this)

                updated = true
                values.putAll(update)
            }
        }
    }

    protected fun addValue(key: String, obj: Any?) {
        Assert.assertTrue(!delete)
        Assert.assertTrue(!deleted)
        Assert.assertTrue(!created)
        Assert.assertTrue(!updated)

        if (create) {
            Assert.assertTrue(update == null)
        } else {
            Assert.assertTrue(update != null)

            update!![key] = obj
        }
    }

    fun delete() {
        Assert.assertTrue(!deleted)
        Assert.assertTrue(!updated)
        Assert.assertTrue(!created)
        Assert.assertTrue(!delete)
        Assert.assertTrue(!create)
        Assert.assertTrue(update != null)

        delete = true
    }
}
