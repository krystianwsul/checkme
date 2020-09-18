package com.krystianwsul.checkme.gui

import android.animation.ValueAnimator
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.StaticLayout
import android.util.AttributeSet
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.MenuRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.internal.CollapsingTextHelper
import com.jakewharton.rxbinding3.widget.textChanges
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.collapse_app_bar_layout.view.*
import kotlin.math.abs

class CollapseAppBarLayout : AppBarLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var valueAnimator: ValueAnimator? = null

    private var initialHeight: Int? = null

    private lateinit var title: String
    private lateinit var paddingLayout: View

    private val collapsingTextHelper: CollapsingTextHelper by lazy {
        toolbarCollapseLayout.getPrivateField("collapsingTextHelper")
    }

    private val textLayout: StaticLayout by lazy {
        collapsingTextHelper.getPrivateField("textLayout")
    }

    val menu get() = toolbar.menu!!

    private val inputMethodManager by lazy {
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    private val searchingRelay = BehaviorRelay.createDefault(false)
    private val showDeleted = BehaviorRelay.createDefault(false)

    val isSearching get() = searchingRelay.value!!

    val searchData = BehaviorRelay.create<NullableWrapper<TaskListFragment.SearchData>>()

    private val attachedToWindowDisposable = CompositeDisposable()

    init {
        View.inflate(context, R.layout.collapse_app_bar_layout, this)

        searchToolbar.apply {
            inflateMenu(R.menu.main_activity_search)

            setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.actionSearchClose -> searchToolbarText.text = null
                    R.id.actionSearchShowDeleted -> showDeleted.accept(!showDeleted.value!!)
                    else -> throw IllegalArgumentException()
                }

                true
            }

            setNavigationOnClickListener { closeSearch() }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        attachedToWindowDisposable += showDeleted.subscribe {
            searchToolbar
                    .menu
                    .findItem(R.id.actionSearchShowDeleted)
                    .isChecked = it
        }

        searchingRelay.flatMap {
            if (it) {
                Observables.combineLatest(
                        searchToolbarText.textChanges(),
                        showDeleted
                ) { searchText, showDeleted ->
                    NullableWrapper(TaskListFragment.SearchData(searchText.toString(), showDeleted))
                }
            } else {
                Observable.just(NullableWrapper())
            }
        }
                .subscribe(searchData)
                .addTo(attachedToWindowDisposable)
    }

    override fun onDetachedFromWindow() {
        attachedToWindowDisposable.clear()

        super.onDetachedFromWindow()
    }

    fun setText(title: String, text: String?, paddingLayout: View) {
        this.title = title
        this.paddingLayout = paddingLayout

        valueAnimator?.cancel()

        val hide = searchingRelay.value!!

        if (!hide) toolbarCollapseLayout.title = title

        toolbarCollapseText.also {
            val hideText = text.isNullOrEmpty() || hide

            it.isVisible = !hideText
            it.text = text

            it.addOneShotGlobalLayoutListener {
                if (initialHeight == null) initialHeight = it.height

                animateHeight(hideText)
            }
        }
    }

    private fun animateHeight(hideText: Boolean) {
        fun setNewHeight(newHeight: Int) {
            updateLayoutParams<CoordinatorLayout.LayoutParams> {
                height = newHeight
            }

            paddingLayout.setPadding(0, 0, 0, newHeight)
        }

        val newHeight = if (hideText) {
            toolbar.height + 1 // stupid hack because otherwise title doesn't show
        } else {
            initialHeight!! + context.dpToPx(35).toInt() + textLayout.height
        }

        if (abs(newHeight - height) <= 1) { // same stupid hack
            setNewHeight(newHeight)
        } else {
            valueAnimator = ValueAnimator.ofInt(height, newHeight).apply {
                addUpdateListener {
                    val currentHeight = it.animatedValue as Int

                    setNewHeight(currentHeight)
                }

                start()
            }
        }
    }

    fun inflateMenu(@MenuRes resId: Int) = toolbar.inflateMenu(resId)

    fun setOnMenuItemClickListener(listener: (MenuItem) -> Unit) {
        toolbar.setOnMenuItemClickListener {
            listener(it)

            true
        }
    }

    fun closeSearch() {
        toolbarCollapseLayout.title = title

        val hideText = toolbarCollapseText.text.isEmpty()
        toolbarCollapseText.isVisible = !hideText

        animateHeight(hideText)

        searchToolbar.apply {
            check(isVisible)

            animateVisibility(listOf(), listOf(this), duration = MyBottomBar.duration)
        }

        searchToolbarText.text = null

        searchingRelay.accept(false)
    }

    fun startSearch() {
        searchingRelay.accept(true)

        toolbarCollapseLayout.title = null
        toolbarCollapseText.isVisible = false

        animateHeight(true)

        animateVisibility(listOf(searchToolbar), listOf(), duration = MyBottomBar.duration)

        searchToolbarText.apply {
            requestFocus()

            inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        return SavedState(super.onSaveInstanceState()).apply {
            searchData = this@CollapseAppBarLayout.searchData
                    .getCurrentValue()
                    .value
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)

            state.searchData?.let {
                searchingRelay.accept(true)
                showDeleted.accept(it.showDeleted)

                searchToolbarText.apply {
                    setText(it.query)

                    requestFocus()
                }

                searchToolbar.isVisible = true
            }
        } else {
            super.onRestoreInstanceState(state)
        }
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

        var searchData: TaskListFragment.SearchData? = null

        constructor(source: Parcel) : super(source) {
            searchData = source.readParcelable(SavedState::class.java.classLoader)
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)

            out.writeParcelable(searchData, 0)
        }
    }
}