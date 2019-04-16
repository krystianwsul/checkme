/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Servlet Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloWorld
*/

package com.krystianwsul.checkme.backend

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.common.base.Joiner
import com.google.gson.Gson
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

class NotificationServlet : HttpServlet() {

    public override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
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

        resp.writer.println("firebase token: $firebaseToken")
        resp.writer.println()

        val gson = Gson()

        val senderToken = req.getParameter("senderToken")
        resp.writer.println("sender token: $senderToken")
        resp.writer.println()

        val userTokens = HashSet<String>()
        if (req.getParameterValues("projects") != null) {
            val projects = HashSet(Arrays.asList(*req.getParameterValues("projects")))
            check(projects.isNotEmpty())

            resp.writer.print("projects: " + Joiner.on(", ").join(projects))
            resp.writer.println()

            for (project in projects) {
                check(!StringUtils.isEmpty(project))

                val usersUrl = URL("https://check-me-add47.firebaseio.com/$prefix/records/$project/projectJson/users.json?access_token=$firebaseToken")

                resp.writer.println(usersUrl.toString())
                resp.writer.println()

                val usersReader = BufferedReader(InputStreamReader(usersUrl.openStream()))
                val users = gson.fromJson<HashMap<String, Map<String, Map<String, String>>>>(usersReader, HashMap<String, Map<String, Map<String, String>>>().javaClass)
                usersReader.close()

                if (users == null)
                    throw NoUsersException(usersUrl.toString())

                resp.writer.println("user keys before removing sender: " + Joiner.on(", ").join(users.keys))

                resp.writer.println("user keys after removing sender: " + Joiner.on(", ").join(users.keys))

                for (user in users.values) {
                    val userTokenMap = user["tokens"]

                    if (userTokenMap == null) {
                        resp.writer.println("user/tokens is null")
                        continue
                    }

                    val tokens = ArrayList<String>(userTokenMap.values)

                    resp.writer.println("user/tokens: $tokens")
                    userTokens.addAll(tokens)
                }
            }
        }

        if (req.getParameterValues("userKeys") != null) {
            val userKeys = HashSet(Arrays.asList(*req.getParameterValues("userKeys")))
            check(userKeys.isNotEmpty())

            resp.writer.print("userKeys: " + Joiner.on(", ").join(userKeys))
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

                val tokens = ArrayList<String>(userTokenMap.values)

                resp.writer.println("user tokens: $tokens")
                userTokens.addAll(tokens)
            }
        }

        val prunedUserTokens = ArrayList(userTokens)

        resp.writer.println("user tokens before removing sender: $prunedUserTokens")

        prunedUserTokens.remove(senderToken)

        resp.writer.println("user tokens after removing sender: $prunedUserTokens")

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

            resp.writer.println("response: $response")
            resp.writer.println()
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
