package com.krystianwsul.checkme.utils;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import junit.framework.Assert;

import java.io.Serializable;

public class CustomTimeKey implements Parcelable, Serializable {
    @Nullable
    public final Integer mLocalCustomTimeId;

    @Nullable
    public final String mRemoteCustomTimeId;

    public CustomTimeKey(int localCustomTimeId) {
        mLocalCustomTimeId = localCustomTimeId;
        mRemoteCustomTimeId = null;
    }

    public CustomTimeKey(@NonNull String remoteCustomTimeId) {
        Assert.assertTrue(!TextUtils.isEmpty(remoteCustomTimeId));

        mLocalCustomTimeId = null;
        mRemoteCustomTimeId = remoteCustomTimeId;
    }

    @Override
    public int hashCode() {
        if (mLocalCustomTimeId != null) {
            Assert.assertTrue(TextUtils.isEmpty(mRemoteCustomTimeId));

            return mLocalCustomTimeId;
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(mRemoteCustomTimeId));

            return mRemoteCustomTimeId.hashCode();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (obj == this)
            return true;

        if (!(obj instanceof CustomTimeKey))
            return false;

        CustomTimeKey customTimeKey = (CustomTimeKey) obj;

        if (mLocalCustomTimeId != null) {
            Assert.assertTrue(TextUtils.isEmpty(mRemoteCustomTimeId));

            return mLocalCustomTimeId.equals(customTimeKey.mLocalCustomTimeId);
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(mRemoteCustomTimeId));

            return mRemoteCustomTimeId.equals(customTimeKey.mRemoteCustomTimeId);
        }
    }

    @Override
    public String toString() {
        return "CustomTimeKey " + mLocalCustomTimeId + " - " + mRemoteCustomTimeId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (mLocalCustomTimeId != null) {
            Assert.assertTrue(TextUtils.isEmpty(mRemoteCustomTimeId));

            dest.writeInt(1);
            dest.writeInt(mLocalCustomTimeId);
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(mRemoteCustomTimeId));

            dest.writeInt(0);
            dest.writeString(mRemoteCustomTimeId);
        }
    }

    public static final Creator<CustomTimeKey> CREATOR = new Creator<CustomTimeKey>() {
        @Override
        public CustomTimeKey createFromParcel(Parcel in) {
            if (in.readInt() == 1) {
                int localCustomTimeId = in.readInt();

                return new CustomTimeKey(localCustomTimeId);
            } else {
                String remoteCustomTimeId = in.readString();
                Assert.assertTrue(!TextUtils.isEmpty(remoteCustomTimeId));

                return new CustomTimeKey(remoteCustomTimeId);
            }
        }

        @Override
        public CustomTimeKey[] newArray(int size) {
            return new CustomTimeKey[size];
        }
    };
}
