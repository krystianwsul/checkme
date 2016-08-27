package com.krystianwsul.checkme.persistencemodel;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class SaveService extends IntentService {
    private static final String INSERT_COMMAND_KEY = "insertCommands";
    private static final String UPDATE_COMMAND_KEY = "updateCommands";

    public static void startService(Context context, ArrayList<InsertCommand> insertCommands, ArrayList<UpdateCommand> updateCommands) {
        Intent intent = new Intent(context, SaveService.class);
        intent.putParcelableArrayListExtra(INSERT_COMMAND_KEY, insertCommands);
        intent.putParcelableArrayListExtra(UPDATE_COMMAND_KEY, updateCommands);

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

        SQLiteDatabase sqLiteDatabase = PersistenceManger.getInstance(this).getSQLiteDatabase();
        Assert.assertTrue(sqLiteDatabase != null);

        try {
            save(sqLiteDatabase, insertCommands, updateCommands);
        } catch (Exception e) {
            DomainFactory.getDomainFactory(this).reset();
            throw e;
        }
    }

    static void save(SQLiteDatabase sqLiteDatabase, List<InsertCommand> insertCommands, List<UpdateCommand> updateCommands) {
        Assert.assertTrue(sqLiteDatabase != null);
        Assert.assertTrue(insertCommands != null);
        Assert.assertTrue(updateCommands != null);

        sqLiteDatabase.beginTransaction();

        try {
            for (InsertCommand insertCommand : insertCommands)
                insertCommand.execute(sqLiteDatabase);

            for (UpdateCommand updateCommand : updateCommands)
                updateCommand.execute(sqLiteDatabase);

            sqLiteDatabase.setTransactionSuccessful();
        } finally {
            sqLiteDatabase.endTransaction();
        }
    }
}
