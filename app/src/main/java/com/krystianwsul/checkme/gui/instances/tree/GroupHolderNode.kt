package com.krystianwsul.checkme.gui.instances.tree

import android.app.Activity
import android.graphics.Color
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.RowAssignedChipBinding
import com.krystianwsul.checkme.domainmodel.toImageLoader
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.checkme.gui.instances.tree.avatar.AvatarDelegate
import com.krystianwsul.checkme.gui.instances.tree.checkable.CheckBoxState
import com.krystianwsul.checkme.gui.instances.tree.checkable.CheckableDelegate
import com.krystianwsul.checkme.gui.instances.tree.expandable.ExpandableDelegate
import com.krystianwsul.checkme.utils.isLandscape
import com.krystianwsul.checkme.utils.loadPhoto
import com.krystianwsul.checkme.utils.setIndent
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.ModelState
import com.krystianwsul.treeadapter.TreeNode
import com.stfalcon.imageviewer.StfalconImageViewer
import io.reactivex.rxkotlin.addTo
import kotlin.math.ceil

abstract class GroupHolderNode(protected val indentation: Int) : ModelNode<NodeHolder> {

    companion object {

        fun getColor(@ColorRes color: Int) = ContextCompat.getColor(MyApplication.instance, color)

        val colorPrimary by lazy { getColor(R.color.textPrimary) }
        val colorSecondary by lazy { getColor(R.color.textSecondary) }
        val colorDisabled by lazy { getColor(R.color.textDisabled) }
        val colorSelected by lazy { getColor(R.color.selected) }
        val colorBackground by lazy { getColor(R.color.materialBackground) }

        const val TOTAL_LINES = 3

        private val textWidths = InitMap<WidthKey, BehaviorRelay<Int>> { BehaviorRelay.create() }
    }

    private class InitMap<T, U>(private val initializer: (T) -> U) {

        private val map = mutableMapOf<T, U>()

        operator fun get(key: T): U {
            if (!map.containsKey(key))
                map[key] = initializer(key)
            return map.getValue(key)
        }
    }

    protected abstract val treeNode: TreeNode<NodeHolder>

    protected abstract val name: NameData

    protected open val details: Pair<String, Int>? = null

    protected open val children: Pair<String, Int>? = null

    protected open val hasAvatar = false // todo delegate
    open val checkBoxState: CheckBoxState = CheckBoxState.Gone // todo delegate

    open fun onLongClick(viewHolder: RecyclerView.ViewHolder) = treeNode.onLongClick()

    final override val itemViewType = GroupListFragment.GroupAdapter.TYPE_GROUP

    protected open val textSelectable = false

    protected open val ripple = false

    protected open val imageData: ImageNode.ImageData? = null

    protected open val assignedTo: List<AssignedNode.User> = listOf()

    protected open val thumbnail: ImageState? = null

    protected open val delegates = listOf<NodeDelegate>()

    final override val state
        get() = State(
                id,
                name,
                details,
                children,
                indentation,
                imageData?.imageState,
                assignedTo,
                thumbnail,
                delegates.map { it.state }
        )

    protected open val colorBackground = GroupHolderNode.colorBackground

    data class State(
            val id: Any,
            val name: NameData?,
            val details: Pair<String, Int>?,
            val children: Pair<String, Int>?,
            val indentation: Int,
            val imageState: ImageState?,
            val assignedTo: List<AssignedNode.User>,
            val thumbnail: ImageState?,
            val delegateStates: List<Any>,
    ) : ModelState {

        override fun same(other: ModelState) = (other as State).id == id
    }

    protected fun showImage(rowBigImage: ImageView, taskImage: ImageNode.ImageData) {
        val activity = rowBigImage.context as Activity

        fun setStatusColor(@ColorInt color: Int) {
            activity.window.statusBarColor = color
        }

        val fixStatusBar = !activity.resources.isLandscape

        val viewer = StfalconImageViewer.Builder(
                rowBigImage.context,
                listOf(taskImage.imageState)
        ) { view, image -> image.toImageLoader().load(view) }
                .withTransitionFrom(rowBigImage)
                .withDismissListener {
                    taskImage.onDismiss()

                    if (fixStatusBar)
                        setStatusColor(ContextCompat.getColor(rowBigImage.context, R.color.primaryDarkColor))
                }.apply {
                    if (fixStatusBar) withHiddenStatusBar(false)
                }
                .show()

        if (fixStatusBar) setStatusColor(Color.BLACK)

        taskImage.onImageShown(viewer)
    }

