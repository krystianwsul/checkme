package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.utils.time.ExactTimeStamp

class NotificationWrapperImplOMr1 : NotificationWrapperImplO() {

    override fun notifyInstance(instance: Instance, silent: Boolean, now: ExactTimeStamp) = notifyInstanceHelper(instance, silent, now)
}