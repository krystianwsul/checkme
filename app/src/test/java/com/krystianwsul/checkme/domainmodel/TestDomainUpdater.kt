package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.domainmodel.update.DomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.common.firebase.models.checkInconsistentRootTaskIds
import com.krystianwsul.common.time.ExactTimeStamp
import io.reactivex.rxjava3.core.Single


class TestDomainUpdater(
        private val domainFactory: DomainFactory,
        private val now: ExactTimeStamp.Local,
) : DomainUpdater() {

    override fun <T : Any> performDomainUpdate(domainUpdate: DomainUpdate<T>): Single<T> {
        val (data, params) = domainUpdate.doAction(domainFactory, now)

        domainFactory.updateNotifications(params, now)

        // todo remove this once error found
        domainFactory.apply {
            checkInconsistentRootTaskIds(domainFactory.rootTasksFactory.rootTasks.values, projectsFactory.projects.values)
        }

        domainFactory.saveAndNotifyCloud(params, now)

        return Single.just(data)
    }
}