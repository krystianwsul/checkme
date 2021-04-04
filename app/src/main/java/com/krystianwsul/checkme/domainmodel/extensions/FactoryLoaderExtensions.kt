package com.krystianwsul.checkme.domainmodel.extensions

import androidx.annotation.CheckResult
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.update.CompletableDomainUpdate
import com.krystianwsul.checkme.domainmodel.update.DomainUpdater
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.DomainThreadChecker
import io.reactivex.rxjava3.core.Completable

@CheckResult
fun DomainUpdater.updateDeviceDbInfo(deviceDbInfo: DeviceDbInfo): Completable =
        CompletableDomainUpdate.create("updateDeviceDbInfo") {
            DomainThreadChecker.instance.requireDomainThread()

            if (myUserFactory.isSaved || projectsFactory.isSharedSaved) throw SavedFactoryException()

            this.deviceDbInfo = deviceDbInfo

            myUserFactory.user.apply {
                name = deviceDbInfo.name
                setToken(deviceDbInfo)
            }

            projectsFactory.updateDeviceInfo(deviceDbInfo)

            DomainUpdater.Params(false, DomainListenerManager.NotificationType.All)
        }.perform(this)