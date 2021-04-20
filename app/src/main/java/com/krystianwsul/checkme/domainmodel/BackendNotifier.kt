package com.krystianwsul.checkme.domainmodel

import android.util.Log
import com.android.volley.NoConnectionError
import com.android.volley.Request
import com.android.volley.ServerError
import com.android.volley.TimeoutError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.common.domain.DeviceInfo
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey

object BackendNotifier {

    private const val PREFIX = "https://us-central1-check-me-add47.cloudfunctions.net/notify?"

    private fun getUrl(projects: Set<ProjectKey<*>>, production: Boolean, userKeys: Collection<UserKey>, senderToken: String): String {
        check(projects.isNotEmpty() || !userKeys.isEmpty())

        val parameters = projects.map { "projects=${it.key}" }.toMutableSet()

        parameters.addAll(userKeys.map { "userKeys=${it.key}" })

        if (production)
            parameters.add("production=1")

        parameters.add("senderToken=$senderToken")

        return PREFIX + parameters.joinToString("&")
    }

    fun notify(projects: Set<Project<*>>, deviceInfo: DeviceInfo, userKeys: Collection<UserKey>) {
        val production = when (AndroidDatabaseWrapper.root) {
            "development" -> false
            "production" -> true
            else -> throw IllegalArgumentException()
        }

        val projectIds = projects.map { it.projectKey }.toSet()

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
                    when (it) {
                        is TimeoutError, is NoConnectionError -> Log.e("asdf", "BackendNotifier error", it)
                        is ServerError -> MyCrashlytics.logException(ServerErrorWrapper("message: ${it.message},  networkResponse: " + it.networkResponse?.let { "code: ${it.statusCode}, data: ${String(it.data)}" }))
                        else -> MyCrashlytics.logException(it)
                    }
                })

        queue.add(stringRequest)
    }

    private class BackendException(message: String) : Exception(message)

    private class ServerErrorWrapper(message: String) : Exception(message)
}
