package com.krystianwsul.checkme.firebase

import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.kotlin.merge

inline fun <T : Any, U : Any> mergePaperAndRx(
        paperMaybe: Maybe<T>,
        firebaseObservable: Observable<U>,
        crossinline paperToSnapshot: (T) -> U,
        crossinline equal: (T, U) -> Boolean,
): Observable<U> {
    val permutationObservable = Observables.combineLatest(
            paperMaybe.toObservable()
                    .map<AndroidDatabaseWrapper.LoadState<T>> { AndroidDatabaseWrapper.LoadState.Loaded(it) }
                    .startWithItem(AndroidDatabaseWrapper.LoadState.Initial()),
            firebaseObservable.take(1)
                    .map<AndroidDatabaseWrapper.LoadState<U>> { AndroidDatabaseWrapper.LoadState.Loaded(it) }
                    .startWithItem(AndroidDatabaseWrapper.LoadState.Initial()),
    ).share()

    // three pairs. ignore pair 1, because that's just both initial
    val secondPairSingle = permutationObservable.skip(1).firstOrError()
    val thirdPairSingle = permutationObservable.skip(2).firstOrError()

    val firstValueObservable: Observable<U> = secondPairSingle.flatMapObservable { (paper, firebase) ->
        if (paper is AndroidDatabaseWrapper.LoadState.Loaded) {
            check(firebase is AndroidDatabaseWrapper.LoadState.Initial)

            listOf(
                    Single.just(paperToSnapshot(paper.value)).toObservable(),
                    thirdPairSingle.flatMapMaybe { (paper2, firebase2) ->
                        check(paper2 is AndroidDatabaseWrapper.LoadState.Loaded)
                        check(firebase2 is AndroidDatabaseWrapper.LoadState.Loaded)

                        if (equal(paper2.value, firebase2.value)) {
                            Maybe.empty()
                        } else {
                            Maybe.just(firebase2.value)
                        }
                    }.toObservable()
            ).merge()
        } else {
            check(firebase is AndroidDatabaseWrapper.LoadState.Loaded)

            Observable.just(firebase.value)
        }
    }

    return listOf(firstValueObservable, firebaseObservable.skip(1)).merge()
}

private sealed class PairState<T : Any, U : Any> {

    abstract fun processNextPair(
            newPaperState: AndroidDatabaseWrapper.LoadState<T>,
            newFirebaseState: AndroidDatabaseWrapper.LoadState<U>,
    ): PairState<T, U>

    // todo fun emit U

    class Initial<T : Any, U : Any> : PairState<T, U>() {

        override fun processNextPair(
                newPaperState: AndroidDatabaseWrapper.LoadState<T>,
                newFirebaseState: AndroidDatabaseWrapper.LoadState<U>,
        ): PairState<T, U> {
            check(newPaperState is AndroidDatabaseWrapper.LoadState.Initial)
            check(newFirebaseState is AndroidDatabaseWrapper.LoadState.Initial)

            return SkippingFirst()
        }
    }

    class SkippingFirst<T : Any, U : Any> : PairState<T, U>() {

        override fun processNextPair(
                newPaperState: AndroidDatabaseWrapper.LoadState<T>,
                newFirebaseState: AndroidDatabaseWrapper.LoadState<U>,
        ): PairState<T, U> {
            return if (newPaperState is AndroidDatabaseWrapper.LoadState.Initial) {
                check(newFirebaseState is AndroidDatabaseWrapper.LoadState.Loaded)

                FirebaseCameFirst(newPaperState, newFirebaseState.value) // todo paper log warning if paper comes after this
            } else {
                check(newPaperState is AndroidDatabaseWrapper.LoadState.Loaded)
                check(newFirebaseState is AndroidDatabaseWrapper.LoadState.Initial)

                PaperCameFirst(newPaperState.value, newFirebaseState)
            }
        }
    }

    class FirebaseCameFirst<T : Any, U : Any>(
            paperState: AndroidDatabaseWrapper.LoadState.Initial<T>,
            private val firebase: U,
    ) : PairState<T, U>() {

        override fun processNextPair(
                newPaperState: AndroidDatabaseWrapper.LoadState<T>,
                newFirebaseState: AndroidDatabaseWrapper.LoadState<U>,
        ): PairState<T, U> {
            check(newPaperState is AndroidDatabaseWrapper.LoadState.Loaded)
            check(newFirebaseState is AndroidDatabaseWrapper.LoadState.Loaded)

            check(newFirebaseState.value == firebase)

            // todo paper log warning

            return Terminal()
        }
    }

    class PaperCameFirst<T : Any, U : Any>(
            private val paper: T,
            private val firebaseState: AndroidDatabaseWrapper.LoadState<U>,
    ) : PairState<T, U>() {

        override fun processNextPair(
                newPaperState: AndroidDatabaseWrapper.LoadState<T>,
                newFirebaseState: AndroidDatabaseWrapper.LoadState<U>,
        ): PairState<T, U> {
            check(newPaperState is AndroidDatabaseWrapper.LoadState.Loaded)
            check(newFirebaseState is AndroidDatabaseWrapper.LoadState.Loaded)

            // todo emit firebase if different from paper

            return Terminal()
        }
    }

    class Terminal<T : Any, U : Any>() : PairState<T, U>() {

        override fun processNextPair(
                newPaperState: AndroidDatabaseWrapper.LoadState<T>,
                newFirebaseState: AndroidDatabaseWrapper.LoadState<U>,
        ): PairState<T, U> = throw IllegalStateException()
    }
}