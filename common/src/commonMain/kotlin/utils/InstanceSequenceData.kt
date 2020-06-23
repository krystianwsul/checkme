package com.krystianwsul.common.utils

import com.krystianwsul.common.firebase.models.Instance

class InstanceSequenceData<T : ProjectType>(val instances: Sequence<Instance<T>>, val hasMore: Boolean?)