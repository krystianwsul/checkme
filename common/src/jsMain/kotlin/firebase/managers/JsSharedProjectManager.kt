package firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.managers.RemoteSharedProjectManager
import com.krystianwsul.common.firebase.records.RemoteSharedProjectRecord
import com.krystianwsul.common.utils.ProjectKey

class JsSharedProjectManager(
        override val databaseWrapper: DatabaseWrapper,
        jsonWrappers: Map<String, JsonWrapper>
) : RemoteSharedProjectManager<Unit>() {

    override val remoteProjectRecords = jsonWrappers.entries
            .associate {
                val projectKey = ProjectKey.Shared(it.key)
                projectKey to RemoteSharedProjectRecord(databaseWrapper, this, projectKey, it.value)
            }
            .toMutableMap()

    override var saveCallback: (() -> Unit)? = null

    override fun getDatabaseCallback(extra: Unit): DatabaseCallback {
        return { message, _, _ ->
            ErrorLogger.instance.log(message)
            saveCallback?.invoke()
        }
    }
}