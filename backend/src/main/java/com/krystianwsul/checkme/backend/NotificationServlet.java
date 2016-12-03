/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Servlet Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloWorld
*/

package com.krystianwsul.checkme.backend;

import com.google.appengine.repackaged.com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.appengine.repackaged.com.google.gson.Gson;
import com.google.common.base.Joiner;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class NotificationServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<String> projects = Arrays.asList(req.getParameterValues("projects"));

        resp.getWriter().print("projects: " + Joiner.on(", ").join(projects));
        resp.getWriter().println();

        GoogleCredential googleCred = GoogleCredential.fromStream(new FileInputStream("WEB-INF/check-me-add47-firebase-adminsdk-5hvuz-436ce98200.json"));
        GoogleCredential scoped = googleCred.createScoped(
                Arrays.asList(
                        "https://www.googleapis.com/auth/firebase.database",
                        "https://www.googleapis.com/auth/userinfo.email"
                )
        );

        scoped.refreshToken();
        String firebaseToken = scoped.getAccessToken();

        resp.getWriter().println("firebase token: " + firebaseToken);
        resp.getWriter().println();

        Gson gson = new Gson();

        for (String project : projects) {
            URL recordOfUrl = new URL("https://check-me-add47.firebaseio.com/development/records/" + project + "/recordOf.json?access_token=" + firebaseToken);

            resp.getWriter().println(recordOfUrl.toString());
            resp.getWriter().println();

            BufferedReader recordOfReader = new BufferedReader(new InputStreamReader(recordOfUrl.openStream()));
            @SuppressWarnings("InstantiatingObjectToGetClassObject") Map<String, Boolean> recordOf = gson.fromJson(recordOfReader, new HashMap<String, Boolean>().getClass());
            recordOfReader.close();

            Set<String> userKeys = recordOf.keySet();

            resp.getWriter().println("user keys: " + Joiner.on(", ").join(userKeys));

            for (String userKey : userKeys) {
                URL tokenUrl = new URL("https://check-me-add47.firebaseio.com/development/users/" + userKey + "/userData/token.json?access_token=" + firebaseToken);

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

                    HttpClient httpClient = HttpClientBuilder.create().build();

                    HttpPost httpPost = new HttpPost("https://fcm.googleapis.com/fcm/send");
                    StringEntity params = new StringEntity(gson.toJson(new Notification(userToken)));
                    httpPost.addHeader("Authorization", "key=AAAACS58vvk:APA91bGMSthxVrK-Tw9Kht63VM09uw2TBbCZLg6Y1utntVFLy4PGfjsvxm2QK830JGO_S87yvaxeDByMzWRqGBPXzqBpEMZPbWOUHDnYvSQXF_KllfCpcn17UBIKE9RPAXzhwkk3CqYEWvbxZCvl4L_MYodKHfhNMQ");
                    httpPost.addHeader("Content-Type", "application/json");
                    httpPost.setEntity(params);

                    resp.getWriter().println("request: " + httpPost);
                    resp.getWriter().println();

                    HttpResponse response = httpClient.execute(httpPost);

                    resp.getWriter().println("response" + response);
                    resp.getWriter().println();
                }
            }
        }
    }
}
