package com.example.krystianwsul.organizator.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.krystianwsul.organizator.persistencemodel.CustomTimeRecord;
import com.example.krystianwsul.organizator.persistencemodel.DailyScheduleTimeRecord;
import com.example.krystianwsul.organizator.persistencemodel.InstanceRecord;
import com.example.krystianwsul.organizator.persistencemodel.ScheduleRecord;
import com.example.krystianwsul.organizator.persistencemodel.SingleScheduleDateTimeRecord;
import com.example.krystianwsul.organizator.persistencemodel.TaskHierarchyRecord;
import com.example.krystianwsul.organizator.persistencemodel.TaskRecord;
import com.example.krystianwsul.organizator.persistencemodel.WeeklyScheduleDayOfWeekTimeRecord;

public class MySQLiteHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "tasks.db";
    public static final int DATABASE_VERSION = 1;

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        CustomTimeRecord.onCreate(sqLiteDatabase);
        DailyScheduleTimeRecord.onCreate(sqLiteDatabase);
        InstanceRecord.onCreate(sqLiteDatabase);
        ScheduleRecord.onCreate(sqLiteDatabase);
        SingleScheduleDateTimeRecord.onCreate(sqLiteDatabase);
        TaskHierarchyRecord.onCreate(sqLiteDatabase);
        TaskRecord.onCreate(sqLiteDatabase);
        WeeklyScheduleDayOfWeekTimeRecord.onCreate(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        CustomTimeRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
        DailyScheduleTimeRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
        InstanceRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
        ScheduleRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
        SingleScheduleDateTimeRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
        TaskHierarchyRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
        TaskRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
        WeeklyScheduleDayOfWeekTimeRecord.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
    }
}
