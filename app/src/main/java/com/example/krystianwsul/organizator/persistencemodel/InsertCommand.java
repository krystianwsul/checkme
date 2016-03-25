package com.example.krystianwsul.organizator.persistencemodel;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import junit.framework.Assert;

class InsertCommand implements Parcelable {
    private final String mTableName;
    private final ContentValues mContentValues;

    public InsertCommand(String tableName, ContentValues contentValues) {
        Assert.assertTrue(!TextUtils.isEmpty(tableName));
        Assert.assertTrue(contentValues != null);
        Assert.assertTrue(contentValues.size() > 0);

        mTableName = tableName;
        mContentValues = contentValues;
    }

    public void execute(SQLiteDatabase sqLiteDatabase) {
        Assert.assertTrue(sqLiteDatabase != null);

        long id = sqLiteDatabase.insert(mTableName, null, mContentValues);
        Assert.assertTrue(id != -1);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mTableName);
        out.writeParcelable(mContentValues, 0);
    }

    public static final Parcelable.Creator<InsertCommand> CREATOR = new Creator<InsertCommand>() {
        @Override
        public InsertCommand createFromParcel(Parcel source) {
            String tableName = source.readString();
            Assert.assertTrue(!TextUtils.isEmpty(tableName));

            ContentValues contentValues = source.readParcelable(ContentValues.class.getClassLoader());
            Assert.assertTrue(contentValues != null);

            return new InsertCommand(tableName, contentValues);
        }

        @Override
        public InsertCommand[] newArray(int size) {
            return new InsertCommand[size];
        }
    };
}
