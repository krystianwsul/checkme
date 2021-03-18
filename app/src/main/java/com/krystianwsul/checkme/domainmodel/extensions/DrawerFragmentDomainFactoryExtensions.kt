package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.viewmodels.DrawerViewModel
import com.krystianwsul.common.firebase.DomainThreadChecker

fun DomainFactory.getDrawerData(): DrawerViewModel.Data {
    MyCrashlytics.log("DomainFactory.getDrawerData")

    DomainThreadChecker.instance.requireDomainThread()

    return myUserFactory.user.run { DrawerViewModel.Data(name, email, photoUrl) }
}