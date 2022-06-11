package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.domainmodel.update.DomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.common.time.ExactTimeStamp
import com.mindorks.scheduler.Priority
import io.reactivex.rxjava3.core.Single


class TestDomainUpdater(
        private val domainFactory: DomainFactory,
        private val now: ExactTimeStamp.Local,
) : DomainUpdater() {

    override fun <T : Any> performDomainUpdate(domainUpdate: DomainUpdate<T>): Single<T> {
        // DomainFactory.tryNotifyListeners operates on the assumption that DomainUpdates are performed asynchronously

        return Single.just(Unit)
            .observeOnDomain(Priority.FIRST_READ)
            .map {
                val (data, params) = domainUpdate.doAction(domainFactory, now)

                onUpdated.accept(Unit)

                domainFactory.updateNotifications(params, now)

                /*
                domainFactory.projectsFactory.apply {
                    checkInconsistentRootTaskIds(allDependenciesLoadedTasks.filterIsInstance<RootTask>(), projects.values)
                }
                 */

                domainFactory.saveAndNotifyCloud(params, now)

                data
            }
    }
}