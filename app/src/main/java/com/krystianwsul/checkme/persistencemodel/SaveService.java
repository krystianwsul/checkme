package com.krystianwsul.checkme.persistencemodel;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.krystianwsul.checkme.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SaveService extends WakefulIntentService {
    private static final String INSERT_COMMAND_KEY = "insertCommands";
    private static final String UPDATE_COMMAND_KEY = "updateCommands";
    private static final String DELETE_COMMAND_KEY = "deleteCommands";

    public SaveService() {
        super("SaveService");
    }

    @Override
    protected void doWakefulWork(Intent intent) {
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

    private static void save(@NonNull SQLiteDatabase sqLiteDatabase, @NonNull List<InsertCommand> insertCommands, @NonNull List<UpdateCommand> updateCommands, @NonNull List<DeleteCommand> deleteCommands) {
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
                List<Collection<? extends Record>> collections = Arrays.asList(
                        persistenceManger.mCustomTimeRecords,
                        persistenceManger.mTaskRecords,
                        persistenceManger.mTaskHierarchyRecords,
                        persistenceManger.mScheduleRecords,
                        persistenceManger.mScheduleRecords,
                        persistenceManger.mSingleScheduleRecords.values(),
                        persistenceManger.mDailyScheduleRecords.values(),
                        persistenceManger.mWeeklyScheduleRecords.values(),
                        persistenceManger.mMonthlyDayScheduleRecords.values(),
                        persistenceManger.mMonthlyWeekScheduleRecords.values(),
                        persistenceManger.mInstanceRecords,
                        persistenceManger.mInstanceShownRecords);

                ArrayList<InsertCommand> insertCommands = Stream.of(collections)
                        .flatMap(Stream::of)
                        .filter(Record::needsInsert)
                        .map(Record::getInsertCommand)
                        .collect(Collectors.toCollection(ArrayList::new));

                // update

                ArrayList<UpdateCommand> updateCommands = Stream.of(collections)
                        .flatMap(Stream::of)
                        .filter(Record::needsUpdate)
                        .map(Record::getUpdateCommand)
                        .collect(Collectors.toCollection(ArrayList::new));

                ArrayList<DeleteCommand> deleteCommands = Stream.of(collections)
                        .map(FactoryImpl::delete)
                        .flatMap(Stream::of)
                        .collect(Collectors.toCollection(ArrayList::new));

                Intent intent = new Intent(context, SaveService.class);
                intent.putParcelableArrayListExtra(INSERT_COMMAND_KEY, insertCommands);
                intent.putParcelableArrayListExtra(UPDATE_COMMAND_KEY, updateCommands);
                intent.putParcelableArrayListExtra(DELETE_COMMAND_KEY, deleteCommands);

                WakefulIntentService.sendWakefulWork(context, intent);
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
