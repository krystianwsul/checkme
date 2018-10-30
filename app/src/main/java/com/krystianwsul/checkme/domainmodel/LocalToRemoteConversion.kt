package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.domainmodel.local.LocalInstance
import com.krystianwsul.checkme.domainmodel.local.LocalTask
import com.krystianwsul.checkme.domainmodel.local.LocalTaskHierarchy
import com.krystianwsul.checkme.firebase.RemoteTask
import com.krystianwsul.checkme.firebase.RemoteTaskHierarchy

class LocalToRemoteConversion {

    val localTasks = mutableMapOf<Int, Pair<LocalTask, List<LocalInstance>>>()
    val localTaskHierarchies = mutableListOf<LocalTaskHierarchy>()

    val remoteTasks = mutableMapOf<Int, RemoteTask>()
    val remoteTaskHierarchies = mutableListOf<RemoteTaskHierarchy>()
}
