package firebase.managers

import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.managers.RootUserManager
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.UserKey

class JsRootUserManager(userWrappers: Map<String, UserWrapper>) : RootUserManager() {

    override var rootUserRecords = userWrappers.map { RootUserRecord(false, it.value, UserKey(it.key)) to false }
            .associateBy { it.first.userKey }
            .toMutableMap()
}