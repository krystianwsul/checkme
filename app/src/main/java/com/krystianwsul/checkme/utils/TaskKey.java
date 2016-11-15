package com.krystianwsul.checkme.utils;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import junit.framework.Assert;

import java.io.Serializable;

public class TaskKey implements Parcelable, Serializable {
    @Nullable
    public final Integer mLocalTaskId;

    @Nullable
    public final String mRemoteTaskId;

    public TaskKey(int localTaskId) {
        mLocalTaskId = localTaskId;
        mRemoteTaskId = null;
    }

    public TaskKey(@NonNull String remoteTaskId) {
        Assert.assertTrue(!TextUtils.isEmpty(remoteTaskId));

        mLocalTaskId = null;
        mRemoteTaskId = remoteTaskId;
    }

    @Override
    public int hashCode() {
        if (mLocalTaskId != null) {
            Assert.assertTrue(TextUtils.isEmpty(mRemoteTaskId));

            return mLocalTaskId;
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(mRemoteTaskId));

            return mRemoteTaskId.hashCode();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (obj == this)
            return true;

        if (!(obj instanceof TaskKey))
            return false;

        TaskKey taskKey = (TaskKey) obj;

        if (mLocalTaskId != null) {
            Assert.assertTrue(TextUtils.isEmpty(mRemoteTaskId));

            return mLocalTaskId.equals(taskKey.mLocalTaskId);
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(mRemoteTaskId));

            return mRemoteTaskId.equals(taskKey.mRemoteTaskId);
        }
    }

    @Override
    public String toString() {
        return super.toString() + ": " + mLocalTaskId + ", " + mRemoteTaskId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (mLocalTaskId != null) {
            Assert.assertTrue(TextUtils.isEmpty(mRemoteTaskId));

            dest.writeInt(1);
            dest.writeInt(mLocalTaskId);
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(mRemoteTaskId));

            dest.writeInt(0);
            dest.writeString(mRemoteTaskId);
        }
    }

    public static final Creator<TaskKey> CREATOR = new Creator<TaskKey>() {
        @Override
        public TaskKey createFromParcel(Parcel in) {
            if (in.readInt() == 1) {
                int localTaskId = in.readInt();

                return new TaskKey(localTaskId);
            } else {
                String remoteTaskId = in.readString();
                Assert.assertTrue(!TextUtils.isEmpty(remoteTaskId));

                return new TaskKey(remoteTaskId);
            }
        }

        @Override
        public TaskKey[] newArray(int size) {
            return new TaskKey[size];
        }
    };
}
