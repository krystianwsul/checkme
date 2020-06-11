package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.viewmodels.DrawerViewModel

@Synchronized
fun DomainFactory.getDrawerData(): DrawerViewModel.Data {
    MyCrashlytics.log("DomainFactory.getDrawerData")

    return myUserFactory.user.run { DrawerViewModel.Data(name, email, photoUrl) }
}