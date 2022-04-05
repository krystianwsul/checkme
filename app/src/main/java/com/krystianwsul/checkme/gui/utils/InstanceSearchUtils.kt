package com.krystianwsul.checkme.gui.utils

import com.krystianwsul.checkme.viewmodels.DomainData
import com.krystianwsul.checkme.viewmodels.DomainViewModel
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.treeadapter.FilterCriteria
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign

fun <T : DomainData> connectInstanceSearch(
        searchCriteriaObservable: Observable<SearchCriteria>,
        getPage: () -> Int,
        setPage: (Int) -> Unit,
        onProgressShownObservable: Observable<Unit>,
        compositeDisposable: CompositeDisposable,
        viewModel: DomainViewModel<T>,
        setAdapterData: (data: T) -> Unit,
        startViewModel: (searchCriteria: SearchCriteria, page: Int) -> Unit,
) {
    /**
     * This whole observable flow no longer serves its original purpose, but it happens to trigger searches more or less
     * when I want it to.
     */

    val searchParameters = Observables.combineLatest(
            searchCriteriaObservable,
            onProgressShownObservable.doOnNext { setPage(getPage() + 1) }.startWithItem(Unit),
    )
            .replay(1)
            .apply { compositeDisposable += connect() }

        viewModel.data
                .doOnNext(setAdapterData)
                .map { }
                .startWithItem(Unit)
                .switchMap { searchParameters }
                .subscribe { startViewModel(it.first, getPage()) }
                .addTo(compositeDisposable)
}

private fun FilterCriteria.Full.toSearchCriteria(showDone: Boolean, excludedInstanceKeys: Set<InstanceKey>) = SearchCriteria(
        search,
        filterParams.showAssignedToOthers,
        showDone,
        excludedInstanceKeys,
)

fun Observable<FilterCriteria.Full>.toSearchCriteria(showDone: Boolean, excludedInstanceKeys: Set<InstanceKey>) =
        map { it.toSearchCriteria(showDone, excludedInstanceKeys) }