package firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.managers.RemotePrivateProjectManager
import com.krystianwsul.common.firebase.records.RemotePrivateProjectRecord
import com.krystianwsul.common.utils.ProjectKey

class JsPrivateProjectManager(
        override val databaseWrapper: DatabaseWrapper,
        privateProjectJsons: Map<String, PrivateProjectJson>
) : RemotePrivateProjectManager<Unit>() {

    override val remotePrivateProjectRecords = privateProjectJsons.map { RemotePrivateProjectRecord(databaseWrapper, ProjectKey.Private(it.key), it.value) }

    override var saveCallback: (() -> Unit)? = null

    override fun getDatabaseCallback(extra: Unit, values: Map<String, Any?>): DatabaseCallback {
        return { message, _, _ ->
            ErrorLogger.instance.log(message)
            saveCallback?.invoke()
        }
    }
}