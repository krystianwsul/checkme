package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper

class MixedInstanceDataCollection(
    val instanceDatas: Collection<GroupListDataWrapper.InstanceData>,
    val groupTypeTree: List<GroupTypeFactory.Bridge>,
) {

    constructor(
        instanceDatas: Collection<GroupListDataWrapper.InstanceData>,
        groupingMode: GroupType.GroupingMode = GroupType.GroupingMode.None,
    ) : this(instanceDatas, GroupTypeFactory.getGroupTypeTree(instanceDatas.toList(), groupingMode))
}