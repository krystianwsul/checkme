package com.krystianwsul.checkme.persistencemodel;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class SaveService extends IntentService {
    private static final String INSERT_COMMAND_KEY = "insertCommands";
    private static final String UPDATE_COMMAND_KEY = "updateCommands";
    private static final String DELETE_COMMAND_KEY = "deleteCommands";

    public static void startService(@NonNull Context context, @NonNull ArrayList<InsertCommand> insertCommands, @NonNull ArrayList<UpdateCommand> updateCommands, @NonNull ArrayList<DeleteCommand> deleteCommands) {
        Intent intent = new Intent(context, SaveService.class);
        intent.putParcelableArrayListExtra(INSERT_COMMAND_KEY, insertCommands);
        intent.putParcelableArrayListExtra(UPDATE_COMMAND_KEY, updateCommands);
        intent.putParcelableArrayListExtra(DELETE_COMMAND_KEY, deleteCommands);

        context.startService(intent);
    }

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
            DomainFactory.getDomainFactory(this).reset();
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
}
