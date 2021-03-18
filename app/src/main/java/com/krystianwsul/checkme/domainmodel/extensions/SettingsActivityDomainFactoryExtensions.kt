package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.completeOnDomain
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.SettingsViewModel
import com.krystianwsul.common.firebase.DomainThreadChecker

fun DomainFactory.getSettingsData(): SettingsViewModel.Data {
    MyCrashlytics.log("DomainFactory.getSettingsData")

    DomainThreadChecker.instance.requireDomainThread()

    return SettingsViewModel.Data(myUserFactory.user.defaultReminder)
}

@CheckResult
fun DomainFactory.updateDefaultTab(source: SaveService.Source, defaultTab: Int) = completeOnDomain {
    MyCrashlytics.log("DomainFactory.updateDefaultTab")
    if (myUserFactory.isSaved) throw SavedFactoryException()

    myUserFactory.user.defaultTab = defaultTab

    save(0, source)
}

@CheckResult
fun DomainFactory.updateDefaultReminder(
        dataId: Int,
        source: SaveService.Source,
        defaultReminder: Boolean,
) = completeOnDomain {
    MyCrashlytics.log("DomainFactory.updateDefaultReminder")
    if (myUserFactory.isSaved) throw SavedFactoryException()

    myUserFactory.user.defaultReminder = defaultReminder

    save(dataId, source)
}