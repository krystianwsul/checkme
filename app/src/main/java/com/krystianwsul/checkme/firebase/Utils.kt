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
    )

    // three pairs. ignore pair 1, because that's just both initial
    val secondPairSingle = permutationObservable.skip(1).firstOrError()
    val thirdPairSingle = permutationObservable.skip(2).firstOrError()

    val firstValueSingle: Observable<U> = secondPairSingle.flatMapObservable { (paper, firebase) ->
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

    return listOf(firstValueSingle, firebaseObservable.skip(1)).merge()
}