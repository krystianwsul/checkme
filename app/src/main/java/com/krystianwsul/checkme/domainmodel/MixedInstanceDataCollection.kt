package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper

class MixedInstanceDataCollection(
    val instanceDatas: Collection<GroupListDataWrapper.InstanceData>,
    private val groupingMode: GroupType.GroupingMode = GroupType.GroupingMode.None,
) {

    fun getGroupTypeTree() = GroupTypeFactory.getGroupTypeTree(instanceDatas.toList(), groupingMode)
}