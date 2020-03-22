package firebase.managers

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.managers.SharedProjectManager
import com.krystianwsul.common.firebase.records.SharedProjectRecord
import com.krystianwsul.common.utils.ProjectKey

class JsSharedProjectManager(
        override val databaseWrapper: DatabaseWrapper,
        jsonWrappers: Map<String, JsonWrapper>
) : SharedProjectManager<Unit>() {

    override var sharedProjectRecords = jsonWrappers.entries
            .associate {
                val projectKey = ProjectKey.Shared(it.key)
                projectKey to Pair(SharedProjectRecord(databaseWrapper, this, projectKey, it.value), false)
            }

    override var saveCallback: (() -> Unit)? = null

    override fun getDatabaseCallback(extra: Unit): DatabaseCallback {
        return { message, _, _ ->
            ErrorLogger.instance.log(message)
            saveCallback?.invoke()
        }
    }

    override fun setSharedProjectRecord(projectKey: ProjectKey.Shared, pair: Pair<SharedProjectRecord, Boolean>) {
        sharedProjectRecords = sharedProjectRecords.toMutableMap().also {
            it[projectKey] = pair
        }
    }

    override fun deleteRemoteSharedProjectRecord(projectKey: ProjectKey.Shared) {
        sharedProjectRecords = sharedProjectRecords.toMutableMap().apply {
            remove(projectKey)
        }
    }
}