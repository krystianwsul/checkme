package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.firebase.RemoteInstance
import com.krystianwsul.checkme.firebase.RemoteTask
import com.krystianwsul.checkme.firebase.RemoteTaskHierarchy
import com.krystianwsul.checkme.utils.RemoteCustomTimeId

class RemoteToRemoteConversion<S : RemoteCustomTimeId> {

    val startTasks = mutableMapOf<String, Pair<RemoteTask<S>, List<RemoteInstance<S>>>>()
    val startTaskHierarchies = mutableListOf<RemoteTaskHierarchy<S>>()

    val endTasks = HashMap<String, RemoteTask<*>>()
    val endTaskHierarchies = ArrayList<RemoteTaskHierarchy<*>>()
}
