package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.common.firebase.models.ProjectOrdinalManager

class MixedInstanceDataCollection(
    val instanceDatas: Collection<GroupListDataWrapper.InstanceData>,
    val groupTypeTree: List<GroupTypeFactory.Bridge>,
) {

    constructor(
        instanceDescriptors: Collection<GroupTypeFactory.InstanceDescriptor>,
        projectOrdinalManagerProvider: ProjectOrdinalManager.Provider,
        projectProvider: GroupTypeFactory.ProjectProvider,
        groupingMode: GroupType.GroupingMode = GroupType.GroupingMode.None,
    ) : this(
        instanceDescriptors.map { it.instanceData },
        GroupTypeFactory(projectOrdinalManagerProvider, projectProvider).getGroupTypeTree(instanceDescriptors, groupingMode),
    )
}