package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.FactoryProvider
import com.krystianwsul.checkme.utils.RelayProperty
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.firebase.json.PrivateProjectJson
import com.krystianwsul.common.firebase.managers.PrivateProjectManager
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.time.ExactTimeStamp
import io.reactivex.Observable

class AndroidPrivateProjectManager(
        private val userInfo: UserInfo,
        dataSnapshot: FactoryProvider.Database.Snapshot,
        now: ExactTimeStamp,
        private val factoryProvider: FactoryProvider
) : PrivateProjectManager<DomainFactory>() {

    private val privateProjectProperty = RelayProperty(
            this,
            dataSnapshot.takeIf { it.exists() }
                    ?.let { dataSnapshot.toRecord() }
                    ?: PrivateProjectRecord(factoryProvider.database, userInfo, PrivateProjectJson(startTime = now.long))
    )

    var privateProjectRecord by privateProjectProperty
        private set

    val privateProjectObservable: Observable<PrivateProjectRecord> = privateProjectProperty.observable

    override val privateProjectRecords get() = listOf(privateProjectRecord)

    override val databaseWrapper = factoryProvider.database

    private fun FactoryProvider.Database.Snapshot.toRecord() = PrivateProjectRecord(
            factoryProvider.database,
            userInfo.key.toPrivateProjectKey(),
            getValue(PrivateProjectJson::class.java)!!
    )

    fun newSnapshot(dataSnapshot: FactoryProvider.Database.Snapshot): PrivateProjectRecord {
        privateProjectRecord = dataSnapshot.toRecord()
        return privateProjectRecord
    }

    override fun getDatabaseCallback(extra: DomainFactory, values: Map<String, Any?>): DatabaseCallback {
        return checkError(extra, "RemotePrivateProjectManager.save", values)
    }
}