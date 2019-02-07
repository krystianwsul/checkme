package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.domainmodel.local.LocalCustomTime
import com.krystianwsul.checkme.firebase.RemoteCustomTime
import com.krystianwsul.checkme.firebase.RemoteProject

@Suppress("unused")
class Irrelevant(
        val localCustomTimes: List<LocalCustomTime>,
        val tasks: List<Task>,
        val instances: List<Instance>,
        val remoteCustomTimes: List<RemoteCustomTime<*>>?,
        val remoteProjects: List<RemoteProject<*>>?)
