package com.krystianwsul.checkme.domainmodel.undo

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.common.firebase.models.Project
import com.krystianwsul.common.time.ExactTimeStamp

interface UndoData {

    fun undo(domainFactory: DomainFactory, now: ExactTimeStamp.Local): Set<Project<*>>
}