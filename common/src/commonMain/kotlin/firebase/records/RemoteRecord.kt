package com.krystianwsul.common.firebase.records


abstract class RemoteRecord(create: Boolean) {

    protected var shouldDelete = false

    var update = if (create) null else mutableMapOf<String, Any?>()

    abstract val key: String

    abstract val createObject: Any

    open val children = listOf<RemoteRecord>()

    protected abstract fun deleteFromParent()

    fun getValues(values: MutableMap<String, Any?>): Boolean {
        if (shouldDelete) {
            check(update != null)

            values[key] = null
            shouldDelete = false

            deleteFromParent()

            return true
        } else {
            if (update == null) {
                values[key] = createObject

                setCreated()
            } else {
                if (update!!.isNotEmpty()) {
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
        check(!shouldDelete)

        update?.put(key, obj)
    }

    fun delete() {
        check(!shouldDelete)
        check(update != null)

        shouldDelete = true
    }
}
