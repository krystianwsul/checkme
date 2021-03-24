package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.DomainUpdater
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.InstanceKey
import io.reactivex.rxjava3.core.Single

@CheckResult
fun DomainUpdater.setInstancesDone(
        notificationType: DomainListenerManager.NotificationType,
        instanceKeys: List<InstanceKey>,
        done: Boolean,
): Single<ExactTimeStamp.Local> = updateDomainSingle {
    MyCrashlytics.log("DomainFactory.setInstancesDone")

    check(instanceKeys.isNotEmpty())

    val now = ExactTimeStamp.Local.now

    val instances = instanceKeys.map(this::getInstance)

    instances.forEach { it.setDone(localFactory, done, now) }

    val remoteProjects = instances.map { it.task.project }.toSet()

    DomainUpdater.Result(now, now, notificationType, DomainFactory.CloudParams(remoteProjects))
}