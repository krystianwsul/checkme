package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactory.Companion.syncOnDomain
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.SettingsViewModel

fun DomainFactory.getSettingsData(): SettingsViewModel.Data = syncOnDomain {
    MyCrashlytics.log("DomainFactory.getSettingsData")

    SettingsViewModel.Data(myUserFactory.user.defaultReminder)
}

fun DomainFactory.updateDefaultTab(source: SaveService.Source, defaultTab: Int) = syncOnDomain {
    MyCrashlytics.log("DomainFactory.updateDefaultTab")
    if (myUserFactory.isSaved) throw SavedFactoryException()

    myUserFactory.user.defaultTab = defaultTab

    save(0, source)
}

fun DomainFactory.updateDefaultReminder(
        dataId: Int,
        source: SaveService.Source,
        defaultReminder: Boolean
) = syncOnDomain {
    MyCrashlytics.log("DomainFactory.updateDefaultReminder")
    if (myUserFactory.isSaved) throw SavedFactoryException()

    myUserFactory.user.defaultReminder = defaultReminder

    save(dataId, source)
}