package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.utils.ProjectType

interface SharedProjectsProvider : ProjectsProvider<ProjectType.Shared, JsonWrapper> {

    val projectProvider: ProjectProvider
}