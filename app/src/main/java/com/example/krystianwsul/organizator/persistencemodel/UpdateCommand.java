package com.example.krystianwsul.organizator.persistencemodel;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import junit.framework.Assert;

public class UpdateCommand implements Parcelable {
    public final String mTableName;
    public final ContentValues mContentValues;
    public final String mWhereClause;

    public UpdateCommand(String tableName, ContentValues contentValues, String whereClause) {
        Assert.assertTrue(!TextUtils.isEmpty(tableName));
        Assert.assertTrue(contentValues != null);
        Assert.assertTrue(contentValues.size() > 0);
        Assert.assertTrue(!TextUtils.isEmpty(whereClause));

        mTableName = tableName;
        mContentValues = contentValues;
        mWhereClause = whereClause;
    }

    public void execute(SQLiteDatabase sqLiteDatabase) {
        Assert.assertTrue(sqLiteDatabase != null);

        int updated = sqLiteDatabase.update(mTableName, mContentValues, mWhereClause, null);
        Assert.assertTrue(updated == 1);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mTableName);
        out.writeParcelable(mContentValues, 0);
        out.writeString(mWhereClause);
    }

    public static final Parcelable.Creator<UpdateCommand> CREATOR = new Creator<UpdateCommand>() {
        @Override
        public UpdateCommand createFromParcel(Parcel source) {
            String tableName = source.readString();
            Assert.assertTrue(!TextUtils.isEmpty(tableName));

            ContentValues contentValues = source.readParcelable(ContentValues.class.getClassLoader());
            Assert.assertTrue(contentValues != null);

            String whereClause = source.readString();
            Assert.assertTrue(!TextUtils.isEmpty(whereClause));

            return new UpdateCommand(tableName, contentValues, whereClause);
        }

        @Override
        public UpdateCommand[] newArray(int size) {
            return new UpdateCommand[size];
        }
    };
}
