package com.krystianwsul.checkme.domainmodel.extensions

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainFactory.Companion.syncOnDomain
import com.krystianwsul.checkme.viewmodels.DrawerViewModel

fun DomainFactory.getDrawerData(): DrawerViewModel.Data = syncOnDomain {
    MyCrashlytics.log("DomainFactory.getDrawerData")

    myUserFactory.user.run { DrawerViewModel.Data(name, email, photoUrl) }
}