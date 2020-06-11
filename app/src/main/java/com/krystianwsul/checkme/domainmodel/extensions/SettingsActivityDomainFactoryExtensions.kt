package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.SettingsViewModel

@Synchronized
fun DomainFactory.getSettingsData(): SettingsViewModel.Data {
    MyCrashlytics.log("DomainFactory.getSettingsData")

    return SettingsViewModel.Data(myUserFactory.user.defaultReminder)
}

@Synchronized
fun DomainFactory.updateDefaultTab(source: SaveService.Source, defaultTab: Int) {
    MyCrashlytics.log("DomainFactory.updateDefaultTab")
    if (myUserFactory.isSaved) throw SavedFactoryException()

    myUserFactory.user.defaultTab = defaultTab

    save(0, source)
}

@Synchronized
fun DomainFactory.updateDefaultReminder(
    dataId: Int,
    source: SaveService.Source,
    defaultReminder: Boolean
) {
    MyCrashlytics.log("DomainFactory.updateDefaultReminder")
    if (myUserFactory.isSaved) throw SavedFactoryException()

    myUserFactory.user.defaultReminder = defaultReminder

    save(dataId, source)
}