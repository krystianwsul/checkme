package com.example.krystianwsul.organizator.persistencemodel;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;

import junit.framework.Assert;

import java.util.ArrayList;

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
        try
        {
            ArrayList<InsertCommand> insertCommands = intent.getParcelableArrayListExtra(INSERT_COMMAND_KEY);
            Assert.assertTrue(insertCommands != null);

            ArrayList<UpdateCommand> updateCommands = intent.getParcelableArrayListExtra(UPDATE_COMMAND_KEY);
            Assert.assertTrue(updateCommands != null);

            SQLiteDatabase sqLiteDatabase = PersistenceManger.getInstance(this).getSQLiteDatabase();

            sqLiteDatabase.beginTransaction();

            try
            {
                for (InsertCommand insertCommand : insertCommands)
                    insertCommand.execute(sqLiteDatabase);

                for (UpdateCommand updateCommand : updateCommands)
                    updateCommand.execute(sqLiteDatabase);

                sqLiteDatabase.setTransactionSuccessful();
            } finally {
                sqLiteDatabase.endTransaction();
            }
        } catch (Exception e)
        {
            Log.e("SaveService", "save error", e);
            DomainFactory.getDomainFactory(this).reset();
        }
    }
}
