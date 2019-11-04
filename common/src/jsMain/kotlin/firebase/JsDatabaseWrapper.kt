package firebase

import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper

external fun require(module: String): dynamic

class JsDatabaseWrapper(root: String) : DatabaseWrapper() {

    private val admin = require("firebase-admin")
    private val rootReference = admin.database().ref(root)

    override fun getNewId(path: String) = rootReference.child(path)
            .push()
            .key as String

    override fun update(path: String, values: Map<String, Any?>, callback: DatabaseCallback) {
        rootReference.child(path).update(values) { error ->
            callback("error: $error", error == null, null)
        }
    }
}