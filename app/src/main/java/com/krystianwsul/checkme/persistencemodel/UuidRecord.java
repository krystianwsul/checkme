package com.krystianwsul.checkme.persistencemodel;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import junit.framework.Assert;

import java.util.UUID;

class UuidRecord extends Record {
    static final String TABLE_UUID = "uuid";

    private static final String COLUMN_UUID = "uuid";

    @NonNull
    private final String mUuid;

    @NonNull
    static String newUuid() {
        String uuid = UUID.randomUUID().toString();
        Assert.assertTrue(!TextUtils.isEmpty(uuid));

        return uuid;
    }

    public static void onCreate(@NonNull SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + TABLE_UUID
                + " (" + COLUMN_UUID + " TEXT NOT NULL);");

        UuidRecord uuidRecord = new UuidRecord(false, newUuid());
        uuidRecord.getInsertCommand().execute(sqLiteDatabase);
    }

    @NonNull
    static UuidRecord getUuidRecord(@NonNull SQLiteDatabase sqLiteDatabase) {
        Cursor cursor = sqLiteDatabase.query(TABLE_UUID, null, null, null, null, null, null);
        cursor.moveToFirst();

        UuidRecord uuidRecord = cursorToCustomTimeRecord(cursor);

        Assert.assertTrue(cursor.isLast());

        return uuidRecord;
    }

    @NonNull
    private static UuidRecord cursorToCustomTimeRecord(@NonNull Cursor cursor) {
        String uuid = cursor.getString(0);
        Assert.assertTrue(!TextUtils.isEmpty(uuid));

        return new UuidRecord(true, uuid);
    }

    UuidRecord(boolean created, @NonNull String uuid) {
        super(created);

        Assert.assertTrue(!TextUtils.isEmpty(uuid));

        mUuid = uuid;
    }

    @NonNull
    String getUuid() {
        return mUuid;
    }

    @NonNull
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
