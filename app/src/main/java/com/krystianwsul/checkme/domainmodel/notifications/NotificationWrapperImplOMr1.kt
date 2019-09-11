package com.krystianwsul.checkme.domainmodel.notifications

import com.krystianwsul.checkme.domain.Instance
import com.krystianwsul.common.time.ExactTimeStamp

class NotificationWrapperImplOMr1 : NotificationWrapperImplO() {

    override fun getInstanceData(instance: Instance, silent: Boolean, now: ExactTimeStamp) = InstanceData(instance, now, silent)
}