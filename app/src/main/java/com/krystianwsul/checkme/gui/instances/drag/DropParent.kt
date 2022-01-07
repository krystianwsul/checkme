package com.krystianwsul.checkme.gui.instances.drag

import com.krystianwsul.checkme.domainmodel.GroupTypeFactory

interface DropParent {

    fun canDropIntoParent(droppedTimeChild: GroupTypeFactory.TimeChild): Boolean
}