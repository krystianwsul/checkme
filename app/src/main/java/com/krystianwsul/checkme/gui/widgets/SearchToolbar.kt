package com.krystianwsul.checkme.gui.widgets

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.core.content.getSystemService
import com.jakewharton.rxbinding4.appcompat.navigationClicks
import com.jakewharton.rxbinding4.widget.textChanges
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ToolbarSearchInnerBinding
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.utils.normalized
import com.krystianwsul.treeadapter.getCurrentValue
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign

class SearchToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) :
        FrameLayout(context, attrs, defStyleAttr) {

    private val binding =
            ToolbarSearchInnerBinding.inflate(LayoutInflater.from(context), this, true)

    private val inputMethodManager by lazy { context.getSystemService<InputMethodManager>()!! }

    val searchParamsObservable by lazy {
        Observables.combineLatest(
            binding.searchText
                .textChanges()
                .map { it.toString() }
                .distinctUntilChanged()
                .map { it.normalized() }
                .distinctUntilChanged(),
            Preferences.showAssignedObservable,
        )
            .map { (query, showAssignedToOthers) -> SearchParams(query, showAssignedToOthers) }
            .distinctUntilChanged()
            .replay(1)
            .apply { attachedToWindowDisposable += connect() } // this should be hooked up in attachedToWindow
    }

    private val attachedToWindowDisposable = CompositeDisposable()

    init {
        binding.searchToolbar.apply {
            inflateMenu(R.menu.main_activity_search)

            setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)

            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.actionSearchClose -> binding.searchText.text = null
                    R.id.actionSearchShowDeleted -> Preferences.showDeleted = !Preferences.showDeleted
                    R.id.actionSearchShowAssigned -> Preferences.showAssigned = !Preferences.showAssigned
                    R.id.actionSearchShowProjects -> Preferences.showProjects = !Preferences.showProjects
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
                    Preferences.showDeletedObservable
                            .subscribe { findItem(R.id.actionSearchShowDeleted).isChecked = it }
                            .addTo(attachedToWindowDisposable)

                    Preferences.showAssignedObservable
                            .subscribe { findItem(R.id.actionSearchShowAssigned).isChecked = it }
                            .addTo(attachedToWindowDisposable)

                    Preferences.showProjectsObservable
                            .subscribe { findItem(R.id.actionSearchShowProjects).isChecked = it }
                            .addTo(attachedToWindowDisposable)
                }
    }

    override fun onDetachedFromWindow() {
        attachedToWindowDisposable.clear()

        super.onDetachedFromWindow()
    }

    fun requestSearchFocus(delayed: Boolean = false) {
        binding.searchText.requestFocus()

        fun showKeyboard() = inputMethodManager.showSoftInput(binding.searchText, InputMethodManager.SHOW_IMPLICIT)

        if (delayed) {
            postDelayed(::showKeyboard, 1000)
        } else {
            showKeyboard()
        }
    }

    fun navigationClicks() = binding.searchToolbar.navigationClicks()

    override fun onSaveInstanceState(): Parcelable = SavedState(
        super.onSaveInstanceState(),
        searchParamsObservable.getCurrentValue().query,
    )

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)

            binding.searchText.setText(state.query)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    fun setMenuOptions(showDeleted: Boolean, showAssignedToOthers: Boolean, showProjects: Boolean) {
        binding.searchToolbar
                .menu
                .apply {
                    findItem(R.id.actionSearchShowDeleted).isVisible = showDeleted
                    findItem(R.id.actionSearchShowAssigned).isVisible = showAssignedToOthers
                    findItem(R.id.actionSearchShowProjects).isVisible = showProjects
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

        var query: String

        constructor(source: Parcel) : super(source) {
            query = source.readString()!!
        }

        constructor(superState: Parcelable?, query: String) : super(superState) {
            this.query = query
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)

            out.writeString(query)
        }
    }

    data class SearchParams(
        val query: String = SearchCriteria.Search
            .Query
            .empty
            .query,
        val showAssignedToOthers: Boolean = SearchCriteria.empty.showAssignedToOthers,
    )
}