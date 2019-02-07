package com.krystianwsul.checkme.domainmodel

import android.util.Log
import com.android.volley.NoConnectionError
import com.android.volley.Request
import com.android.volley.TimeoutError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.RemoteProject

object BackendNotifier {

    private const val PREFIX = "https://check-me-add47.appspot.com/notify?"

    fun getUrl(projects: Set<String>, production: Boolean, userKeys: Collection<String>, senderToken: String?): String {
        check(!projects.isEmpty() || !userKeys.isEmpty())

        val parameters = projects.map { "projects=$it" }.toMutableSet()

        parameters.addAll(userKeys.map { "userKeys=$it" })

        if (production)
            parameters.add("production=1")

        parameters.add("senderToken=" + senderToken!!)

        return PREFIX + parameters.joinToString("&")
    }

    fun notify(remoteProjects: Set<RemoteProject<*>>, userInfo: UserInfo, userKeys: Collection<String>) {
        val root = DatabaseWrapper.root

        val production = when (root) {
            "development" -> false
            "production" -> true
            else -> throw IllegalArgumentException()
        }

        val projectIds = remoteProjects.map { it.id }.toSet()

        val url = getUrl(projectIds, production, userKeys, userInfo.token)
        check(url.isNotEmpty())

        run(url)
    }

    private fun run(url: String) {
        check(url.isNotEmpty())

        val queue = Volley.newRequestQueue(MyApplication.instance)

        val stringRequest = StringRequest(
                Request.Method.GET, url,
                { Log.e("asdf", "BackendNotifier response:$it") }
                , {
            if (it is TimeoutError || it is NoConnectionError) {
                Log.e("asdf", "BackendNotifier error", it)
            } else {
                MyCrashlytics.logException(it)
            }
        })

        queue.add(stringRequest)
    }
}
