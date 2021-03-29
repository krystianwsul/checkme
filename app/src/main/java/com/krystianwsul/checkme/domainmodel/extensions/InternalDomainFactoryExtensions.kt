package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import io.reactivex.rxjava3.core.Completable

fun AndroidDomainUpdater.fixOffsets(source: String): Completable = CompletableDomainUpdate.create {
    MyCrashlytics.log("triggering fixing offsets from $source")

    projectsFactory.projects
            .values
            .forEach { it.fixOffsets() }

    DomainUpdater.Params(false, DomainListenerManager.NotificationType.All)
}.perform(this)