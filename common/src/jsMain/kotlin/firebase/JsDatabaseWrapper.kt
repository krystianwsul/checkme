package firebase

import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json

class JsDatabaseWrapper(admin: dynamic, root: String) : DatabaseWrapper() {

    private val rootReference = admin.database().ref(root)

    override fun getNewId(path: String) = rootReference.child(path)
            .push()
            .key as String

    override fun update(path: String, values: Map<String, Any?>, callback: DatabaseCallback) {
        rootReference.child(path).update(values) { error ->
            callback("error: $error", error == null, null)
        }
    }

    fun getPrivateProjects(callback: (Map<String, PrivateProjectJson>) -> Unit) {
        rootReference.child(PRIVATE_PROJECTS_KEY).on("value") { snapshot ->
            val privateProjectJsons = mutableMapOf<String, PrivateProjectJson>()

            snapshot.forEach { child ->
                privateProjectJsons[child.key as String] = parseVal(PrivateProjectJson.serializer(), child)

                Unit
            }

            callback(privateProjectJsons)
        }
    }

    fun getSharedProjects(callback: (Map<String, JsonWrapper>) -> Unit) {
        rootReference.child(RECORDS_KEY).on("value") { snapshot ->
            val jsonWrappers = mutableMapOf<String, JsonWrapper>()

            snapshot.forEach { child ->
                jsonWrappers[child.key as String] = parseVal(JsonWrapper.serializer(), child)

                Unit
            }

            callback(jsonWrappers)
        }
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    fun <T> parse(
            serializer: DeserializationStrategy<T>,
            data: dynamic
    ) = Json.nonstrict.parse(serializer, JSON.stringify(data))

    fun <T> parseVal(
            serializer: DeserializationStrategy<T>,
            snapshot: dynamic
    ) = parse(serializer, snapshot.`val`())
}