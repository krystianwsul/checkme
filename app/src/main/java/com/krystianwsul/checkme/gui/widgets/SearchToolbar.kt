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
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.ToolbarSearchInnerBinding
import com.krystianwsul.common.utils.normalized
import com.krystianwsul.treeadapter.TreeViewAdapter
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

    val filterCriteriaObservable by lazy {
        Observables.combineLatest(
                showDeletedRelay,
                binding.searchText
                        .textChanges()
                        .map { it.toString() }
                        .distinctUntilChanged()
                        .map { it.normalized() }
        )
                .map { (showDeleted, query) ->
                    TreeViewAdapter.FilterCriteria(
                            query,
                            TreeViewAdapter.FilterCriteria.FilterParams(showDeleted)
                    )
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
                when (it.itemId) {
                    R.id.actionSearchClose -> binding.searchText.text = null
                    R.id.actionSearchShowDeleted -> showDeletedRelay.accept(!showDeletedRelay.value!!)
                    else -> throw IllegalArgumentException()
                }

                true
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        attachedToWindowDisposable += showDeletedRelay.subscribe {
            binding.searchToolbar
                    .menu
                    .findItem(R.id.actionSearchShowDeleted)
                    .isChecked = it
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
            showDeletedRelay.value!!,
            binding.searchText
                    .text
                    .toString()
    )

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)

            showDeletedRelay.accept(state.showDeleted)
            binding.searchText.setText(state.query)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    fun setShowDeletedVisible(isVisible: Boolean) { // todo assigned add option
        binding.searchToolbar
                .menu
                .findItem(R.id.actionSearchShowDeleted)
                .isVisible = isVisible
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

        var showDeleted: Boolean
        var query: String

        constructor(source: Parcel) : super(source) {
            showDeleted = source.readInt() == 1
            query = source.readString()!!
        }

        constructor(superState: Parcelable?, showDeleted: Boolean, query: String) : super(superState) {
            this.showDeleted = showDeleted
            this.query = query
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)

            out.writeInt(if (showDeleted) 1 else 0)
            out.writeString(query)
        }
    }
}