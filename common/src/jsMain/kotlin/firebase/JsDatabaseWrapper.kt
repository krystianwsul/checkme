package com.krystianwsul.common.firebase

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.json.users.ProjectOrdinalEntryJson
import com.krystianwsul.common.firebase.json.users.UserWrapper
import json
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToDynamic

class JsDatabaseWrapper(admin: dynamic, root: String) : DatabaseWrapper() {

    private val rootReference = admin.database().ref(root)

    override fun getNewId(path: String) = rootReference.child(path)
            .push()
            .key as String

    override fun update(values: Map<String, Any?>, callback: DatabaseCallback) {
        val dynamicValues: dynamic = object {}

        values.forEach {
            val value = it.value.let {
                if (it is Map<*, *>) {
                    val map: dynamic = object {}

                    it.forEach { (key, value) ->
                        map[key as String] = json.encodeToDynamic(value as ProjectOrdinalEntryJson)
                    }

                    map
                } else {
                    it
                }
            }

            dynamicValues[it.key] = value
        }

        rootReference.update(dynamicValues) { error ->
            callback("error: " + error?.toString(), error == null, null)
        }
    }

    fun getPrivateProjects(callback: (Map<String, PrivateProjectJson>) -> Unit) {
        rootReference.child(PRIVATE_PROJECTS_KEY).once("value") { snapshot ->
            callback(parse(PrivateProjects.serializer(), object {

                @Suppress("unused")
                val privateProjectJsons = snapshot
            }).privateProjectJsons ?: mapOf())
        }
    }

    @Serializable
    private class PrivateProjects(val privateProjectJsons: Map<String, PrivateProjectJson>? = null)

    fun getSharedProjects(callback: (Map<String, JsonWrapper>) -> Unit) {
        rootReference.child(RECORDS_KEY).once("value") { snapshot ->
            callback(parse(SharedProjects.serializer(), object {

                @Suppress("unused")
                val jsonWrappers = snapshot
            }).jsonWrappers ?: mapOf())
        }
    }

    @Serializable
    private class SharedProjects(val jsonWrappers: Map<String, JsonWrapper>? = null)

    fun getUsers(callback: (Map<String, UserWrapper>) -> Unit) {
        rootReference.child(USERS_KEY).once("value") { snapshot ->
            callback(parse(Users.serializer(), object {

                @Suppress("unused")
                val userWrappers = snapshot
            }).userWrappers ?: mapOf())
        }
    }

    @Serializable
    private class Users(val userWrappers: Map<String, UserWrapper>? = null)

    fun getRootTasks(callback: (Map<String, RootTaskJson>) -> Unit) {
        rootReference.child(TASKS_KEY).once("value") { snapshot ->
            callback(parse(RootTasks.serializer(), object {

                @Suppress("unused")
                val rootTaskJsons = snapshot
            }).rootTaskJsons ?: mapOf())
        }
    }

    @Serializable
    private class RootTasks(val rootTaskJsons: Map<String, RootTaskJson>? = null)

    @Suppress("EXPERIMENTAL_API_USAGE", "DEPRECATION", "UnsafeCastFromDynamic")
    private fun <T> parse(
            serializer: DeserializationStrategy<T>,
            data: dynamic
    ) = json.decodeFromString(serializer, JSON.stringify(data))
}