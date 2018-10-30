package com.krystianwsul.checkme.persistencemodel

import android.util.Log
import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

object SaveService {

    private fun save(insertCommands: List<InsertCommand>, updateCommands: List<UpdateCommand>, deleteCommands: List<DeleteCommand>, source: Source) {
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
            KotlinDomainFactory.getKotlinDomainFactory().domainFactory.reset(source)
            throw e
        }
    }

    abstract class Factory {

        companion object {

            var instance: Factory = FactoryImpl()
        }

        abstract fun startService(persistenceManger: PersistenceManger, source: Source)

        private class FactoryImpl : Factory() {

            override fun startService(persistenceManger: PersistenceManger, source: Source) {
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
                    Source.GUI -> Observable.fromCallable { save(insertCommands, updateCommands, deleteCommands, source) }
                            .subscribeOn(Schedulers.io())
                            .subscribe()
                    Source.SERVICE -> save(insertCommands, updateCommands, deleteCommands, source)
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
