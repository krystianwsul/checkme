package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.firebase.RemoteCustomTime
import com.krystianwsul.checkme.firebase.RemoteProject

@Suppress("unused")
class Irrelevant(
        val tasks: List<Task>,
        val instances: List<Instance>,
        val remoteCustomTimes: List<RemoteCustomTime<*>>?,
        val remoteProjects: List<RemoteProject<*>>?)
