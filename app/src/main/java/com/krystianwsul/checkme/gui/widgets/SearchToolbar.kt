package com.krystianwsul.checkme.gui.widgets

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import com.jakewharton.rxbinding3.widget.textChanges
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ToolbarSearchInnerBinding
import com.krystianwsul.common.utils.normalized
import com.krystianwsul.treeadapter.FilterCriteria
import com.krystianwsul.treeadapter.getCurrentValue
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign

class SearchToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) :
        FrameLayout(context, attrs, defStyleAttr) {

    private val binding =
            ToolbarSearchInnerBinding.inflate(LayoutInflater.from(context), this, true)

    private val inputMethodManager by lazy {
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    private val showDeletedRelay = BehaviorRelay.createDefault(false)
    private val showAssignedToOthersRelay = BehaviorRelay.createDefault(Preferences.showAssigned)

    val filterCriteriaObservable by lazy {
        Observables.combineLatest(
                binding.searchText
                        .textChanges()
                        .map { it.toString() }
                        .distinctUntilChanged()
                        .map { it.normalized() },
                showDeletedRelay,
                showAssignedToOthersRelay,
        )
                .map { (query, showDeleted, showAssignedToOthers) ->
                    FilterCriteria.Full(query, showDeleted, showAssignedToOthers)
                }
                .distinctUntilChanged()
                .replay(1)
                .apply { attachedToWindowDisposable += connect() }!!
    }

    private val attachedToWindowDisposable = CompositeDisposable()

    init {
        binding.searchToolbar.apply {
            inflateMenu(R.menu.main_activity_search)

            setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)

            setOnMenuItemClickListener {
                fun BehaviorRelay<Boolean>.toggle() = accept(!value!!)

                when (it.itemId) {
                    R.id.actionSearchClose -> binding.searchText.text = null
                    R.id.actionSearchShowDeleted -> showDeletedRelay.toggle()
                    R.id.actionSearchShowAssigned -> showAssignedToOthersRelay.toggle()
                    else -> throw IllegalArgumentException()
                }

                true
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        binding.searchToolbar
                .menu
                .apply {
                    attachedToWindowDisposable += showDeletedRelay.subscribe {
                        findItem(R.id.actionSearchShowDeleted).isChecked = it
                    }

                    attachedToWindowDisposable += showAssignedToOthersRelay.subscribe {
                        findItem(R.id.actionSearchShowAssigned).isChecked = it
                    }
                }
    }

    override fun onDetachedFromWindow() {
        attachedToWindowDisposable.clear()

        super.onDetachedFromWindow()
    }

    fun requestSearchFocus() {
        binding.searchText.requestFocus()
        inputMethodManager.showSoftInput(binding.searchText, InputMethodManager.SHOW_IMPLICIT)
    }

    fun setNavigationOnClickListener(listener: () -> Unit) =
            binding.searchToolbar.setNavigationOnClickListener { listener() }

    override fun onSaveInstanceState(): Parcelable = SavedState(
            super.onSaveInstanceState(),
            filterCriteriaObservable.getCurrentValue()
    )

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)

            binding.searchText.setText(state.filterCriteria.query)
            showDeletedRelay.accept(state.filterCriteria.showDeleted)
            showAssignedToOthersRelay.accept(state.filterCriteria.showAssignedToOthers)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    fun setMenuOptions(showDeleted: Boolean, showAssignedToOthers: Boolean) {
        binding.searchToolbar
                .menu
                .apply {
                    findItem(R.id.actionSearchShowDeleted).isVisible = showDeleted
                    findItem(R.id.actionSearchShowAssigned).isVisible = showAssignedToOthers
                }
    }

    fun clearSearch() {
        binding.searchText.text = null
        inputMethodManager.hideSoftInputFromWindow(binding.searchText.windowToken, 0)
    }

    private class SavedState : BaseSavedState {

        companion object {

            @Suppress("unused")
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {

                override fun createFromParcel(source: Parcel) = SavedState(source)

                override fun newArray(size: Int) = arrayOfNulls<SavedState>(size)
            }
        }

        var filterCriteria: FilterCriteria.Full

        constructor(source: Parcel) : super(source) {
            filterCriteria = source.readParcelable(FilterCriteria::class.java.classLoader)!!
        }

        constructor(superState: Parcelable?, filterCriteria: FilterCriteria.Full) : super(superState) {
            this.filterCriteria = filterCriteria
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)

            out.writeParcelable(filterCriteria, 0)
        }
    }
}