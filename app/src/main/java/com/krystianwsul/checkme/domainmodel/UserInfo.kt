package com.krystianwsul.checkme.domainmodel

import android.text.TextUtils
import com.google.firebase.auth.FirebaseUser
import com.krystianwsul.checkme.MyFirebaseInstanceIdService
import com.krystianwsul.checkme.firebase.UserData
import com.krystianwsul.checkme.utils.Utils
import junit.framework.Assert
import java.util.*

class UserInfo(firebaseUser: FirebaseUser) {

    private val email = firebaseUser.email!!

    val name = firebaseUser.displayName!!

    val token = MyFirebaseInstanceIdService.token

    val key by lazy { UserData.getKey(email) }

    init {
        Assert.assertTrue(!TextUtils.isEmpty(email))
        Assert.assertTrue(!TextUtils.isEmpty(name))
    }

    fun getValues(uuid: String): Map<String, Any?> {
        val values = HashMap<String, Any?>()

        values["email"] = email
        values["name"] = name
        values["tokens/$uuid"] = token

        return values
    }

    override fun hashCode(): Int {
        var hash = email.hashCode()
        hash += name.hashCode()
        if (!TextUtils.isEmpty(token))
            hash += token!!.hashCode()
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (other == null)
            return false

        if (other === this)
            return true

        if (other !is UserInfo)
            return false

        if (email != other.email)
            return false

        if (name != other.name)
            return false

        return Utils.stringEquals(token, other.token)
    }
}
