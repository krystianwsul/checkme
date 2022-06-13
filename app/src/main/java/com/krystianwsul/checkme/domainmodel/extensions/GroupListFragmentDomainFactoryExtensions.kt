package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.common.firebase.models.project.SharedOwnedProject
import com.krystianwsul.common.firebase.models.users.ProjectOrdinalManager
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.Ordinal
import io.reactivex.rxjava3.core.Completable

@CheckResult
fun DomainUpdater.setInstancesDone(
        notificationType: DomainListenerManager.NotificationType,
        instanceKeys: List<InstanceKey>,
        done: Boolean,
) = CompletableDomainUpdate.create("setInstancesDone") { now ->
    check(instanceKeys.isNotEmpty())

    val instances = instanceKeys.map(this::getInstance)

    instances.forEach { it.setDone(shownFactory, done, now) }

    val remoteProjects = instances.map { it.task.project }.toSet()

    DomainUpdater.Params(true, notificationType, DomainFactory.CloudParams(remoteProjects))
}.perform(this)

@CheckResult
fun DomainUpdater.setOrdinalProject(
    notificationType: DomainListenerManager.NotificationType,
    instanceKeys: Set<InstanceKey>,
    ordinal: Ordinal,
    parentInstanceKey: InstanceKey?,
): Completable = CompletableDomainUpdate.create("setOrdinalProject") { now ->
    val instances = instanceKeys.map(::getInstance)

    val project = instances.map { it.getProject() }
        .distinct()
        .single()
        .let { it as SharedOwnedProject } // todo group ordinal

    val parentInstance = parentInstanceKey?.let(::getInstance)

    val key = ProjectOrdinalManager.Key(instances, parentInstance)

    myUserFactory.user.getProjectOrdinalManager(project).setOrdinal(project, key, ordinal, now)

    DomainUpdater.Params(true, notificationType, DomainFactory.CloudParams(project))
}.perform(this)