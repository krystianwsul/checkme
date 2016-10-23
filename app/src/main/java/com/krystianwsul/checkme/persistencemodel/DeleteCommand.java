package com.krystianwsul.checkme.persistencemodel;

import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import junit.framework.Assert;

public class DeleteCommand implements Parcelable {
    private final String mTableName;
    private final String mWhereClause;

    public DeleteCommand(@NonNull String tableName, @NonNull String whereClause) {
        Assert.assertTrue(!TextUtils.isEmpty(tableName));
        Assert.assertTrue(!TextUtils.isEmpty(whereClause));

        mTableName = tableName;
        mWhereClause = whereClause;
    }

    public void execute(@NonNull SQLiteDatabase sqLiteDatabase) {
        int deleted = sqLiteDatabase.delete(mTableName, mWhereClause, null);

        if (deleted != 1)
            throw new IllegalStateException("mTableName == " + mTableName + ", mWhereClause == " + mWhereClause + ", deleted == " + deleted);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mTableName);
        out.writeString(mWhereClause);
    }

    public static final Creator<DeleteCommand> CREATOR = new Creator<DeleteCommand>() {
        @Override
        public DeleteCommand createFromParcel(Parcel source) {
            String tableName = source.readString();
            Assert.assertTrue(!TextUtils.isEmpty(tableName));

            String whereClause = source.readString();
            Assert.assertTrue(!TextUtils.isEmpty(whereClause));

            return new DeleteCommand(tableName, whereClause);
        }

        @Override
        public DeleteCommand[] newArray(int size) {
            return new DeleteCommand[size];
        }
    };
}
