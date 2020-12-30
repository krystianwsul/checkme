package com.krystianwsul.checkme.gui.widgets

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import com.jakewharton.rxbinding3.widget.textChanges
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ToolbarSearchInnerBinding
import com.krystianwsul.common.utils.normalized
import com.krystianwsul.treeadapter.FilterCriteria
import com.krystianwsul.treeadapter.getCurrentValue
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign

class SearchToolbar @JvmOverloads constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) :
        FrameLayout(context, attrs, defStyleAttr) {

    private val binding =
            ToolbarSearchInnerBinding.inflate(LayoutInflater.from(context), this, true)

    private val inputMethodManager by lazy {
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    val filterCriteriaObservable by lazy {
        Observables.combineLatest(
                binding.searchText
                        .textChanges()
                        .map { it.toString() }
                        .distinctUntilChanged()
                        .map { it.normalized() }
                        .distinctUntilChanged(),
                Preferences.filterParamsObservable,
        )
                .map { (query, filterParams) -> FilterCriteria.Full(query, filterParams) }
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
                when (it.itemId) {
                    R.id.actionSearchClose -> binding.searchText.text = null
                    R.id.actionSearchShowDeleted -> Preferences.showDeleted = !Preferences.showDeleted
                    R.id.actionSearchShowAssigned -> Preferences.showAssigned = !Preferences.showAssigned
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
                            .subscribe {
                                findItem(R.id.actionSearchShowDeleted).isChecked = it
                            }
                            .addTo(attachedToWindowDisposable)

                    Preferences.showAssignedObservable
                            .subscribe {
                                findItem(R.id.actionSearchShowAssigned).isChecked = it
                            }
                            .addTo(attachedToWindowDisposable)
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
            filterCriteriaObservable.getCurrentValue().query
    )

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)

            binding.searchText.setText(state.query)
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
}