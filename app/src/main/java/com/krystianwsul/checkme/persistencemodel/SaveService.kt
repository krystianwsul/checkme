package com.krystianwsul.checkme.persistencemodel

import android.util.Log
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlin.properties.Delegates.observable

object SaveService {

    var isSaved by observable(false) { _, oldValue, newValue ->
        Log.e("asdf", "SaveService.isSaved changing from $oldValue to $newValue")
        DomainFactory.nullableInstance?.updateIsSaved()
    }
        private set

    private fun save(insertCommands: List<InsertCommand>, updateCommands: List<UpdateCommand>, deleteCommands: List<DeleteCommand>) {
        Log.e("asdf", "SaveService.save")

        check(isSaved)

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
            isSaved = false
        }
    }

    abstract class Factory {

        companion object {

            var instance: Factory = FactoryImpl()
        }

        abstract fun startService(persistenceManager: PersistenceManager, source: Source): Boolean

        private class FactoryImpl : Factory() {

            override fun startService(persistenceManager: PersistenceManager, source: Source): Boolean {
                MyCrashlytics.logMethod(this, "previous saved: $isSaved")
                check(!isSaved)

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
                    isSaved = true

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
