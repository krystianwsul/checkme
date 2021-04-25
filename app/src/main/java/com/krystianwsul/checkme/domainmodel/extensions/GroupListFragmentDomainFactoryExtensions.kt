package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.common.utils.InstanceKey

@CheckResult
fun DomainUpdater.setInstancesDone(
        notificationType: DomainListenerManager.NotificationType,
        instanceKeys: List<InstanceKey>,
        done: Boolean,
) = CompletableDomainUpdate.create("setInstancesDone") { now ->
    check(instanceKeys.isNotEmpty())

    val instances = instanceKeys.map(this::getInstance)

    instances.forEach { it.setDone(localFactory, done, now) }

    val remoteProjects = instances.map { it.task.project }.toSet() // todo task project

    DomainUpdater.Params(true, notificationType, DomainFactory.CloudParams(remoteProjects))
}.perform(this)