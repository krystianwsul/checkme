package com.krystianwsul.checkme.persistencemodel

import android.content.Context
import android.util.Log
import com.krystianwsul.checkme.domainmodel.DomainFactory
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

object SaveService {

    private fun save(context: Context, insertCommands: List<InsertCommand>, updateCommands: List<UpdateCommand>, deleteCommands: List<DeleteCommand>, source: Source) {
        Log.e("asdf", "SaveService.save")

        try {
            val sqLiteDatabase = MySQLiteHelper.database

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
            DomainFactory.getDomainFactory().reset(context, source)
            throw e
        }
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

                val insertCommands = collections.flatten()
                        .filter { it.needsInsert() }
                        .map { it.insertCommand }

                // update

                val updateCommands = collections.flatten()
                        .filter { it.needsUpdate() }
                        .map { it.updateCommand }

                val deleteCommands = collections.map { delete(it) }
                        .flatten()
                        .toMutableList()

                when (source) {
                    Source.GUI -> Observable.fromCallable { save(context, insertCommands, updateCommands, deleteCommands, source) }
                            .subscribeOn(Schedulers.io())
                            .subscribe()
                    Source.SERVICE -> save(context, insertCommands, updateCommands, deleteCommands, source)
                }
            }

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
