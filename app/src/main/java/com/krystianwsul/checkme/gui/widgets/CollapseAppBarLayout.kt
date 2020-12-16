package com.krystianwsul.checkme.gui.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.Layout
import android.text.StaticLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.MenuRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.internal.CollapsingTextHelper
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.CollapseAppBarLayoutBinding
import com.krystianwsul.checkme.utils.addOneShotGlobalLayoutListener
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.utils.dpToPx
import com.krystianwsul.checkme.utils.getPrivateField
import com.krystianwsul.common.utils.normalized
import com.krystianwsul.treeadapter.TreeViewAdapter
import com.krystianwsul.treeadapter.getCurrentValue
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import kotlin.math.abs
import kotlin.math.roundToInt

class CollapseAppBarLayout : AppBarLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var valueAnimator: ValueAnimator? = null

    private var initialTextHeight: Int? = null

    private var title: String? = null
    private var paddingLayout: View? = null

    private val collapsingTextHelper: CollapsingTextHelper by lazy {
        binding.toolbarCollapseLayout.getPrivateField("collapsingTextHelper")
    }

    private val textLayout: StaticLayout? get() = collapsingTextHelper.getPrivateField("textLayout")

    val menu get() = binding.toolbar.menu!!

    private val inputMethodManager by lazy {
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    private val searchingRelay = BehaviorRelay.createDefault(false)
    private val showDeleted = BehaviorRelay.createDefault(false)

    val isSearching get() = searchingRelay.value!!

    val filterCriteria = BehaviorRelay.create<TreeViewAdapter.FilterCriteria>()

    private val attachedToWindowDisposable = CompositeDisposable()

    private val globalLayoutPerformed = BehaviorRelay.create<Unit>()

    private var collapseState: CollapseState = CollapseState.Expanded

    private val binding = CollapseAppBarLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        binding.searchInclude
                .toolbar
                .apply {
                    inflateMenu(R.menu.main_activity_search)

                    setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)

                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.actionSearchClose -> text = ""
                            R.id.actionSearchShowDeleted -> showDeleted.accept(!showDeleted.value!!)
                            else -> throw IllegalArgumentException()
                        }

                        true
                    }

                    setNavigationOnClickListener { closeSearch() }
                }

        binding.toolbarCollapseText.addOneShotGlobalLayoutListener { globalLayoutPerformed.accept(Unit) }
    }

    fun hideShowDeleted() {
        binding.searchInclude
                .toolbar
                .menu
                .findItem(R.id.actionSearchShowDeleted)
                .isVisible = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        attachedToWindowDisposable += showDeleted.subscribe {
            binding.searchInclude
                    .toolbar
                    .menu
                    .findItem(R.id.actionSearchShowDeleted)
                    .isChecked = it
        }

        searchingRelay.flatMap {
            if (it) {
                Observables.combineLatest(
                        binding.searchInclude
                                .toolbar
                                .textChanges(),
                        showDeleted
                ) { searchText, showDeleted ->
                    TreeViewAdapter.FilterCriteria(searchText.toString().normalized(), showDeleted)
                }
            } else {
                Observable.just(TreeViewAdapter.FilterCriteria())
            }
        }
                .subscribe(filterCriteria)
                .addTo(attachedToWindowDisposable)
    }

    override fun onDetachedFromWindow() {
        attachedToWindowDisposable.clear()

        super.onDetachedFromWindow()
    }


    private val textWidth by lazy {
        val screenWidth = resources.displayMetrics.widthPixels
        val collapseTextStartMargin = resources.getDimension(R.dimen.collapseTextStartMargin)
        val collapseTextEndMargin = resources.getDimension(R.dimen.collapseTextEndMargin)

        (screenWidth - collapseTextStartMargin - collapseTextEndMargin).roundToInt()
    }

    private val bottomMargin by lazy { dpToPx(35).toInt() }

    fun setText(title: String, text: String?, paddingLayout: View?, immediate: Boolean) {
        this.title = title
        this.paddingLayout = paddingLayout

        valueAnimator?.cancel()

        attachedToWindowDisposable += globalLayoutPerformed.subscribe {
            val hide = searchingRelay.value!! || collapseState is CollapseState.Collapsed

            if (!hide) binding.toolbarCollapseLayout.title = title

            binding.toolbarCollapseText.also {
                val hideText = text.isNullOrEmpty() || hide

                it.isVisible = !hideText
                it.text = text

                if (!text.isNullOrEmpty()) {
                    initialTextHeight = StaticLayout.Builder
                            .obtain(text, 0, text.length, it.paint, textWidth)
                            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            .setLineSpacing(it.lineSpacingExtra, it.lineSpacingMultiplier)
                            .setIncludePad(it.includeFontPadding)
                            .build()
                            .height
                }

                binding.toolbarCollapseLayout.addOneShotGlobalLayoutListener {
                    animateHeight(
                            hideText,
                            immediate,
                            (collapseState as? CollapseState.Collapsed)?.titleHack ?: true
                    )
                }
            }
        }
    }

    private fun animateHeight(hideText: Boolean, immediate: Boolean, titleHack: Boolean) {
        fun setNewHeight(newHeight: Int) {
            updateLayoutParams<CoordinatorLayout.LayoutParams> {
                height = newHeight
            }

            paddingLayout?.setPadding(0, 0, 0, newHeight)
        }

        val newHeight = when {
            collapseState is CollapseState.Collapsed || textLayout == null -> binding.toolbar.height
            hideText -> {
                (bottomMargin + textLayout!!.height).coerceAtLeast(binding.toolbar.height) + if (titleHack) 1 else 0
            }
            else -> initialTextHeight!! + bottomMargin + textLayout!!.height
        }

        if (immediate || abs(newHeight - height) <= 1) { // same stupid hack
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

    fun inflateMenu(@MenuRes resId: Int) = binding.toolbar.inflateMenu(resId)

    fun setOnMenuItemClickListener(listener: (MenuItem) -> Unit) {
        binding.toolbar.setOnMenuItemClickListener {
            listener(it)

            true
        }
    }

    fun closeSearch() {
        expand(true)

        binding.searchInclude
                .toolbar
                .apply {
                    check(isVisible)

                    animateVisibility(listOf(), listOf(this), duration = MyBottomBar.duration)

                    text = ""

                    closeKeyboard()
                }

        searchingRelay.accept(false)
    }

    fun collapse(titleHack: Boolean = false) {
        collapseState = CollapseState.Collapsed(titleHack)

        attachedToWindowDisposable += globalLayoutPerformed.subscribe {
            binding.toolbarCollapseLayout.title = null
            binding.toolbarCollapseText.isVisible = false

            animateHeight(true, immediate = false, titleHack = titleHack)
        }
    }

    fun expand(titleHack: Boolean = false) {
        collapseState = CollapseState.Expanded

        binding.toolbarCollapseLayout.title = title

        val hideText = binding.toolbarCollapseText.text.isEmpty()
        binding.toolbarCollapseText.isVisible = !hideText

        animateHeight(hideText, false, titleHack = titleHack)
    }

    fun startSearch() {
        searchingRelay.accept(true)

        collapse(true)

        animateVisibility(listOf(binding.searchInclude.toolbar), listOf(), duration = MyBottomBar.duration)

        binding.searchInclude
                .toolbar
                .apply {
                    requestSearchFocus()
                    showKeyboard()
                }
    }

    override fun onSaveInstanceState(): Parcelable = SavedState(
            super.onSaveInstanceState(),
            this@CollapseAppBarLayout.filterCriteria.getCurrentValue(),
            this@CollapseAppBarLayout.isSearching
    )

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)

            binding.searchInclude
                    .toolbar
                    .text = state.filterCriteria.query

            showDeleted.accept(state.filterCriteria.showDeleted)

            searchingRelay.accept(state.isSearching)

            if (state.isSearching) {
                binding.searchInclude
                        .toolbar
                        .isVisible = true

                binding.toolbarCollapseLayout.title = null
                binding.toolbarCollapseText.isVisible = false

                binding.searchInclude
                        .toolbar
                        .requestSearchFocus()
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

        var filterCriteria: TreeViewAdapter.FilterCriteria
        var isSearching: Boolean

        constructor(source: Parcel) : super(source) {
            filterCriteria = source.readParcelable(SavedState::class.java.classLoader)!!
            isSearching = source.readInt() == 1
        }

        constructor(
                superState: Parcelable?,
                filterCriteria: TreeViewAdapter.FilterCriteria,
                isSearching: Boolean,
        ) : super(superState) {
            this.filterCriteria = filterCriteria
            this.isSearching = isSearching
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)

            out.writeParcelable(filterCriteria, 0)
            out.writeInt(if (isSearching) 1 else 0)
        }
    }

    private sealed class CollapseState {

        data class Collapsed(val titleHack: Boolean) : CollapseState()

        object Expanded : CollapseState()
    }
}