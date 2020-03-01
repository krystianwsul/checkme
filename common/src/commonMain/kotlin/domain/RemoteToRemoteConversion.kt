package com.krystianwsul.common.domain

import com.krystianwsul.common.firebase.models.RemoteInstance
import com.krystianwsul.common.firebase.models.RemoteTask
import com.krystianwsul.common.firebase.models.RemoteTaskHierarchy
import com.krystianwsul.common.utils.ProjectKey

import com.krystianwsul.common.utils.RemoteCustomTimeId

class RemoteToRemoteConversion<S : RemoteCustomTimeId, T : ProjectKey> {

    val startTasks = mutableMapOf<String, Pair<RemoteTask<S, T>, List<RemoteInstance<S, T>>>>()
    val startTaskHierarchies = mutableListOf<RemoteTaskHierarchy<S, T>>()

    val endTasks = HashMap<String, RemoteTask<*, *>>()
    val endTaskHierarchies = ArrayList<RemoteTaskHierarchy<*, *>>()
}
