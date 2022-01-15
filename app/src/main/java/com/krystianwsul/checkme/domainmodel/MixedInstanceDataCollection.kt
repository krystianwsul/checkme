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
        projectProvider: GroupTypeFactory.ProjectProvider,
        groupingMode: GroupType.GroupingMode,
        showDisplayText: Boolean,
        includeProjectDetails: Boolean,
    ) : this(
        instanceDescriptors.map { it.instanceData },
        GroupTypeFactory(
            projectOrdinalManagerProvider,
            projectProvider,
            showDisplayText,
            includeProjectDetails,
        ).getGroupTypeTree(instanceDescriptors, groupingMode),
    )
}