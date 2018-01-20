package com.krystianwsul.checkme.persistencemodel

import android.content.Context
import android.content.Intent
import android.support.v4.app.JobIntentService
import android.util.Log
import com.krystianwsul.checkme.domainmodel.DomainFactory
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.*

class SaveService : JobIntentService() {

    companion object {

        private val INSERT_COMMAND_KEY = "insertCommands"
        private val UPDATE_COMMAND_KEY = "updateCommands"
        private val DELETE_COMMAND_KEY = "deleteCommands"
        private val SOURCE_KEY = "source"

        private fun save(context: Context, insertCommands: List<InsertCommand>, updateCommands: List<UpdateCommand>, deleteCommands: List<DeleteCommand>, source: Source) {
            Log.e("asdf", "SaveService.save")

            try {
                val sqLiteDatabase = PersistenceManger.getInstance(context).sqLiteDatabase!!

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
            } catch (e: Exception) {
                DomainFactory.getDomainFactory(context).reset(context, source)
                throw e
            }
        }
    }

    override fun onHandleWork(intent: Intent) {
        val insertCommands = intent.getParcelableArrayListExtra<InsertCommand>(INSERT_COMMAND_KEY)!!
        val updateCommands = intent.getParcelableArrayListExtra<UpdateCommand>(UPDATE_COMMAND_KEY)!!
        val deleteCommands = intent.getParcelableArrayListExtra<DeleteCommand>(DELETE_COMMAND_KEY)!!
        val source = intent.getSerializableExtra(SOURCE_KEY) as Source

        save(this, insertCommands, updateCommands, deleteCommands, source)
    }

    abstract class Factory {

        companion object {

            var instance: Factory = FactoryImpl()
        }

        abstract fun startService(context: Context, persistenceManger: PersistenceManger, source: Source)

        private class FactoryImpl : Factory() {

            override fun startService(context: Context, persistenceManger: PersistenceManger, source: Source) {
                val collections = listOf(
                        persistenceManger.customTimeRecords,
                        persistenceManger.taskRecords,
                        persistenceManger.taskHierarchyRecords,
                        persistenceManger.scheduleRecords,
                        persistenceManger.singleScheduleRecords,
                        persistenceManger.dailyScheduleRecords,
                        persistenceManger.weeklyScheduleRecords,
                        persistenceManger.monthlyDayScheduleRecords,
                        persistenceManger.monthlyWeekScheduleRecords,
                        persistenceManger.instanceRecords,
                        persistenceManger.instanceShownRecords)

                val insertCommands = ArrayList(collections.flatten()
                        .filter { it.needsInsert() }
                        .map { it.insertCommand })

                // update

                val updateCommands = ArrayList(collections.flatten()
                        .filter { it.needsUpdate() }
                        .map { it.updateCommand })

                val deleteCommands = ArrayList(collections.map { delete(it) }.flatten())

                when (source) {
                    Source.GUI -> Observable.fromCallable { save(context, insertCommands, updateCommands, deleteCommands, source) }
                            .subscribeOn(Schedulers.io())
                            .subscribe()
                    Source.SERVICE -> save(context, insertCommands, updateCommands, deleteCommands, source)
                } // todo remove service
            } // todo move rx stuff to domainFactory call site

            private fun delete(collection: MutableCollection<out Record>): List<DeleteCommand> {
                val deleted = collection.filter { it.needsDelete() }

                collection.removeAll(deleted)

                return deleted.map { it.deleteCommand }
            }
        }
    }

    enum class Source {
        GUI, SERVICE
    }
}
