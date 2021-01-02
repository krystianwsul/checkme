package com.krystianwsul.checkme.gui.utils

import com.krystianwsul.checkme.viewmodels.DomainData
import com.krystianwsul.checkme.viewmodels.DomainViewModel
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.treeadapter.FilterCriteria
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign

fun <T : DomainData> connectInstanceSearch(
        filterCriteriaObservable: Observable<FilterCriteria.Full>,
        showDone: Boolean,
        getPage: () -> Int,
        setPage: (Int) -> Unit,
        onProgressShownObservable: Observable<Unit>,
        compositeDisposable: CompositeDisposable,
        viewModel: DomainViewModel<T>,
        setAdapterData: (data: T) -> Unit,
        startViewModel: (searchCriteria: SearchCriteria, page: Int) -> Unit,
        excludedInstanceKeys: Set<InstanceKey>,
) {
    /**
     * This whole observable flow no longer serves its original purpose, but it happens to trigger searches more or less
     * when I want it to.
     */

    val searchParameters = Observables.combineLatest(
            filterCriteriaObservable.distinctUntilChanged().map {
                SearchCriteria(it.query, it.filterParams.showAssignedToOthers, showDone, excludedInstanceKeys)
            },
            onProgressShownObservable.doOnNext { setPage(getPage() + 1) }.startWith(Unit)
    )
            .replay(1)
            .apply { compositeDisposable += connect() }

    viewModel.data
            .doOnNext(setAdapterData)
            .map { }
            .startWith(Unit)
            .switchMap { searchParameters }
            .subscribe { startViewModel(it.first, getPage()) }
            .addTo(compositeDisposable)
}