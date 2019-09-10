/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Servlet Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloWorld
*/

package com.krystianwsul.checkme.backend

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.common.base.Joiner
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.krystianwsul.common.Notification
import com.krystianwsul.common.Response
import com.krystianwsul.common.firebase.json.UserJson
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.collections.HashMap


class NotificationServlet : HttpServlet() {

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val productionParam = req.getParameter("production")
        val production = !StringUtils.isEmpty(productionParam)

        resp.writer.println("production? $production")
        resp.writer.println()

        val prefix = if (production) "production" else "development"

        val scoped = GoogleCredential.getApplicationDefault().createScoped(listOf(
                "https://www.googleapis.com/auth/firebase.database",
                "https://www.googleapis.com/auth/userinfo.email"
        ))

        scoped.refreshToken()
        val firebaseToken = scoped.accessToken
        check(!StringUtils.isEmpty(firebaseToken))

        /*
        val resource = servletContext.getResource("/WEB-INF/check-me-add47-firebase-adminsdk-ajfq4-dfade3b2a9.json")
        val file = File(resource.toURI())
        val input = FileInputStream(file)

        val options = FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(input))
                .setDatabaseUrl("https://check-me-add47.firebaseio.com/")
                .build()

        try {
            FirebaseApp.initializeApp(options)
        } catch (e : IllegalStateException) {
            // already initialized
        }
        val defaultDatabase = FirebaseDatabase.getInstance()
        */

        val gson = Gson()

        val senderToken = req.getParameter("senderToken")

        val projectTokenData = mutableMapOf<String, Triple<String, String, String>>()

        val userTokens = HashSet<String>()
        if (req.getParameterValues("projects") != null) {
            val projects = HashSet(listOf(*req.getParameterValues("projects")))
            check(projects.isNotEmpty())

            resp.writer.println("projects: $projects")
            resp.writer.println()

            for (project in projects) {
                check(!StringUtils.isEmpty(project))

                val usersUrl = URL("https://check-me-add47.firebaseio.com/$prefix/records/$project/projectJson/users.json?access_token=$firebaseToken")

                val usersReader = BufferedReader(InputStreamReader(usersUrl.openStream()))
                val typeToken = object : TypeToken<HashMap<String, UserJson>>() {}.type
                val users = gson.fromJson<HashMap<String, UserJson>>(usersReader, typeToken)
                usersReader.close()

                if (users == null)
                    throw NoUsersException(usersUrl.toString())

                resp.writer.println("project user keys: " + Joiner.on(", ").join(users.keys))

                for ((userKey, user) in users) {
                    val userTokenMap = user.tokens

                    val tokens = ArrayList<String>(userTokenMap.values)

                    projectTokenData.putAll(userTokenMap.entries.associate { (uuid, token) -> token!! to Triple(project, userKey, uuid) })

                    userTokens.addAll(tokens)
                }
            }
        }

        val userTokenData = mutableMapOf<String, Pair<String, String>>()

        if (req.getParameterValues("userKeys") != null) {
            val userKeys = HashSet(listOf(*req.getParameterValues("userKeys")))
            check(userKeys.isNotEmpty())

            resp.writer.print("root user keys: " + Joiner.on(", ").join(userKeys))
            resp.writer.println()

            for (userKey in userKeys) {
                check(!StringUtils.isEmpty(userKey))

                val tokenUrl = URL("https://check-me-add47.firebaseio.com/$prefix/users/$userKey/userData/tokens.json?access_token=$firebaseToken")

                resp.writer.println(tokenUrl.toString())
                resp.writer.println()

                val tokenReader = BufferedReader(InputStreamReader(tokenUrl.openStream()))
                val userTokenMap = gson.fromJson<HashMap<String, String>>(tokenReader, HashMap<String, String>().javaClass)
                tokenReader.close()

                if (userTokenMap == null) {
                    resp.writer.println("empty, skipping")
                    continue
                }

                val tokens = ArrayList(userTokenMap.values)

                userTokenData.putAll(userTokenMap.entries.associate { (uuid, token) -> token to Pair(userKey, uuid) })

                resp.writer.println("user tokens: $tokens")
                userTokens.addAll(tokens)
            }
        }

        val prunedUserTokens = ArrayList(userTokens)

        prunedUserTokens.remove(senderToken)

        if (prunedUserTokens.isEmpty()) {
            resp.writer.println("no user tokens, exiting")
            return
        }

        val fcmUrl = URL("https://fcm.googleapis.com/fcm/send")
        val urlConnection = fcmUrl.openConnection()
        val httpURLConnection = urlConnection as HttpURLConnection

        httpURLConnection.setRequestProperty("Authorization", "key=AAAACS58vvk:APA91bGMSthxVrK-Tw9Kht63VM09uw2TBbCZLg6Y1utntVFLy4PGfjsvxm2QK830JGO_S87yvaxeDByMzWRqGBPXzqBpEMZPbWOUHDnYvSQXF_KllfCpcn17UBIKE9RPAXzhwkk3CqYEWvbxZCvl4L_MYodKHfhNMQ")
        httpURLConnection.setRequestProperty("Content-Type", "application/json")

        httpURLConnection.doOutput = true
        httpURLConnection.requestMethod = "POST"

        val outputStreamWriter = OutputStreamWriter(httpURLConnection.outputStream)
        outputStreamWriter.write(gson.toJson(Notification(prunedUserTokens)))
        outputStreamWriter.close()

        val respCode = httpURLConnection.responseCode
        if (respCode == HttpsURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(httpURLConnection.inputStream))
            val response = IOUtils.toString(reader)
            IOUtils.closeQuietly(reader)

            val parsedResponse = gson.fromJson(response, Response::class.java)
            resp.writer.println("parsed response: $parsedResponse")
            resp.writer.println()

            /*
            val removeTokens = parsedResponse.results
                    .mapIndexed { index, result -> Pair(index, result) }
                    .filterNot { it.second.error.isNullOrEmpty() }
                    .map { prunedUserTokens[it.first] }

            resp.writer.println("tokens to remove: $removeTokens")

            val removeProjectUsers = removeTokens.mapNotNull { projectTokenData[it] }.map { (project, userKey, uuid) ->
                "$prefix/records/$project/projectJson/users/$userKey/tokens/$uuid"
            }

            val removeRootUsers = removeTokens.mapNotNull { userTokenData[it] }.map { (userKey, uuid) ->
                "$prefix/users/$userKey/userData/tokens/$uuid"
            }

            resp.writer.println("project users to remove:\n" + removeProjectUsers.joinToString("\n"))
            resp.writer.println()

            resp.writer.println("root users to remove:\n" + removeRootUsers.joinToString("\n"))
            resp.writer.println()

            val map = (removeProjectUsers + removeRootUsers).associate { it to null }

            defaultDatabase.reference.updateChildrenAsync(map).get()
            */
        } else {
            resp.writer.println("error: " + httpURLConnection.responseCode + " " + httpURLConnection.responseMessage)

            val reader = BufferedReader(InputStreamReader(httpURLConnection.inputStream))
            val response = IOUtils.toString(reader)
            IOUtils.closeQuietly(reader)

            resp.writer.println("response: $response")
            resp.writer.println()
        }
    }

    private class NoUsersException internal constructor(url: String) : RuntimeException("url: $url")
}
