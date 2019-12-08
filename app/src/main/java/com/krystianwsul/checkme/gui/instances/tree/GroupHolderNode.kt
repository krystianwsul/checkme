package com.krystianwsul.checkme.gui.instances.tree

import android.graphics.Rect
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.toImageLoader
import com.krystianwsul.checkme.utils.loadPhoto
import com.krystianwsul.checkme.utils.setIndent
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.ModelState
import com.krystianwsul.treeadapter.TreeNode
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.loader.ImageLoader
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

    protected abstract val name: NameData?

    protected open val details: Pair<String, Int>? = null

    protected open val children: Pair<String, Int>? = null

    open val checkBoxState: CheckBoxState = CheckBoxState.Gone

    protected open val avatarImage: NullableWrapper<String>? = null

    open fun onLongClick(viewHolder: RecyclerView.ViewHolder) = treeNode.onLongClick()

    override val itemViewType: Int = GroupListFragment.GroupAdapter.TYPE_GROUP

    protected open val textSelectable = false

    open val ripple = false

    open val imageData: ImageNode.ImageData? = null

    protected open val thumbnail: ImageState? = null

    final override val state
        get() = State(
                id,
                name,
                details,
                children,
                indentation,
                treeNode.expandVisible,
                treeNode.isExpanded,
                checkBoxState,
                imageData?.imageState
        )

    protected open val colorBackground = GroupHolderNode.colorBackground

    data class State(
            val id: Any,
            val name: NameData?,
            val details: Pair<String, Int>?,
            val children: Pair<String, Int>?,
            val indentation: Int,
            val expandVisible: Boolean,
            val isExpanded: Boolean,
            val checkBoxState: CheckBoxState,
            val imageState: ImageState?
    ) : ModelState {

        override fun same(other: ModelState) = (other as State).id == id
    }

    protected fun showImage(rowBigImage: ImageView, taskImage: ImageNode.ImageData) {
        val viewer = StfalconImageViewer.Builder(rowBigImage.context, listOf(taskImage.imageState), MyImageLoader)
                .withTransitionFrom(rowBigImage)
                .withDismissListener { taskImage.onDismiss() }
                .show()

        taskImage.onImageShown(viewer)
    }

    final override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, startingDrag: Boolean) {
        val groupHolder = viewHolder as NodeHolder

        groupHolder.run {
            val taskImage = imageData

            if (taskImage != null) {
                rowContainer.visibility = View.GONE
                rowBigImageLayout!!.visibility = View.VISIBLE

                taskImage.imageState
                        .toImageLoader()
                        .load(rowBigImage!!)

                if (taskImage.showImage)
                    showImage(rowBigImage, taskImage)
            } else {
                rowContainer.visibility = View.VISIBLE
                rowBigImageLayout?.visibility = View.GONE

                val checkBoxState = checkBoxState
                val widthKey = WidthKey(
                        indentation,
                        checkBoxState.visibility == View.GONE,
                        rowContainer.context
                                .resources
                                .configuration
                                .orientation,
                        avatarImage != null,
                        thumbnail != null
                )

                rowContainer.setIndent(indentation)

                val textWidthRelay = textWidths[widthKey]

                val minLines = 1 + (details?.let { 1 } ?: 0) + (children?.let { 1 } ?: 0)
                var remainingLines = TOTAL_LINES - minLines

                fun allocateLines(textViews: List<TextView>) {
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

                        val lines = listOf(wantLines, remainingLines + 1).min()!!

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
                        if (it != null) {
                            visibility = View.VISIBLE
                            text = it.text
                            setTextColor(it.color)

                            if (it.unlimitedLines) {
                                maxLines = Int.MAX_VALUE
                                isSingleLine = false
                            } else {
                                allocateTextViews += this
                            }
                        } else {
                            visibility = View.INVISIBLE

                            setSingleLine()
                        }

                        setTextIsSelectable(textSelectable)
                    }
                }

                rowDetails.run {
                    details.let {
                        if (it != null) {
                            check(name?.unlimitedLines != true)

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
                            check(name?.unlimitedLines != true)

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

                rowExpand.run {
                    visibility = if (treeNode.expandVisible) View.VISIBLE else View.INVISIBLE
                    setImageResource(if (treeNode.isExpanded) R.drawable.ic_expand_less_black_36dp else R.drawable.ic_expand_more_black_36dp)
                }

                rowCheckBoxFrame.visibility = checkBoxState.visibility

                rowCheckBox.isChecked = checkBoxState.checked

                if (avatarImage != null) {
                    rowImage!!.run {
                        visibility = View.VISIBLE
                        loadPhoto(avatarImage!!.value)
                    }
                } else {
                    rowImage?.visibility = View.GONE
                }

                rowMargin.visibility = if (checkBoxState.visibility == View.GONE && avatarImage == null) View.VISIBLE else View.GONE

                itemView.run {
                    setBackgroundColor(if (treeNode.isSelected && !(isPressed && startingDrag)) colorSelected else colorBackground)

                    @SuppressWarnings("TargetApi")
                    foreground = if (ripple && !isPressed) ContextCompat.getDrawable(context, R.drawable.item_background_material) else null
                }
            }

            rowSeparator.visibility = if (treeNode.separatorVisible) View.VISIBLE else View.INVISIBLE
        }
    }

    object MyImageLoader : ImageLoader<ImageState> {

        override fun loadImage(imageView: ImageView?, image: ImageState?) {
            image!!.toImageLoader().load(imageView!!)
        }
    }

    private data class WidthKey(
            val indentation: Int,
            val checkBoxVisible: Boolean,
            val orientation: Int,
            val avatarVisible: Boolean,
            val thumbnailVisible: Boolean
    )

    sealed class CheckBoxState {

        abstract val visibility: Int
        open val checked: Boolean = false

        object Gone : CheckBoxState() {

            override val visibility = View.GONE
        }

        object Invisible : CheckBoxState() {

            override val visibility = View.INVISIBLE
        }

        class Visible(override val checked: Boolean, val listener: () -> Unit) : CheckBoxState() {

            override val visibility = View.VISIBLE

            override fun hashCode() = (if (checked) 1 else 0) + 31 * visibility

            override fun equals(other: Any?): Boolean {
                if (other === null) return false
                if (other === this) return true

                if (other !is Visible)
                    return false

                if (other.checked != checked)
                    return false

                return true
            }
        }
    }
}
