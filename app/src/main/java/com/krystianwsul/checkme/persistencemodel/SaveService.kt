package com.krystianwsul.checkme.persistencemodel

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import com.commonsware.cwac.wakeful.WakefulIntentService
import com.krystianwsul.checkme.domainmodel.DomainFactory
import java.util.*

class SaveService : WakefulIntentService("SaveService") {

    companion object {

        private val INSERT_COMMAND_KEY = "insertCommands"
        private val UPDATE_COMMAND_KEY = "updateCommands"
        private val DELETE_COMMAND_KEY = "deleteCommands"

        private fun save(sqLiteDatabase: SQLiteDatabase, insertCommands: List<InsertCommand>, updateCommands: List<UpdateCommand>, deleteCommands: List<DeleteCommand>) {
            sqLiteDatabase.beginTransaction()

            try {
                for (insertCommand in insertCommands)
                    insertCommand.execute(sqLiteDatabase)

                for (updateCommand in updateCommands)
                    updateCommand.execute(sqLiteDatabase)

                for (deleteCommand in deleteCommands)
                    deleteCommand.execute(sqLiteDatabase)

                sqLiteDatabase.setTransactionSuccessful()
            } finally {
                sqLiteDatabase.endTransaction()
            }
        }
    }

    override fun doWakefulWork(intent: Intent) {
        val insertCommands = intent.getParcelableArrayListExtra<InsertCommand>(INSERT_COMMAND_KEY)!!
        val updateCommands = intent.getParcelableArrayListExtra<UpdateCommand>(UPDATE_COMMAND_KEY)!!
        val deleteCommands = intent.getParcelableArrayListExtra<DeleteCommand>(DELETE_COMMAND_KEY)!!

        val sqLiteDatabase = PersistenceManger.getInstance(this).sqLiteDatabase!!

        try {
            save(sqLiteDatabase, insertCommands, updateCommands, deleteCommands)
        } catch (e: Exception) {
            DomainFactory.getDomainFactory(this).reset(this)
            throw e
        }

    }

    abstract class Factory {

        companion object {

            var instance: Factory = FactoryImpl()
        }

        abstract fun startService(context: Context, persistenceManger: PersistenceManger)

        private class FactoryImpl : Factory() {

            override fun startService(context: Context, persistenceManger: PersistenceManger) {
                val collections = Arrays.asList(
                        persistenceManger.mCustomTimeRecords,
                        persistenceManger.mTaskRecords,
                        persistenceManger.mTaskHierarchyRecords,
                        persistenceManger.mScheduleRecords,
                        persistenceManger.mScheduleRecords,
                        persistenceManger.mSingleScheduleRecords.values,
                        persistenceManger.mDailyScheduleRecords.values,
                        persistenceManger.mWeeklyScheduleRecords.values,
                        persistenceManger.mMonthlyDayScheduleRecords.values,
                        persistenceManger.mMonthlyWeekScheduleRecords.values,
                        persistenceManger.mInstanceRecords,
                        persistenceManger.mInstanceShownRecords)

                val insertCommands = ArrayList(collections.flatten()
                        .filter { it.needsInsert() }
                        .map { it.insertCommand })

                // update

                val updateCommands = ArrayList(collections.flatten()
                        .filter { it.needsUpdate() }
                        .map { it.updateCommand })

                val deleteCommands = ArrayList(collections.map { delete(it) }.flatten())

                val intent = Intent(context, SaveService::class.java)
                intent.putParcelableArrayListExtra(INSERT_COMMAND_KEY, insertCommands)
                intent.putParcelableArrayListExtra(UPDATE_COMMAND_KEY, updateCommands)
                intent.putParcelableArrayListExtra(DELETE_COMMAND_KEY, deleteCommands)

                WakefulIntentService.sendWakefulWork(context, intent)
            }

            private fun delete(collection: MutableCollection<out Record>): List<DeleteCommand> {
                val deleted = collection.filter { it.needsDelete() }

                collection.removeAll(deleted)

                return deleted.map { it.deleteCommand }
            }
        }
    }
}
