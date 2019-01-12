package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.utils.time.ExactTimeStamp

class NotificationWrapperImplOMr1 : NotificationWrapperImplO() {

    override fun notifyInstance(domainFactory: DomainFactory, instance: Instance, silent: Boolean, now: ExactTimeStamp) = notifyInstanceHelper(domainFactory, instance, silent, now)
}