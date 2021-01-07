package com.krystianwsul.checkme.persistencemodel

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers

object SaveService {

    private fun save(insertCommands: List<InsertCommand>, updateCommands: List<UpdateCommand>, deleteCommands: List<DeleteCommand>) {
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

        abstract fun startService(persistenceManager: PersistenceManager, source: Source): Boolean

        private class FactoryImpl : Factory() {

            override fun startService(persistenceManager: PersistenceManager, source: Source): Boolean {
                val collections = persistenceManager.instanceShownRecords

                val insertCommands = collections.asSequence()
                        .filter { it.needsInsert() }
                        .map { it.insertCommand }
                        .toList()

                // update

                val updateCommands = collections.asSequence()
                        .filter { it.needsUpdate() }
                        .map { it.updateCommand }
                        .toList()

                val deleteCommands = delete(collections).toMutableList()

                val hasChanges = insertCommands.any() || updateCommands.any() || deleteCommands.any()

                if (hasChanges) {
                    when (source) {
                        Source.GUI -> Observable.fromCallable { save(insertCommands, updateCommands, deleteCommands) }
                                .subscribeOn(Schedulers.io())
                                .subscribe()
                        Source.SERVICE -> save(insertCommands, updateCommands, deleteCommands)
                    }
                }

                return hasChanges
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
