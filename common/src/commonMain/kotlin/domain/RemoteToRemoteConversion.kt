package com.krystianwsul.common.domain

import com.krystianwsul.common.firebase.models.RemoteInstance
import com.krystianwsul.common.firebase.models.RemoteTask
import com.krystianwsul.common.firebase.models.RemoteTaskHierarchy

import com.krystianwsul.common.utils.RemoteCustomTimeId

class RemoteToRemoteConversion<S : RemoteCustomTimeId> {

    val startTasks = mutableMapOf<String, Pair<RemoteTask<S>, List<RemoteInstance<S>>>>()
    val startTaskHierarchies = mutableListOf<RemoteTaskHierarchy<S>>()

    val endTasks = HashMap<String, RemoteTask<*>>()
    val endTaskHierarchies = ArrayList<RemoteTaskHierarchy<*>>()
}
