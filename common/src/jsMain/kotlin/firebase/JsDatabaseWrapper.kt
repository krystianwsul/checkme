package firebase

import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.json.UserWrapper
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class JsDatabaseWrapper(admin: dynamic, root: String) : DatabaseWrapper() {

    private val rootReference = admin.database().ref(root)

    override fun getNewId(path: String) = rootReference.child(path)
            .push()
            .key as String

    override fun update(path: String, values: Map<String, Any?>, callback: DatabaseCallback) {
        check(values.values.all { it == null })

        val dynamicValues: dynamic = object {}

        values.forEach {
            dynamicValues[it.key] = it.value
        }

        rootReference.child(path).update(dynamicValues) { error ->
            callback("error: " + error?.toString(), error == null, null)
        }
    }

    fun getPrivateProjects(callback: (Map<String, PrivateProjectJson>) -> Unit) {
        rootReference.child(PRIVATE_PROJECTS_KEY).once("value") { snapshot ->
            callback(parse(PrivateProjects.serializer(), object {

                @Suppress("unused")
                val privateProjectJsons = snapshot
            }).privateProjectJsons)
        }
    }

    @Serializable
    private class PrivateProjects(val privateProjectJsons: Map<String, PrivateProjectJson>)

    fun getSharedProjects(callback: (Map<String, JsonWrapper>) -> Unit) {
        rootReference.child(RECORDS_KEY).once("value") { snapshot ->
            callback(parse(SharedProjects.serializer(), object {

                @Suppress("unused")
                val jsonWrappers = snapshot
            }).jsonWrappers)
        }
    }

    @Serializable
    private class SharedProjects(val jsonWrappers: Map<String, JsonWrapper>)

    fun getUsers(callback: (Map<String, UserWrapper>) -> Unit) {
        rootReference.child(USERS_KEY).once("value") { snapshot ->
            callback(parse(Users.serializer(), object {

                @Suppress("unused")
                val userWrappers = snapshot
            }).userWrappers)
        }
    }

    @Serializable
    private class Users(val userWrappers: Map<String, UserWrapper>)

    @Suppress("EXPERIMENTAL_API_USAGE")
    fun <T> parse(
            serializer: DeserializationStrategy<T>,
            data: dynamic
    ) = Json.nonstrict.parse(serializer, JSON.stringify(data))
}