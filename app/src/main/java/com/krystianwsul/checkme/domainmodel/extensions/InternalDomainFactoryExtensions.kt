package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import io.reactivex.rxjava3.core.Completable

fun DomainUpdater.fixStuff(source: String): Completable =
    CompletableDomainUpdate.create("fixStuff, source: $source") { now ->
        projectsFactory.projects
            .values
            .forEach { it.fixOffsets() }

        projectsFactory.privateProject.apply {
            ownerName = deviceDbInfo.name

            customTimes.filter { it.notDeleted }.forEach { migratePrivateCustomTime(it, now) }
        }

        rootTasksFactory.rootTasks
            .values
            .filter { it.dependenciesLoaded }
            .forEach { it.fixProjectKeys() }

        DomainUpdater.Params(false, DomainListenerManager.NotificationType.All)
        }.perform(this)