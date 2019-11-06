package firebase

import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class JsDatabaseWrapper(admin: dynamic, root: String) : DatabaseWrapper() {

    private val rootReference = admin.database().ref(root)

    override fun getNewId(path: String) = rootReference.child(path)
            .push()
            .key as String

    override fun update(path: String, values: Map<String, Any?>, callback: DatabaseCallback) {
        /* todo enable saving
        rootReference.child(path).update(values) { error ->
            callback("error: $error", error == null, null)
        }
         */

        callback("not actually saving", true, null)
    }

    fun getPrivateProjects(callback: (Map<String, PrivateProjectJson>) -> Unit) {
        rootReference.child(PRIVATE_PROJECTS_KEY).once("value") { snapshot ->
            callback(parse(PrivateProjects.serializer(), object {
                val privateProjectJsons = snapshot
            }).privateProjectJsons)
        }
    }

    @Serializable
    private class PrivateProjects(val privateProjectJsons: Map<String, PrivateProjectJson>)

    fun getSharedProjects(callback: (Map<String, JsonWrapper>) -> Unit) {
        rootReference.child(RECORDS_KEY).once("value") { snapshot ->
            callback(parse(SharedProjects.serializer(), object {
                val jsonWrappers = snapshot
            }).jsonWrappers)
        }
    }

    @Serializable
    private class SharedProjects(val jsonWrappers: Map<String, JsonWrapper>)

    @Suppress("EXPERIMENTAL_API_USAGE")
    fun <T> parse(
            serializer: DeserializationStrategy<T>,
            data: dynamic
    ) = Json.nonstrict.parse(serializer, JSON.stringify(data))
}