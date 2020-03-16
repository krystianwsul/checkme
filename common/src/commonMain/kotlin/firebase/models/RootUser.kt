package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.RootUserProperties
import com.krystianwsul.common.firebase.records.RootUserRecord


open class RootUser(private val remoteRootUserRecord: RootUserRecord) : RootUserProperties by remoteRootUserRecord