package com.krystianwsul.checkme.domainmodel

import android.util.Log
import com.android.volley.NoConnectionError
import com.android.volley.Request
import com.android.volley.TimeoutError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.firebase.RemoteProject
import com.krystianwsul.common.domain.DeviceInfo

object BackendNotifier {

    private const val PREFIX = "https://us-central1-check-me-add47.cloudfunctions.net/notify?"

    fun getUrl(projects: Set<String>, production: Boolean, userKeys: Collection<String>, senderToken: String): String {
        check(projects.isNotEmpty() || !userKeys.isEmpty())

        val parameters = projects.map { "projects=$it" }.toMutableSet()

        parameters.addAll(userKeys.map { "userKeys=$it" })

        if (production)
            parameters.add("production=1")

        parameters.add("senderToken=$senderToken")

        return PREFIX + parameters.joinToString("&")
    }

    fun notify(remoteProjects: Set<RemoteProject<*>>, deviceInfo: DeviceInfo, userKeys: Collection<String>) {
        val production = when (AndroidDatabaseWrapper.root) {
            "development" -> false
            "production" -> true
            else -> throw IllegalArgumentException()
        }

        val projectIds = remoteProjects.map { it.id }.toSet()

        val url = getUrl(projectIds, production, userKeys, deviceInfo.token!!)
        check(url.isNotEmpty())

        run(url)
    }

    private fun run(url: String) {
        check(url.isNotEmpty())

        val queue = Volley.newRequestQueue(MyApplication.instance)
        Log.e("asdf", "BackendNotifier url: $url")
        val stringRequest = StringRequest(
                Request.Method.GET,
                url,
                {
                    Log.e("asdf", "BackendNotifier response:")

                    val lines = it.lines()

                    lines.forEach { Log.e("asdf", it) }

                    lines.firstOrNull { it.startsWith("error") }?.let {
                        val error = (lines.indexOf(it) until lines.size).joinToString("\n") { lines[it] }

                        MyCrashlytics.logException(BackendException(error))
                    }
                },
                {
                    if (it is TimeoutError || it is NoConnectionError) {
                        Log.e("asdf", "BackendNotifier error", it)
                    } else {
                        MyCrashlytics.logException(it)
                    }
                })

        queue.add(stringRequest)
    }

    private class BackendException(message: String) : Exception(message)
}
