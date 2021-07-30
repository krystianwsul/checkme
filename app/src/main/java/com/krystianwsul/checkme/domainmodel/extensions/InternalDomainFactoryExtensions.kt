package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import io.reactivex.rxjava3.core.Completable

fun DomainUpdater.fixOffsetsAndCustomTimes(source: String): Completable =
    CompletableDomainUpdate.create("fixOffsetsAndCustomTimes, source: $source") { now ->
        projectsFactory.projects
            .values
            .forEach { it.fixOffsets() }

        projectsFactory.privateProject
            .customTimes
            .filter { it.notDeleted() }
            .forEach { migratePrivateCustomTime(it, now) }

        DomainUpdater.Params(false, DomainListenerManager.NotificationType.All)
        }.perform(this)