package com.krystianwsul.checkme.firebase;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.IgnoreExtraProperties;

import junit.framework.Assert;

import java.io.UnsupportedEncodingException;

@IgnoreExtraProperties
public class UserData implements Parcelable {
    public String email;
    public String displayName;

    @SuppressWarnings("unused")
    public UserData() {

    }

    public UserData(@NonNull FirebaseUser firebaseUser) {
        email = firebaseUser.getEmail();
        Assert.assertTrue(!TextUtils.isEmpty(email));

        displayName = firebaseUser.getDisplayName();
        Assert.assertTrue(!TextUtils.isEmpty(displayName));
    }

    private UserData(@NonNull String email, @NonNull String displayName) {
        Assert.assertTrue(!TextUtils.isEmpty(email));
        Assert.assertTrue(!TextUtils.isEmpty(displayName));

        this.email = email;
        this.displayName = displayName;
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

    @Override
    public int hashCode() {
        return (email.hashCode() + displayName.hashCode());
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

        if (!email.equals(userData.email))
            return false;

        if (!displayName.equals(userData.displayName))
            return false;

        return true;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(email);
        dest.writeString(displayName);
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

            return new UserData(email, displayName);
        }

        @Override
        public UserData[] newArray(int size) {
            return new UserData[size];
        }
    };
}
