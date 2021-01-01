package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.DateTimePair
import com.krystianwsul.common.utils.InstanceKey

class EditInstancesUndoData(val data: List<Pair<InstanceKey, Anchor>>) {

    data class Anchor(val parentState: Instance.ParentState, val dateTimePair: DateTimePair?)
}