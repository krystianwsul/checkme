package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.utils.mapNotNull
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.Observables

fun <T : Any, U : Any> mergePaperAndRx(
        paperMaybe: Maybe<T>,
        firebaseObservable: Observable<U>,
        paperToSnapshot: (T) -> U,
): Observable<U> {
    val permutationObservable = Observables.combineLatest(
            paperMaybe.toObservable()
                    .map<AndroidDatabaseWrapper.LoadState<T>> { AndroidDatabaseWrapper.LoadState.Loaded(it) }
                    .startWithItem(AndroidDatabaseWrapper.LoadState.Initial()),
            firebaseObservable.take(1)
                    .map<AndroidDatabaseWrapper.LoadState<U>> { AndroidDatabaseWrapper.LoadState.Loaded(it) }
                    .startWithItem(AndroidDatabaseWrapper.LoadState.Initial()),
    ).scan<PairState<T, U>>(PairState.Initial(paperToSnapshot)) { oldPairState, (newPaperState, newFirebaseState) ->
        oldPairState.processNextPair(newPaperState, newFirebaseState)
    }

    return permutationObservable.mapNotNull { it.emission }.mergeWith(firebaseObservable.skip(1))
}

private sealed class PairState<T : Any, U : Any> {

    abstract val emission: U?

    abstract fun processNextPair(
            newPaperState: AndroidDatabaseWrapper.LoadState<T>,
            newFirebaseState: AndroidDatabaseWrapper.LoadState<U>,
    ): PairState<T, U>

    class Initial<T : Any, U : Any>(private val paperToSnapshot: (T) -> U) : PairState<T, U>() {

        override val emission: U? get() = null

        override fun processNextPair(
                newPaperState: AndroidDatabaseWrapper.LoadState<T>,
                newFirebaseState: AndroidDatabaseWrapper.LoadState<U>,
        ): PairState<T, U> {
            check(newPaperState is AndroidDatabaseWrapper.LoadState.Initial)
            check(newFirebaseState is AndroidDatabaseWrapper.LoadState.Initial)

            return SkippingFirst(paperToSnapshot)
        }
    }

    class SkippingFirst<T : Any, U : Any>(private val paperToSnapshot: (T) -> U) : PairState<T, U>() {

        override val emission: U? = null

        override fun processNextPair(
                newPaperState: AndroidDatabaseWrapper.LoadState<T>,
                newFirebaseState: AndroidDatabaseWrapper.LoadState<U>,
        ): PairState<T, U> {
            return if (newPaperState is AndroidDatabaseWrapper.LoadState.Initial) {
                check(newFirebaseState is AndroidDatabaseWrapper.LoadState.Loaded)

                FirebaseCameFirst(newFirebaseState.value)
            } else {
                check(newPaperState is AndroidDatabaseWrapper.LoadState.Loaded)
                check(newFirebaseState is AndroidDatabaseWrapper.LoadState.Initial)

                PaperCameFirst(newPaperState.value, paperToSnapshot)
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

            // This shouldn't happen; Paper should either come before FB, or never.  Investigate.
            MyCrashlytics.logException(PaperCacheException("paper came after firebase"))

            return Terminal(null)
        }
    }

    class PaperCameFirst<T : Any, U : Any>(private val paper: T, paperToSnapshot: (T) -> U) : PairState<T, U>() {

        override val emission = paperToSnapshot(paper)

        override fun processNextPair(
                newPaperState: AndroidDatabaseWrapper.LoadState<T>,
                newFirebaseState: AndroidDatabaseWrapper.LoadState<U>,
        ): PairState<T, U> {
            check(newPaperState is AndroidDatabaseWrapper.LoadState.Loaded)
            check(newFirebaseState is AndroidDatabaseWrapper.LoadState.Loaded)

            return if (newFirebaseState.value == paper) {
                Terminal(null)
            } else {
                /**
                 * This isn't necessarily an issue, but I'm expecting them always to be consistent.  It would be nice
                 * to know what happened.
                 */
                MyCrashlytics.logException(PaperCacheException("firebase was different than paper"))

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

private class PaperCacheException(message: String) : Exception(message)