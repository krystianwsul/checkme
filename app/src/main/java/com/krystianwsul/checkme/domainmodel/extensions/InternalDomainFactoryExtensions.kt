package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import io.reactivex.rxjava3.core.Completable

fun DomainUpdater.fixOffsets(source: String): Completable =
        CompletableDomainUpdate.create("fixOffsets, source: $source") {
            projectsFactory.projects
                    .values
                    .forEach { it.fixOffsets() }

            DomainUpdater.Params(false, DomainListenerManager.NotificationType.All)
        }.perform(this)