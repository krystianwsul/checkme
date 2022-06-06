package com.krystianwsul.checkme.gui.edit.dialogs.parentpicker

import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.gui.utils.connectInstanceSearch
import com.krystianwsul.checkme.viewmodels.EditInstancesSearchViewModel
import com.krystianwsul.common.criteria.SearchCriteria
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer

abstract class ParentInstancePickerDelegate(
    compositeDisposable: CompositeDisposable,
    private val editInstancesSearchViewModel: EditInstancesSearchViewModel,
) : ParentPickerFragment.Delegate {

    final override val startedRelay = Consumer<Boolean> { }

    private val queryRelay = BehaviorRelay.createDefault("")

    final override val adapterDataObservable = BehaviorRelay.create<ParentPickerFragment.AdapterData>()

    final override val initialScrollMatcher: ((ParentPickerFragment.EntryData) -> Boolean)? = null

    private val progressShownRelay = PublishRelay.create<Unit>()

    init {
        connectInstanceSearch(
            queryRelay.map { SearchCriteria(SearchCriteria.Search.Query(it), Preferences.showAssigned, false) },
            ::getPage,
            ::setPage,
            progressShownRelay,
            compositeDisposable,
            editInstancesSearchViewModel,
            { adapterDataObservable.accept(ParentPickerFragment.AdapterData(it.instanceEntryDatas, it.showLoader)) },
            editInstancesSearchViewModel::start,
        )
    }

    protected abstract fun getPage(): Int

    protected abstract fun setPage(page: Int)

    final override fun onSearch(query: String) = queryRelay.accept(query)

    final override fun onPaddingShown() = progressShownRelay.accept(Unit)
}