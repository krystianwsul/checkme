package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.common.firebase.models.users.ProjectOrdinalManager

data class MixedInstanceDataCollection(
    val instanceDatas: Collection<GroupListDataWrapper.InstanceData>,
    val groupTypeTree: List<GroupTypeFactory.Bridge>,
) {

    constructor(
        instanceDescriptors: Collection<GroupTypeFactory.InstanceDescriptor>,
        projectOrdinalManagerProvider: ProjectOrdinalManager.Provider,
        groupingMode: GroupType.GroupingMode,
        showDisplayText: Boolean,
        projectInfoMode: ProjectInfoMode,
        compareBy: GroupTypeFactory.SingleBridge.CompareBy,
    ) : this(
        instanceDescriptors.map { it.instanceData },
        GroupTypeFactory(
            projectOrdinalManagerProvider,
            showDisplayText,
            projectInfoMode,
            compareBy,
        ).getGroupTypeTree(instanceDescriptors, groupingMode),
    )
}