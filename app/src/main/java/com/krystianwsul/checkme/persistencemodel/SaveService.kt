package com.krystianwsul.checkme.persistencemodel

import android.util.Log
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

object SaveService {

    private fun save(insertCommands: List<InsertCommand>, updateCommands: List<UpdateCommand>, deleteCommands: List<DeleteCommand>) {
        Log.e("asdf", "SaveService.save")

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
    }

    abstract class Factory {

        companion object {

            var instance: Factory = FactoryImpl()
        }

        abstract fun startService(persistenceManager: PersistenceManager, source: Source)

        private class FactoryImpl : Factory() {

            override fun startService(persistenceManager: PersistenceManager, source: Source) {
                val collections = listOf(
                        persistenceManager.customTimeRecords,
                        persistenceManager.taskRecords,
                        persistenceManager.taskHierarchyRecords,
                        persistenceManager.scheduleRecords,
                        persistenceManager.singleScheduleRecords,
                        persistenceManager.dailyScheduleRecords,
                        persistenceManager.weeklyScheduleRecords,
                        persistenceManager.monthlyDayScheduleRecords,
                        persistenceManager.monthlyWeekScheduleRecords,
                        persistenceManager.localInstanceRecords,
                        persistenceManager.instanceShownRecords)

                val insertCommands = collections.flatten()
                        .asSequence()
                        .filter { it.needsInsert() }
                        .map { it.insertCommand }
                        .toList()

                // update

                val updateCommands = collections.flatten()
                        .asSequence()
                        .filter { it.needsUpdate() }
                        .map { it.updateCommand }
                        .toList()

                val deleteCommands = collections.map { delete(it) }
                        .flatten()
                        .toMutableList()

                when (source) {
                    Source.GUI -> Observable.fromCallable { save(insertCommands, updateCommands, deleteCommands) }
                            .subscribeOn(Schedulers.io())
                            .subscribe()
                    Source.SERVICE -> save(insertCommands, updateCommands, deleteCommands)
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
