package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.utils.mapNotNull
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.Observables

fun <T : Any, U : Any> mergePaperAndRx(
    paperMaybe: Maybe<T>,
    firebaseObservable: Observable<U>,
    converter: Converter<T, U>,
): Observable<U> {
    return Observables.combineLatest(
        paperMaybe.toObservable()
            .map<AndroidDatabaseWrapper.LoadState<T>> { AndroidDatabaseWrapper.LoadState.Loaded(it) }
            .startWithItem(AndroidDatabaseWrapper.LoadState.Initial()),
        firebaseObservable.map<AndroidDatabaseWrapper.LoadState<U>> { AndroidDatabaseWrapper.LoadState.Loaded(it) }
            .take(1)
            .startWithItem(AndroidDatabaseWrapper.LoadState.Initial()),
    )
        .scan<PairState<T, U>>(PairState.SkippingFirst(converter)) { oldPairState, (newPaperState, newFirebaseState) ->
            oldPairState.processNextPair(newPaperState, newFirebaseState)
        }
        .mapNotNull { it.emission }
        .mergeWith(firebaseObservable.skip(1))
}

open class Converter<T : Any, U : Any>(
    private val paperToSnapshot: (T) -> U,
    val snapshotToPaper: (U) -> T,
    val deepCopyPaper: (T) -> T,
) {

    fun copyPaperToSnapshot(value: T) = paperToSnapshot(deepCopyPaper(value))
}

private sealed class PairState<T : Any, U : Any> {

    abstract val emission: U?

    abstract fun processNextPair(
        newPaperState: AndroidDatabaseWrapper.LoadState<T>,
        newFirebaseState: AndroidDatabaseWrapper.LoadState<U>,
    ): PairState<T, U>

    class SkippingFirst<T : Any, U : Any>(private val converter: Converter<T, U>) : PairState<T, U>() {

        override val emission: U? = null

        override fun processNextPair(
            newPaperState: AndroidDatabaseWrapper.LoadState<T>,
            newFirebaseState: AndroidDatabaseWrapper.LoadState<U>,
        ): PairState<T, U> {
            return when {
                newPaperState is AndroidDatabaseWrapper.LoadState.Initial &&
                        newFirebaseState is AndroidDatabaseWrapper.LoadState.Initial -> SkippingFirst(converter)
                newPaperState is AndroidDatabaseWrapper.LoadState.Initial -> {
                    check(newFirebaseState is AndroidDatabaseWrapper.LoadState.Loaded)

                    FirebaseCameFirst(newFirebaseState.value)
                }
                else -> {
                    check(newPaperState is AndroidDatabaseWrapper.LoadState.Loaded)
                    check(newFirebaseState is AndroidDatabaseWrapper.LoadState.Initial)

                    PaperCameFirst(newPaperState.value, converter)
                }
            }
        }
    }

    class FirebaseCameFirst<T : Any, U : Any>(private val firebase: U) : PairState<T, U>() {

        override val emission = firebase

        override fun processNextPair(
            newPaperState: AndroidDatabaseWrapper.LoadState<T>,
            newFirebaseState: AndroidDatabaseWrapper.LoadState<U>,
        ): PairState<T, U> {
            check(newPaperState is AndroidDatabaseWrapper.LoadState.Loaded)
            check(newFirebaseState is AndroidDatabaseWrapper.LoadState.Loaded)

            check(newFirebaseState.value == firebase)

            return Terminal(null)
        }
    }

    class PaperCameFirst<T : Any, U : Any>(private val paper: T, private val converter: Converter<T, U>) :
        PairState<T, U>() {

        override val emission = converter.copyPaperToSnapshot(paper)

        override fun processNextPair(
            newPaperState: AndroidDatabaseWrapper.LoadState<T>,
            newFirebaseState: AndroidDatabaseWrapper.LoadState<U>,
        ): PairState<T, U> {
            check(newPaperState is AndroidDatabaseWrapper.LoadState.Loaded)
            check(newFirebaseState is AndroidDatabaseWrapper.LoadState.Loaded)

            val firebase = converter.snapshotToPaper(newFirebaseState.value)

            /*
            todo notification: here's the race condition: this paper variable then holds RootTaskJson, which is mutable.
            If the notification action is performed before firebase comes in, then this data class gets updated in-place.
            So, the equality check fails afterwards, and it gets overwritten with the firebase value.

            Find a place to COPY this data class, so that we keep the original here
             */

            return if (firebase == paper) {
                Terminal(null)
            } else {
                Terminal(newFirebaseState.value)
            }
        }
    }

    class Terminal<T : Any, U : Any>(override val emission: U?) : PairState<T, U>() {

        override fun processNextPair(
            newPaperState: AndroidDatabaseWrapper.LoadState<T>,
            newFirebaseState: AndroidDatabaseWrapper.LoadState<U>,
        ): PairState<T, U> = throw IllegalStateException()
    }
}