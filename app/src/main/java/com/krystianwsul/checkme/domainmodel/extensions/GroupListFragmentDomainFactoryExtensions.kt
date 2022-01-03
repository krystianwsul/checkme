package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.notifications.Notifier
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.firebase.models.users.ProjectOrdinalManager
import com.krystianwsul.common.utils.InstanceKey
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
    ordinal: Double,
): Completable = CompletableDomainUpdate.create("setOrdinalProject") { now ->
    val instances = instanceKeys.map(::getInstance)

    val project = instances.map { it.getProject() }
        .distinct()
        .single()
        .let { it as SharedProject }

    val key = ProjectOrdinalManager.Key(
        instances.map { ProjectOrdinalManager.Key.Entry(it.instanceKey, it.instanceDateTime.toDateTimePair()) }.toSet()
    )

    myUserFactory.user.getProjectOrdinalManager(project).setOrdinal(project, key, ordinal, now)

//    DomainUpdater.Params(true, notificationType, DomainFactory.CloudParams(project)) todo ordinal revert once this actually commits DB changes
    DomainUpdater.Params(Notifier.Params(), DomainFactory.SaveParams(notificationType, true))
}.perform(this)