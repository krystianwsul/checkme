package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.firebase.RemoteInstance
import com.krystianwsul.checkme.firebase.RemoteTask
import com.krystianwsul.checkme.firebase.RemoteTaskHierarchy

class RemoteToRemoteConversion {

    val startTasks = mutableMapOf<String, Pair<RemoteTask<*>, List<RemoteInstance<*>>>>()
    val startTaskHierarchies = mutableListOf<RemoteTaskHierarchy>()

    val endTasks = mutableMapOf<String, RemoteTask<*>>()
    val endTaskHierarchies = mutableListOf<RemoteTaskHierarchy>()
}
