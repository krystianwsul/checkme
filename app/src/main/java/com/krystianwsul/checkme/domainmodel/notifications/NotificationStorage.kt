package com.krystianwsul.checkme.domainmodel.notifications

import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.utils.toV3
import com.pacoworks.rxpaper2.RxPaperBook
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.cast
import io.reactivex.rxjava3.schedulers.Schedulers

class NotificationStorage(
    private val rxPaperBook: RxPaperBook,
    private var savedProjectNotificationKeys: List<ProjectNotificationKey>,
    private var savedInstanceShownRecords: List<InstanceShownRecord>,
) : FactoryProvider.NotificationStorage {

    companion object : FactoryProvider.NotificationStorageFactory {

        private const val KEY_PROJECTS = "projects"
        private const val KEY_INSTANCES = "instances"

        override fun getNotificationStorage(): Single<FactoryProvider.NotificationStorage> {
            val rxPaperBook = RxPaperBook.with("notifications")

            val projectsSingle = rxPaperBook.read<List<ProjectNotificationKey>>(KEY_PROJECTS)
                .toV3()
                .onErrorReturnItem(emptyList())
                .subscribeOn(Schedulers.io())

            val instancesSingle = rxPaperBook.read<List<InstanceShownRecord>>(KEY_INSTANCES)
                .toV3()
                .onErrorReturnItem(emptyList())
                .subscribeOn(Schedulers.io())

            return Singles.zip(projectsSingle, instancesSingle)
                .map { (projects, instances) -> NotificationStorage(rxPaperBook, projects, instances) }
                .cast<FactoryProvider.NotificationStorage>()
                .observeOnDomain()
        }
    }

    override var projectNotificationKeys = savedProjectNotificationKeys
    override var instanceShownRecords = savedInstanceShownRecords

    override fun save(): Boolean {
        if (projectNotificationKeys == savedProjectNotificationKeys && instanceShownRecords == savedInstanceShownRecords)
            return false

        savedProjectNotificationKeys = projectNotificationKeys
        savedInstanceShownRecords = instanceShownRecords

        rxPaperBook.write(KEY_PROJECTS, projectNotificationKeys)
            .toV3()
            .subscribeOn(Schedulers.io())
            .subscribe()

        rxPaperBook.write(KEY_INSTANCES, instanceShownRecords)
            .toV3()
            .subscribeOn(Schedulers.io())
            .subscribe()

        return true
    }
}