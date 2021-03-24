package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.DomainUpdater
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.DomainThreadChecker

@CheckResult
fun DomainUpdater.updateDeviceDbInfo(deviceDbInfo: DeviceDbInfo) = updateDomainCompletable {
    MyCrashlytics.log("DomainFactory.updateDeviceDbInfo")

    DomainThreadChecker.instance.requireDomainThread()

    if (myUserFactory.isSaved || projectsFactory.isSharedSaved) throw SavedFactoryException()

    this.deviceDbInfo = deviceDbInfo

    myUserFactory.user.apply {
        name = deviceDbInfo.name
        setToken(deviceDbInfo)
    }

    projectsFactory.updateDeviceInfo(deviceDbInfo)

    DomainUpdater.Params(notificationType = DomainListenerManager.NotificationType.All)
}