package com.krystianwsul.checkme.domainmodel.notifications

import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.utils.toV3
import com.pacoworks.rxpaper2.RxPaperBook
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.cast
import io.reactivex.rxjava3.schedulers.Schedulers

class NotificationStorage(
    private val rxPaperBook: RxPaperBook,
    private var projectNotificationKeys: List<ProjectNotificationKey>,
) : FactoryProvider.NotificationStorage {

    companion object : FactoryProvider.NotificationStorageFactory {

        private const val KEY_PROJECTS = "projects"

        override fun getNotificationStorage(): Single<FactoryProvider.NotificationStorage> {
            val rxPaperBook = RxPaperBook.with("notifications")

            return rxPaperBook.read<List<ProjectNotificationKey>>(KEY_PROJECTS)
                .toV3()
                .subscribeOn(Schedulers.io())
                .onErrorReturnItem(emptyList())
                .map { NotificationStorage(rxPaperBook, it) }
                .cast<FactoryProvider.NotificationStorage>()
                .observeOnDomain()
        }
    }

    override fun getKeys() = projectNotificationKeys

    override fun writeKeys(projectNotificationKeys: List<ProjectNotificationKey>) {
        this.projectNotificationKeys = projectNotificationKeys

        rxPaperBook.write(KEY_PROJECTS, projectNotificationKeys)
            .toV3()
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

}