package com.krystianwsul.checkme.gui.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.Layout
import android.text.StaticLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.internal.CollapsingTextHelper
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.CollapseAppBarLayoutBinding
import com.krystianwsul.checkme.utils.addOneShotGlobalLayoutListener
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.utils.dpToPx
import com.krystianwsul.checkme.utils.getPrivateField
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.disposables.CompositeDisposable
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

    private val searchingRelay = BehaviorRelay.createDefault(false)

    val isSearching get() = searchingRelay.value!!

    val filterCriteria by lazy {
        searchingRelay.switchMap {
            if (it) {
                binding.searchInclude
                        .toolbar
                        .filterCriteriaObservable
            } else {
                Preferences.showAssignedObservable.map { TreeViewAdapter.FilterCriteria(showAssignedToOthers = it) }
            }
        }!!
    }

    private val attachedToWindowDisposable = CompositeDisposable()

    private val globalLayoutPerformed = BehaviorRelay.create<Unit>()

    private var collapseState: CollapseState = CollapseState.Expanded

    private val binding = CollapseAppBarLayoutBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        binding.searchInclude
                .toolbar
                .setNavigationOnClickListener { closeSearch() }

        binding.toolbarCollapseText.addOneShotGlobalLayoutListener { globalLayoutPerformed.accept(Unit) }
    }

    fun setSearchMenuOptions(showDeleted: Boolean, showAssignedToOthers: Boolean) {
        binding.searchInclude
                .toolbar
                .setMenuOptions(showDeleted, showAssignedToOthers)
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

    private var first = true
    fun configureMenu(
            @MenuRes menuId: Int,
            @IdRes searchItemId: Int,
            @IdRes showAssignedToOthersId: Int? = null,
            listener: ((Int) -> Unit)? = null,
    ) {
        check(first)

        first = false

        binding.toolbar.apply {
            inflateMenu(menuId)

            showAssignedToOthersId?.let {
                Preferences.showAssignedObservable
                        .subscribe { menu.findItem(showAssignedToOthersId).isChecked = it }
                        .addTo(attachedToWindowDisposable)
            }

            setOnMenuItemClickListener {
                when (val itemId = it.itemId) {
                    searchItemId -> startSearch()
                    showAssignedToOthersId -> Preferences.showAssigned = !Preferences.showAssigned
                    else -> listener?.invoke(itemId) ?: throw IllegalArgumentException()
                }

                true
            }
        }
    }

    fun closeSearch() {
        expand(true)

        binding.searchInclude
                .toolbar
                .apply {
                    check(isVisible)

                    animateVisibility(listOf(), listOf(this), duration = MyBottomBar.duration)

                    clearSearch()
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

    private fun startSearch() {
        searchingRelay.accept(true)

        collapse(true)

        animateVisibility(listOf(binding.searchInclude.toolbar), listOf(), duration = MyBottomBar.duration)

        binding.searchInclude
                .toolbar
                .requestSearchFocus()
    }

    override fun onSaveInstanceState(): Parcelable = SavedState(super.onSaveInstanceState(), isSearching)

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is SavedState) {
            super.onRestoreInstanceState(state.superState)

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

        var isSearching: Boolean

        constructor(source: Parcel) : super(source) {
            isSearching = source.readInt() == 1
        }

        constructor(superState: Parcelable?, isSearching: Boolean) : super(superState) {
            this.isSearching = isSearching
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)

            out.writeInt(if (isSearching) 1 else 0)
        }
    }

    private sealed class CollapseState {

        data class Collapsed(val titleHack: Boolean) : CollapseState()

        object Expanded : CollapseState()
    }
}