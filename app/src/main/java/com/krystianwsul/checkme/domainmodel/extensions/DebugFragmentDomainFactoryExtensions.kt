package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater

fun DomainUpdater.setDebugMode(enabled: Boolean) = CompletableDomainUpdate.create("setDebugMode") {
    debugMode = enabled

    DomainUpdater.Params(saveParams = DomainFactory.SaveParams(DomainListenerManager.NotificationType.All, true))
}.perform(this)