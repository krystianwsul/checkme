package com.krystianwsul.checkme.domainmodel.notifications

import com.krystianwsul.checkme.domainmodel.Instance
import com.krystianwsul.checkme.utils.time.ExactTimeStamp

class NotificationWrapperImplOMr1 : NotificationWrapperImplO() {

    override fun getInstanceData(instance: Instance, silent: Boolean, now: ExactTimeStamp) = InstanceData(instance, now, silent)
}