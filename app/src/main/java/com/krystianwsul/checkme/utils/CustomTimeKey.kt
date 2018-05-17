package com.krystianwsul.checkme.utils

import android.os.Parcelable
import android.text.TextUtils
import junit.framework.Assert
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
data class CustomTimeKey(val localCustomTimeId: Int?, val remoteProjectId: String?, val remoteCustomTimeId: String?) : Parcelable, Serializable {

    val type: TaskKey.Type
        get() {
            return if (localCustomTimeId != null) {
                Assert.assertTrue(TextUtils.isEmpty(remoteProjectId))
                Assert.assertTrue(TextUtils.isEmpty(remoteCustomTimeId))

                TaskKey.Type.LOCAL
            } else {
                Assert.assertTrue(!TextUtils.isEmpty(remoteProjectId))
                Assert.assertTrue(!TextUtils.isEmpty(remoteCustomTimeId))

                TaskKey.Type.REMOTE
            }
        }

    init {
        check(remoteProjectId.isNullOrEmpty() == remoteCustomTimeId.isNullOrEmpty())
        check((localCustomTimeId == null) != remoteProjectId.isNullOrEmpty())
    }

    constructor(localCustomTimeId: Int) : this(localCustomTimeId, null, null)

    constructor(remoteProjectId: String, remoteCustomTimeId: String) : this(null, remoteProjectId, remoteCustomTimeId) // only if local custom time doesn't exist

    override fun toString() = "CustomTimeKey $localCustomTimeId - $remoteProjectId/$remoteCustomTimeId"
}