    final override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, startingDrag: Boolean) {
        val groupHolder = viewHolder as NodeHolder

        groupHolder.run {
            val taskImage = imageData

            if (taskImage != null) {
                rowContainer.visibility = View.GONE
                rowBigImageLayout!!.visibility = View.VISIBLE
                rowChipGroup.isVisible = false

                taskImage.imageState
                        .toImageLoader()
                        .load(rowBigImage!!)

                if (taskImage.showImage) showImage(rowBigImage!!, taskImage)
            } else {
                rowContainer.visibility = View.VISIBLE
                rowBigImageLayout?.visibility = View.GONE

                val widthKey = WidthKey(
                        indentation,
                        checkBoxState.visibility == View.GONE,
                        rowContainer.context
                                .resources
                                .configuration
                                .orientation,
                        hasAvatar,
                        thumbnail != null
                )

                rowContainer.setIndent(indentation)

                val textWidthRelay = textWidths[widthKey]

                val minLines = 1 + (details?.let { 1 } ?: 0) + (children?.let { 1 } ?: 0)

                fun allocateLines(textViews: List<TextView>) {
                    var remainingLines = TOTAL_LINES - minLines

                    textViews.forEach { textView ->
                        fun getWantLines(text: String) = Rect().run {
                            if (textWidthRelay.value != null) {
                                textView.paint.getTextBounds(text, 0, text.length, this)

                                ceil((width() + 1).toDouble() / textWidthRelay.value!!).toInt()
                            } else {
                                1
                            }
                        }

                        val wantLines = textView.text.toString()
                                .split('\n')
                                .map { getWantLines(it) }.sum()

                        val lines = listOf(wantLines, remainingLines + 1).minOrNull()!!

                        remainingLines -= (lines - 1)

                        if (lines == 1) {
                            textView.setSingleLine()
                        } else {
                            check(lines > 1)

                            textView.isSingleLine = false
                            textView.setLines(lines)
                        }
                    }
                }

                val allocateTextViews = mutableListOf<TextView>()

                rowName.run {
                    name.let {
                        when (it) {
                            is NameData.Visible -> {
                                visibility = View.VISIBLE
                                text = it.text
                                setTextColor(it.color)

                                if (it.unlimitedLines) {
                                    maxLines = Int.MAX_VALUE
                                    isSingleLine = false
                                } else {
                                    allocateTextViews += this
                                }
                            }
                            NameData.Invisible -> {
                                visibility = View.INVISIBLE

                                setSingleLine()
                            }
                            NameData.Gone -> {
                                visibility = View.GONE
                            }
                        }
                        setTextIsSelectable(textSelectable)
                    }
                }

                rowDetails.run {
                    details.let {
                        if (it != null) {
                            check(!name.unlimitedLines)

                            visibility = View.VISIBLE
                            text = it.first
                            setTextColor(it.second)

                            allocateTextViews += this
                        } else {
                            visibility = View.GONE
                        }
                    }
                }

                rowChildren.run {
                    children.let {
                        if (it != null) {
                            check(!name.unlimitedLines)

                            visibility = View.VISIBLE
                            text = it.first
                            setTextColor(it.second)

                            allocateTextViews += this
                        } else {
                            visibility = View.GONE
                        }
                    }
                }

                allocateLines(allocateTextViews)

                if (textWidthRelay.value == null) {
                    textWidthRelay.distinctUntilChanged()
                            .subscribe { allocateLines(allocateTextViews) }
                            .addTo(groupHolder.compositeDisposable)
                }

                rowTextLayout.apply {
                    viewTreeObserver.addOnGlobalLayoutListener {
                        textWidths[widthKey].accept(measuredWidth)
                    }
                }

                rowThumbnail.apply {
                    if (thumbnail != null) {
                        visibility = View.VISIBLE

                        thumbnail!!.toImageLoader().load(this, true)
                    } else {
                        visibility = View.GONE
                    }
                }

                delegates.forEach { it.onBindViewHolder(viewHolder) }
                // todo delegate remove these
                if (delegates.none { it is ExpandableDelegate<*> }) {
                    rowExpand.isGone = true
                    rowMarginEnd!!.isVisible = true
                }
                if (delegates.none { it is AvatarDelegate<*> }) rowImage?.isVisible = false
                if (delegates.none { it is CheckableDelegate<*> }) rowCheckBoxFrame.isGone = true

                rowMargin.isVisible = checkBoxState.visibility == View.GONE && !hasAvatar

                itemView.run {
                    setBackgroundColor(if (treeNode.isSelected && !(isPressed && startingDrag)) colorSelected else colorBackground)

                    foreground = if (ripple && !isPressed) ContextCompat.getDrawable(context, R.drawable.item_background_material) else null
                }

                if (assignedTo.isEmpty()) {
                    rowChipGroup.isVisible = false
                } else {
                    rowChipGroup.isVisible = true

                    rowChipGroup.removeAllViews()

                    assignedTo.forEach { user ->
                        RowAssignedChipBinding.inflate(
                                LayoutInflater.from(rowContainer.context),
                                rowChipGroup,
                                true
                        )
                                .root
                                .apply {
                                    text = user.name
                                    loadPhoto(user.photoUrl)
                                    isCloseIconVisible = false
                                }
                    }
                }
            }

            rowSeparator.visibility = if (treeNode.separatorVisible) View.VISIBLE else View.INVISIBLE
        }
    }

    private data class WidthKey(
            val indentation: Int,
            val checkBoxVisible: Boolean,
            val orientation: Int,
            val avatarVisible: Boolean,
            val thumbnailVisible: Boolean
    )
}
