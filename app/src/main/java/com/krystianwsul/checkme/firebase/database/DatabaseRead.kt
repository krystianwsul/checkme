package com.krystianwsul.checkme.firebase.database

import com.androidhuman.rxfirebase2.database.dataChanges
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.core.Path
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.firebase.Converter
import com.krystianwsul.checkme.firebase.mergePaperAndRx
import com.krystianwsul.checkme.firebase.snapshot.Snapshot
import com.krystianwsul.checkme.utils.toV3
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers

abstract class DatabaseRead<DATA : Any> {

    open fun getPriority(taskPriorityMapper: TaskPriorityMapper) = DatabaseReadPriority.NORMAL

    protected open fun pathToPaperKey(path: Path) = path.toString().replace('/', '-')

    protected abstract fun DatabaseReference.getQuery(): Query

    protected abstract fun Query.toSnapshot(): Observable<Snapshot<DATA>>

    fun getResult(): Observable<Snapshot<DATA>> = AndroidDatabaseWrapper.rootReference
        .getQuery()
        .toSnapshot()

    abstract val description: String

    private fun writeNullable(path: Path, value: DATA?): Completable {
        return if (AndroidDatabaseWrapper.ENABLE_PAPER) {
            AndroidDatabaseWrapper.rxPaperBook.write(pathToPaperKey(path), NullableWrapper(value))
                .toV3()
                .subscribeOn(Schedulers.io())
        } else {
            Completable.complete()
        }
    }

    private fun readNullable(path: Path): Maybe<NullableWrapper<DATA>> {
        return if (AndroidDatabaseWrapper.ENABLE_PAPER) {
            AndroidDatabaseWrapper.rxPaperBook.read<NullableWrapper<DATA>>(pathToPaperKey(path))
                .toV3()
                .subscribeOn(Schedulers.io())
                .toMaybe()
                .onErrorComplete()
        } else {
            Maybe.empty()
        }
    }

    protected abstract fun firebaseToSnapshot(dataSnapshot: DataSnapshot): Snapshot<DATA>

    protected fun Query.cache(converter: Converter<NullableWrapper<DATA>, Snapshot<DATA>>): Observable<Snapshot<DATA>> {
        val firebaseObservable = dataChanges().toV3()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.computation())
            .map { firebaseToSnapshot(it) }
            .doOnNext { writeNullable(path, it.value).subscribe() }

        return mergePaperAndRx(readNullable(path), firebaseObservable, converter).flatMapSingle {
            DatabaseResultQueue.enqueueSnapshot(this@DatabaseRead, it)
        }
    }
}