package com.krystianwsul.checkme.firebase.managers

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.utils.RelayProperty
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.managers.PrivateProjectManager
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectKey
import io.reactivex.Observable

class AndroidPrivateProjectManager(
        userInfo: UserInfo,
        dataSnapshot: DataSnapshot,
        now: ExactTimeStamp
) : PrivateProjectManager<DomainFactory>() {

    private val privateProjectProperty = RelayProperty(
            this,
            dataSnapshot.value
                    ?.let { dataSnapshot.toRecord() }
                    ?: PrivateProjectRecord(AndroidDatabaseWrapper, userInfo, PrivateProjectJson(startTime = now.long))
    )

    var privateProjectRecord by privateProjectProperty
        private set

    val privateProjectObservable: Observable<PrivateProjectRecord> = privateProjectProperty.observable

    override val privateProjectRecords get() = listOf(privateProjectRecord)

    override val databaseWrapper = AndroidDatabaseWrapper

    private fun DataSnapshot.toRecord() = PrivateProjectRecord(
            AndroidDatabaseWrapper,
            ProjectKey.Private(key!!),
            getValue(PrivateProjectJson::class.java)!!
    )

    fun newSnapshot(dataSnapshot: DataSnapshot): PrivateProjectRecord {
        privateProjectRecord = dataSnapshot.toRecord()
        return privateProjectRecord
    }

    override fun getDatabaseCallback(extra: DomainFactory, values: Map<String, Any?>): DatabaseCallback {
        return checkError(extra, "RemotePrivateProjectManager.save", values)
    }
}