package com.krystianwsul.common.firebase.records

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty


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

    protected fun <T> setProperty(innerProperty: KMutableProperty0<T>, value: T, path: String = key) {
        if (innerProperty.get() == value)
            return

        innerProperty.set(value)
        addValue("$path/${innerProperty.name}", value)
    }

    protected inner class Committer<T>(
            private val innerProperty: KMutableProperty0<T>,
            private val path: String? = null
    ) : ReadWriteProperty<RemoteRecord, T> {

        override fun getValue(thisRef: RemoteRecord, property: KProperty<*>) = innerProperty.get()

        override fun setValue(
                thisRef: RemoteRecord,
                property: KProperty<*>,
                value: T
        ) = setProperty(innerProperty, value, path ?: key)
    }
}
