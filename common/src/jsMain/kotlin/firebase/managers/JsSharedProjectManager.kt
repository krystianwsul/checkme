package firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.managers.SharedProjectManager
import com.krystianwsul.common.firebase.records.RemoteSharedProjectRecord
import com.krystianwsul.common.utils.ProjectKey

class JsSharedProjectManager(
        override val databaseWrapper: DatabaseWrapper,
        jsonWrappers: Map<String, JsonWrapper>
) : SharedProjectManager<Unit>() {

    override var sharedProjectRecords = jsonWrappers.entries
            .associate {
                val projectKey = ProjectKey.Shared(it.key)
                projectKey to Pair(RemoteSharedProjectRecord(databaseWrapper, this, projectKey, it.value), false)
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