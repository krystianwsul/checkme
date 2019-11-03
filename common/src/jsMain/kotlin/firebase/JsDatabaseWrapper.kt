package firebase

import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.DatabaseWrapper

class JsDatabaseWrapper(private val root: String) : DatabaseWrapper() {

    override fun getNewId(path: String): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateRecords(values: Map<String, Any?>, callback: DatabaseCallback) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updatePrivateProjects(values: Map<String, Any?>, callback: DatabaseCallback) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}