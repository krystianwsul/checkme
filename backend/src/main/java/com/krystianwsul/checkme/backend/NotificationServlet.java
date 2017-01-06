/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Servlet Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloWorld
*/

package com.krystianwsul.checkme.backend;

import com.google.appengine.repackaged.com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.appengine.repackaged.com.google.gson.Gson;
import com.google.common.base.Joiner;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class NotificationServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String productionParam = req.getParameter("production");
        boolean production = !StringUtils.isEmpty(productionParam);

        resp.getWriter().println("production? " + production);
        resp.getWriter().println();

        String prefix = (production ? "production" : "development");

        GoogleCredential googleCred = GoogleCredential.fromStream(new FileInputStream("WEB-INF/check-me-add47-firebase-adminsdk-5hvuz-436ce98200.json"));
        Assert.assertTrue(googleCred != null);

        GoogleCredential scoped = googleCred.createScoped(
                Arrays.asList(
                        "https://www.googleapis.com/auth/firebase.database",
                        "https://www.googleapis.com/auth/userinfo.email"
                )
        );
        Assert.assertTrue(scoped != null);

        scoped.refreshToken();
        String firebaseToken = scoped.getAccessToken();
        Assert.assertTrue(!StringUtils.isEmpty(firebaseToken));

        resp.getWriter().println("firebase token: " + firebaseToken);
        resp.getWriter().println();

        Gson gson = new Gson();

        Set<String> userTokens = new HashSet<>();
        if (req.getParameterValues("projects") != null) {
            List<String> projects = Arrays.asList(req.getParameterValues("projects"));
            Assert.assertTrue(!projects.isEmpty());

            resp.getWriter().print("projects: " + Joiner.on(", ").join(projects));
            resp.getWriter().println();

            String sender = req.getParameter("sender");
            resp.getWriter().println("sender: " + sender);
            resp.getWriter().println();

            Set<String> userKeys = new HashSet<>();
            for (String project : projects) {
                Assert.assertTrue(!StringUtils.isEmpty(project));

                URL usersUrl = new URL("https://check-me-add47.firebaseio.com/" + prefix + "/records/" + project + "/projectJson/users.json?access_token=" + firebaseToken);

                resp.getWriter().println(usersUrl.toString());
                resp.getWriter().println();

                BufferedReader usersReader = new BufferedReader(new InputStreamReader(usersUrl.openStream()));
                @SuppressWarnings("InstantiatingObjectToGetClassObject") Map<String, Map<String, String>> users = gson.fromJson(usersReader, new HashMap<String, Map<String, String>>().getClass());
                usersReader.close();

                if (users == null)
                    throw new NoUsersException(usersUrl.toString());

                userKeys.addAll(users.keySet());

                resp.getWriter().println("user keys before removing sender: " + Joiner.on(", ").join(userKeys));

                if (!StringUtils.isEmpty(sender))
                    userKeys.remove(sender);

                resp.getWriter().println("user keys after removing sender: " + Joiner.on(", ").join(userKeys));
            }

            for (String userKey : userKeys) {
                Assert.assertTrue(!StringUtils.isEmpty(userKey));

                URL tokenUrl = new URL("https://check-me-add47.firebaseio.com/" + prefix + "/users/" + userKey + "/userData/token.json?access_token=" + firebaseToken);

                resp.getWriter().println(tokenUrl.toString());
                resp.getWriter().println();

                BufferedReader tokenReader = new BufferedReader(new InputStreamReader(tokenUrl.openStream()));
                String userToken = gson.fromJson(tokenReader, String.class);
                tokenReader.close();

                resp.getWriter().println("user token: " + userToken);
                if (StringUtils.isEmpty(userToken)) {
                    resp.getWriter().println("empty, skipping");
                    resp.getWriter().println();
                } else {
                    resp.getWriter().println();

                    userTokens.add(userToken);
                }
            }
        } else {
            Set<String> userKeys = new HashSet<>(Arrays.asList(req.getParameterValues("userKeys")));
            Assert.assertTrue(!userKeys.isEmpty());

            resp.getWriter().print("userKeys: " + Joiner.on(", ").join(userKeys));
            resp.getWriter().println();

            for (String userKey : userKeys) {
                Assert.assertTrue(!StringUtils.isEmpty(userKey));

                URL tokenUrl = new URL("https://check-me-add47.firebaseio.com/" + prefix + "/users/" + userKey + "/userData/token.json?access_token=" + firebaseToken);

                resp.getWriter().println(tokenUrl.toString());
                resp.getWriter().println();

                BufferedReader tokenReader = new BufferedReader(new InputStreamReader(tokenUrl.openStream()));
                String userToken = gson.fromJson(tokenReader, String.class);
                tokenReader.close();

                resp.getWriter().println("user token: " + userToken);
                if (StringUtils.isEmpty(userToken)) {
                    resp.getWriter().println("empty, skipping");
                    resp.getWriter().println();
                } else {
                    resp.getWriter().println();

                    userTokens.add(userToken);
                }
            }
        }

        for (String userToken : userTokens) {
            Assert.assertTrue(!StringUtils.isEmpty(userToken));

            URL fcmUrl = new URL("https://fcm.googleapis.com/fcm/send");
            URLConnection urlConnection = fcmUrl.openConnection();
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;

            httpURLConnection.setRequestProperty("Authorization", "key=AAAACS58vvk:APA91bGMSthxVrK-Tw9Kht63VM09uw2TBbCZLg6Y1utntVFLy4PGfjsvxm2QK830JGO_S87yvaxeDByMzWRqGBPXzqBpEMZPbWOUHDnYvSQXF_KllfCpcn17UBIKE9RPAXzhwkk3CqYEWvbxZCvl4L_MYodKHfhNMQ");
            httpURLConnection.setRequestProperty("Content-Type", "application/json");

            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestMethod("POST");

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(httpURLConnection.getOutputStream());
            outputStreamWriter.write(gson.toJson(new Notification(userToken)));
            outputStreamWriter.close();

            int respCode = httpURLConnection.getResponseCode();
            if (respCode == HttpsURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                String response = IOUtils.toString(reader);
                IOUtils.closeQuietly(reader);

                resp.getWriter().println("response: " + response);
                resp.getWriter().println();
            } else {
                resp.getWriter().println("error: " + httpURLConnection.getResponseCode() + " " + httpURLConnection.getResponseMessage());

                BufferedReader reader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                String response = IOUtils.toString(reader);
                IOUtils.closeQuietly(reader);

                resp.getWriter().println("response: " + response);
                resp.getWriter().println();
            }
        }
    }

    private static class NoUsersException extends RuntimeException {
        NoUsersException(String url) {
            super("url: " + url);
        }
    }
}
