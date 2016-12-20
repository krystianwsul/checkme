package com.krystianwsul.checkme.firebase;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.krystianwsul.checkme.MyFirebaseInstanceIdService;
import com.krystianwsul.checkme.firebase.json.UserJson;
import com.krystianwsul.checkme.utils.Utils;

import junit.framework.Assert;

import java.io.UnsupportedEncodingException;

@IgnoreExtraProperties
public class UserData implements Parcelable {
    @Nullable
    private String email;

    @Nullable
    private String displayName;

    @Nullable
    private String token;

    @SuppressWarnings("unused")
    public UserData() {

    }

    public UserData(@NonNull FirebaseUser firebaseUser) {
        email = firebaseUser.getEmail();
        Assert.assertTrue(!TextUtils.isEmpty(email));

        displayName = firebaseUser.getDisplayName();
        Assert.assertTrue(!TextUtils.isEmpty(displayName));

        token = MyFirebaseInstanceIdService.getToken();
    }

    private UserData(@NonNull String email, @NonNull String displayName, @Nullable String token) {
        Assert.assertTrue(!TextUtils.isEmpty(email));
        Assert.assertTrue(!TextUtils.isEmpty(displayName));

        this.email = email;
        this.displayName = displayName;
        this.token = token;
    }

    @NonNull
    public String getEmail() {
        Assert.assertTrue(!TextUtils.isEmpty(email));

        return email;
    }

    @NonNull
    public String getDisplayName() {
        Assert.assertTrue(!TextUtils.isEmpty(displayName));

        return displayName;
    }

    @SuppressWarnings("WeakerAccess")
    @Nullable
    public String getToken() {
        return token;
    }

    @NonNull
    public static String getKey(@NonNull String email) {
        Assert.assertTrue(!TextUtils.isEmpty(email));

        try {
            byte[] encoded = email.trim().toLowerCase().getBytes("UTF-8");
            return Base64.encodeToString(encoded, Base64.URL_SAFE | Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    @Exclude
    public String getKey() {
        return getKey(getEmail());
    }

    @NonNull
    @Exclude
    UserJson toUserJson() {
        return new UserJson(getEmail(), getDisplayName(), token);
    }

    @Override
    public int hashCode() {
        int hash = getEmail().hashCode();
        hash += getDisplayName().hashCode();
        if (!TextUtils.isEmpty(token))
            hash += token.hashCode();
        return hash;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (obj == this)
            return true;

        if (!(obj instanceof UserData))
            return false;

        UserData userData = (UserData) obj;

        if (!getEmail().equals(userData.getEmail()))
            return false;

        if (!getDisplayName().equals(userData.getDisplayName()))
            return false;

        if (TextUtils.isEmpty(token) != TextUtils.isEmpty(userData.token))
            return false;

        if (!TextUtils.isEmpty(token) && !token.equals(userData.getToken()))
            return false;

        return true;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(email);
        dest.writeString(displayName);
        Utils.writeStringToParcel(dest, token);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<UserData> CREATOR = new Creator<UserData>() {
        @Override
        public UserData createFromParcel(Parcel in) {
            String email = in.readString();
            Assert.assertTrue(!TextUtils.isEmpty(email));

            String displayName = in.readString();
            Assert.assertTrue(!TextUtils.isEmpty(displayName));

            String token = Utils.readStringFromParcel(in);

            return new UserData(email, displayName, token);
        }

        @Override
        public UserData[] newArray(int size) {
            return new UserData[size];
        }
    };
}
