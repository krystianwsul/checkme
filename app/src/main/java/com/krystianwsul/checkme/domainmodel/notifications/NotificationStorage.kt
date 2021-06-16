package com.krystianwsul.checkme.domainmodel.notifications

import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.utils.toV3
import com.krystianwsul.common.utils.InstanceShownKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.TaskKeyData
import com.pacoworks.rxpaper2.RxPaperBook
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.cast
import io.reactivex.rxjava3.schedulers.Schedulers

class NotificationStorage(
    private val rxPaperBook: RxPaperBook,
    private var savedProjectNotificationKeys: List<ProjectNotificationKey>,
    private var savedInstanceShownMap: Map<InstanceShownKey, InstanceShownData>,
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

            val instancesSingle = rxPaperBook.read<Map<InstanceShownKey, InstanceShownData>>(KEY_INSTANCES)
                .toV3()
                .onErrorReturnItem(emptyMap())
                .subscribeOn(Schedulers.io())

            return Singles.zip(projectsSingle, instancesSingle)
                .map { (projects, instances) -> NotificationStorage(rxPaperBook, projects, instances) }
                .cast<FactoryProvider.NotificationStorage>()
                .observeOnDomain()
        }
    }

    override var projectNotificationKeys = savedProjectNotificationKeys

    override var instanceShownMap = savedInstanceShownMap.toMutableMap()
        private set

    override fun save(): Boolean {
        if (projectNotificationKeys == savedProjectNotificationKeys && instanceShownMap == savedInstanceShownMap)
            return false

        savedProjectNotificationKeys = projectNotificationKeys
        savedInstanceShownMap = instanceShownMap

        rxPaperBook.write(KEY_PROJECTS, projectNotificationKeys)
            .toV3()
            .subscribeOn(Schedulers.io())
            .subscribe()

        rxPaperBook.write(KEY_INSTANCES, instanceShownMap)
            .toV3()
            .subscribeOn(Schedulers.io())
            .subscribe()

        return true
    }

    override fun deleteInstanceShown(taskKeys: Set<TaskKey>) {
        val taskKeyDatas = taskKeys.map(::TaskKeyData)

        instanceShownMap.keys
            .filterNot { instanceShownKey ->
                taskKeyDatas.any { taskKeyData ->
                    instanceShownKey.projectId == taskKeyData.projectId && instanceShownKey.taskId == taskKeyData.taskId
                }
            }
            .forEach(instanceShownMap::remove)
    }
}