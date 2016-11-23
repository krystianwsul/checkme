package com.krystianwsul.checkme.persistencemodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import junit.framework.Assert;

import java.util.UUID;

class UuidRecord extends Record {
    private static final String TABLE_UUID = "uuid";

    private static final String COLUMN_UUID = "uuid";

    private final String mUuid;

    @NonNull
    static String newUuid() {
        String uuid = UUID.randomUUID().toString();
        Assert.assertTrue(!TextUtils.isEmpty(uuid));

        return uuid;
    }

    public static void onCreate(SQLiteDatabase sqLiteDatabase) {
        Assert.assertTrue(sqLiteDatabase != null);

        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_UUID
                + " (" + COLUMN_UUID + " TEXT NOT NULL);");

        UuidRecord uuidRecord = new UuidRecord(false, newUuid());
        uuidRecord.getInsertCommand().execute(sqLiteDatabase);
    }

    @SuppressWarnings("UnusedParameters")
    public static void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Assert.assertTrue(sqLiteDatabase != null);

        if (oldVersion <= 16) {
            onCreate(sqLiteDatabase);
        }
    }

    static UuidRecord getUuidRecord(SQLiteDatabase sqLiteDatabase) {
        Assert.assertTrue(sqLiteDatabase != null);

        Cursor cursor = sqLiteDatabase.query(TABLE_UUID, null, null, null, null, null, null);
        cursor.moveToFirst();

        UuidRecord uuidRecord = cursorToCustomTimeRecord(cursor);

        Assert.assertTrue(cursor.isLast());

        return uuidRecord;
    }

    private static UuidRecord cursorToCustomTimeRecord(Cursor cursor) {
        Assert.assertTrue(cursor != null);

        String uuid = cursor.getString(0);

        return new UuidRecord(true, uuid);
    }

    UuidRecord(boolean created, String uuid) {
        super(created);

        Assert.assertTrue(!TextUtils.isEmpty(uuid));

        mUuid = uuid;
    }

    String getUuid() {
        return mUuid;
    }

    @Override
    ContentValues getContentValues() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_UUID, mUuid);

        return contentValues;
    }

    @NonNull
    @Override
    UpdateCommand getUpdateCommand() {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    InsertCommand getInsertCommand() {
        return getInsertCommand(TABLE_UUID);
    }

    @NonNull
    @Override
    DeleteCommand getDeleteCommand() {
        throw new UnsupportedOperationException();
    }
}
