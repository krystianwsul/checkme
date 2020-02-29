package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.records.RemoteRootUserRecord


open class RemoteRootUser(private val remoteRootUserRecord: RemoteRootUserRecord) : RemoteRootUserProperties by remoteRootUserRecord