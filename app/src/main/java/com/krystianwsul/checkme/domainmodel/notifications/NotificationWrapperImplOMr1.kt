package com.krystianwsul.checkme.domainmodel.notifications

import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.ExactTimeStamp

class NotificationWrapperImplOMr1 : NotificationWrapperImplO() {

    override fun getInstanceData(
            deviceDbInfo: DeviceDbInfo,
            instance: Instance<*>,
            silent: Boolean,
            now: ExactTimeStamp
    ) = InstanceData(deviceDbInfo, instance, now, silent)
}