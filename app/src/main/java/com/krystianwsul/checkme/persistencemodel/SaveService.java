package com.krystianwsul.checkme.persistencemodel;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SaveService extends IntentService {
    private static final String INSERT_COMMAND_KEY = "insertCommands";
    private static final String UPDATE_COMMAND_KEY = "updateCommands";
    private static final String DELETE_COMMAND_KEY = "deleteCommands";

    public SaveService() {
        super("SaveService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ArrayList<InsertCommand> insertCommands = intent.getParcelableArrayListExtra(INSERT_COMMAND_KEY);
        Assert.assertTrue(insertCommands != null);

        ArrayList<UpdateCommand> updateCommands = intent.getParcelableArrayListExtra(UPDATE_COMMAND_KEY);
        Assert.assertTrue(updateCommands != null);

        ArrayList<DeleteCommand> deleteCommands = intent.getParcelableArrayListExtra(DELETE_COMMAND_KEY);
        Assert.assertTrue(deleteCommands != null);

        SQLiteDatabase sqLiteDatabase = PersistenceManger.getInstance(this).getSQLiteDatabase();
        Assert.assertTrue(sqLiteDatabase != null);

        try {
            save(sqLiteDatabase, insertCommands, updateCommands, deleteCommands);
        } catch (Exception e) {
            DomainFactory.getDomainFactory(this).reset(this);
            throw e;
        }
    }

    static void save(@NonNull SQLiteDatabase sqLiteDatabase, @NonNull List<InsertCommand> insertCommands, @NonNull List<UpdateCommand> updateCommands, @NonNull List<DeleteCommand> deleteCommands) {
        sqLiteDatabase.beginTransaction();

        try {
            for (InsertCommand insertCommand : insertCommands)
                insertCommand.execute(sqLiteDatabase);

            for (UpdateCommand updateCommand : updateCommands)
                updateCommand.execute(sqLiteDatabase);

            for (DeleteCommand deleteCommand : deleteCommands)
                deleteCommand.execute(sqLiteDatabase);

            sqLiteDatabase.setTransactionSuccessful();
        } finally {
            sqLiteDatabase.endTransaction();
        }
    }

    public static abstract class Factory {
        private static Factory sInstance;

        public static Factory getInstance() {
            if (sInstance == null)
                sInstance = new FactoryImpl();
            return sInstance;
        }

        public static void setInstance(@NonNull Factory factory) {
            Assert.assertTrue(sInstance == null);

            sInstance = factory;
        }

        public abstract void startService(@NonNull Context context, @NonNull PersistenceManger persistenceManger);

        private static class FactoryImpl extends Factory {
            @Override
            public void startService(@NonNull Context context, @NonNull PersistenceManger persistenceManger) {
                ArrayList<InsertCommand> insertCommands = new ArrayList<>();

                for (CustomTimeRecord customTimeRecord : persistenceManger.mCustomTimeRecords)
                    if (customTimeRecord.needsInsert())
                        insertCommands.add(customTimeRecord.getInsertCommand());

                for (TaskRecord taskRecord : persistenceManger.mTaskRecords)
                    if (taskRecord.needsInsert())
                        insertCommands.add(taskRecord.getInsertCommand());

                for (TaskHierarchyRecord taskHierarchyRecord : persistenceManger.mTaskHierarchyRecords)
                    if (taskHierarchyRecord.needsInsert())
                        insertCommands.add(taskHierarchyRecord.getInsertCommand());

                for (ScheduleRecord scheduleRecord : persistenceManger.mScheduleRecords)
                    if (scheduleRecord.needsInsert())
                        insertCommands.add(scheduleRecord.getInsertCommand());

                for (SingleScheduleRecord singleScheduleRecord : persistenceManger.mSingleScheduleRecords.values())
                    if (singleScheduleRecord.needsInsert())
                        insertCommands.add(singleScheduleRecord.getInsertCommand());

                for (DailyScheduleRecord dailyScheduleRecord : persistenceManger.mDailyScheduleRecords.values())
                    if (dailyScheduleRecord.needsInsert())
                        insertCommands.add(dailyScheduleRecord.getInsertCommand());

                for (WeeklyScheduleRecord weeklyScheduleRecord : persistenceManger.mWeeklyScheduleRecords.values())
                    if (weeklyScheduleRecord.needsInsert())
                        insertCommands.add(weeklyScheduleRecord.getInsertCommand());

                for (MonthlyDayScheduleRecord monthlyDayScheduleRecord : persistenceManger.mMonthlyDayScheduleRecords.values())
                    if (monthlyDayScheduleRecord.needsInsert())
                        insertCommands.add(monthlyDayScheduleRecord.getInsertCommand());

                for (MonthlyWeekScheduleRecord monthlyWeekScheduleRecord : persistenceManger.mMonthlyWeekScheduleRecords.values())
                    if (monthlyWeekScheduleRecord.needsInsert())
                        insertCommands.add(monthlyWeekScheduleRecord.getInsertCommand());

                for (InstanceRecord instanceRecord : persistenceManger.mInstanceRecords)
                    if (instanceRecord.needsInsert())
                        insertCommands.add(instanceRecord.getInsertCommand());

                for (InstanceShownRecord instanceShownRecord : persistenceManger.mInstanceShownRecords)
                    if (instanceShownRecord.needsInsert())
                        insertCommands.add(instanceShownRecord.getInsertCommand());

                // update

                ArrayList<UpdateCommand> updateCommands = new ArrayList<>();

                for (CustomTimeRecord customTimeRecord : persistenceManger.mCustomTimeRecords)
                    if (customTimeRecord.needsUpdate())
                        updateCommands.add(customTimeRecord.getUpdateCommand());

                for (TaskRecord taskRecord : persistenceManger.mTaskRecords)
                    if (taskRecord.needsUpdate())
                        updateCommands.add(taskRecord.getUpdateCommand());

                for (TaskHierarchyRecord taskHierarchyRecord : persistenceManger.mTaskHierarchyRecords)
                    if (taskHierarchyRecord.needsUpdate())
                        updateCommands.add(taskHierarchyRecord.getUpdateCommand());

                for (ScheduleRecord scheduleRecord : persistenceManger.mScheduleRecords)
                    if (scheduleRecord.needsUpdate())
                        updateCommands.add(scheduleRecord.getUpdateCommand());

                for (SingleScheduleRecord singleScheduleRecord : persistenceManger.mSingleScheduleRecords.values())
                    if (singleScheduleRecord.needsUpdate())
                        updateCommands.add(singleScheduleRecord.getUpdateCommand());

                for (DailyScheduleRecord dailyScheduleRecord : persistenceManger.mDailyScheduleRecords.values())
                    if (dailyScheduleRecord.needsUpdate())
                        updateCommands.add(dailyScheduleRecord.getUpdateCommand());

                for (WeeklyScheduleRecord weeklyScheduleRecord : persistenceManger.mWeeklyScheduleRecords.values())
                    if (weeklyScheduleRecord.needsUpdate())
                        updateCommands.add(weeklyScheduleRecord.getUpdateCommand());

                for (MonthlyDayScheduleRecord monthlyDayScheduleRecord : persistenceManger.mMonthlyDayScheduleRecords.values())
                    if (monthlyDayScheduleRecord.needsUpdate())
                        updateCommands.add(monthlyDayScheduleRecord.getUpdateCommand());

                for (MonthlyWeekScheduleRecord monthlyWeekScheduleRecord : persistenceManger.mMonthlyWeekScheduleRecords.values())
                    if (monthlyWeekScheduleRecord.needsUpdate())
                        updateCommands.add(monthlyWeekScheduleRecord.getUpdateCommand());

                for (InstanceRecord instanceRecord : persistenceManger.mInstanceRecords)
                    if (instanceRecord.needsUpdate())
                        updateCommands.add(instanceRecord.getUpdateCommand());

                for (InstanceShownRecord instanceShownRecord : persistenceManger.mInstanceShownRecords)
                    if (instanceShownRecord.needsUpdate())
                        updateCommands.add(instanceShownRecord.getUpdateCommand());

                ArrayList<DeleteCommand> deleteCommands = new ArrayList<>();

                deleteCommands.addAll(delete(persistenceManger.mCustomTimeRecords));
                deleteCommands.addAll(delete(persistenceManger.mTaskRecords));
                deleteCommands.addAll(delete(persistenceManger.mTaskHierarchyRecords));
                deleteCommands.addAll(delete(persistenceManger.mScheduleRecords));
                deleteCommands.addAll(delete(persistenceManger.mSingleScheduleRecords));
                deleteCommands.addAll(delete(persistenceManger.mDailyScheduleRecords));
                deleteCommands.addAll(delete(persistenceManger.mWeeklyScheduleRecords));
                deleteCommands.addAll(delete(persistenceManger.mMonthlyDayScheduleRecords));
                deleteCommands.addAll(delete(persistenceManger.mMonthlyWeekScheduleRecords));
                deleteCommands.addAll(delete(persistenceManger.mInstanceRecords));
                deleteCommands.addAll(delete(persistenceManger.mInstanceShownRecords));

                Intent intent = new Intent(context, SaveService.class);
                intent.putParcelableArrayListExtra(INSERT_COMMAND_KEY, insertCommands);
                intent.putParcelableArrayListExtra(UPDATE_COMMAND_KEY, updateCommands);
                intent.putParcelableArrayListExtra(DELETE_COMMAND_KEY, deleteCommands);

                context.startService(intent);
            }

            @NonNull
            private static List<DeleteCommand> delete(@NonNull Collection<? extends Record> collection) {
                List<Record> deleted = Stream.of(collection)
                        .filter(Record::needsDelete)
                        .collect(Collectors.toList());

                //noinspection SuspiciousMethodCalls
                collection.removeAll(deleted);

                return Stream.of(deleted)
                        .map(Record::getDeleteCommand)
                        .collect(Collectors.toList());
            }

            @NonNull
            private static List<DeleteCommand> delete(@NonNull Map<Integer, ? extends Record> map) {
                Map<Integer, Record> deleted = Stream.of(map)
                        .filter(pair -> pair.getValue().needsDelete())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                Stream.of(deleted.keySet())
                        .forEach(map::remove);

                return Stream.of(deleted.values())
                        .map(Record::getDeleteCommand)
                        .collect(Collectors.toList());
            }
        }
    }
}
