package com.krystianwsul.common.relevance

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.utils.InstanceKey

fun MutableMap<InstanceKey, InstanceRelevance>.getOrPut(instance: Instance) =
    getOrPut(instance.instanceKey) { InstanceRelevance(instance) }