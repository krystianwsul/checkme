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
    public final String mRemoteProjectId;

    @Nullable
    public final String mRemoteCustomTimeId;

    public CustomTimeKey(int localCustomTimeId) {
        mLocalCustomTimeId = localCustomTimeId;

        mRemoteProjectId = null;
        mRemoteCustomTimeId = null;
    }

    public CustomTimeKey(@NonNull String remoteProjectId, @NonNull String remoteCustomTimeId) { // only if local custom time doesn't exist
        Assert.assertTrue(!TextUtils.isEmpty(remoteProjectId));
        Assert.assertTrue(!TextUtils.isEmpty(remoteCustomTimeId));

        mLocalCustomTimeId = null;
        mRemoteProjectId = remoteProjectId;
        mRemoteCustomTimeId = remoteCustomTimeId;
    }

    @Override
    public int hashCode() {
        if (mLocalCustomTimeId != null) {
            Assert.assertTrue(TextUtils.isEmpty(mRemoteProjectId));
            Assert.assertTrue(TextUtils.isEmpty(mRemoteCustomTimeId));

            return mLocalCustomTimeId;
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(mRemoteProjectId));
            Assert.assertTrue(!TextUtils.isEmpty(mRemoteCustomTimeId));

            return (mRemoteProjectId.hashCode() + mRemoteCustomTimeId.hashCode());
        }
    }

    @SuppressWarnings("RedundantIfStatement")
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
            Assert.assertTrue(TextUtils.isEmpty(mRemoteProjectId));
            Assert.assertTrue(TextUtils.isEmpty(mRemoteCustomTimeId));

            return mLocalCustomTimeId.equals(customTimeKey.mLocalCustomTimeId);
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(mRemoteProjectId));
            Assert.assertTrue(!TextUtils.isEmpty(mRemoteCustomTimeId));

            if (!mRemoteProjectId.equals(customTimeKey.mRemoteProjectId))
                return false;

            if (!mRemoteCustomTimeId.equals(customTimeKey.mRemoteCustomTimeId))
                return false;

            return true;
        }
    }

    @Override
    public String toString() {
        return "CustomTimeKey " + mLocalCustomTimeId + " - " + mRemoteProjectId + "/" + mRemoteCustomTimeId;
    }

    @NonNull
    public TaskKey.Type getType() {
        if (mLocalCustomTimeId != null) {
            Assert.assertTrue(TextUtils.isEmpty(mRemoteProjectId));
            Assert.assertTrue(TextUtils.isEmpty(mRemoteCustomTimeId));

            return TaskKey.Type.LOCAL;
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(mRemoteProjectId));
            Assert.assertTrue(!TextUtils.isEmpty(mRemoteCustomTimeId));

            return TaskKey.Type.REMOTE;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (mLocalCustomTimeId != null) {
            Assert.assertTrue(TextUtils.isEmpty(mRemoteProjectId));
            Assert.assertTrue(TextUtils.isEmpty(mRemoteCustomTimeId));

            dest.writeInt(1);
            dest.writeInt(mLocalCustomTimeId);
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(mRemoteProjectId));
            Assert.assertTrue(!TextUtils.isEmpty(mRemoteCustomTimeId));

            dest.writeInt(0);
            dest.writeString(mRemoteProjectId);
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
                String remoteProjectId = in.readString();
                Assert.assertTrue(!TextUtils.isEmpty(remoteProjectId));

                String remoteCustomTimeId = in.readString();
                Assert.assertTrue(!TextUtils.isEmpty(remoteCustomTimeId));

                return new CustomTimeKey(remoteProjectId, remoteCustomTimeId);
            }
        }

        @Override
        public CustomTimeKey[] newArray(int size) {
            return new CustomTimeKey[size];
        }
    };
}
