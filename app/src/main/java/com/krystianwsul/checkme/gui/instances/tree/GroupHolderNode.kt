package com.krystianwsul.checkme.gui.instances.tree

import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.firebase.models.ImageState
import com.krystianwsul.checkme.utils.loadPhoto
import com.krystianwsul.checkme.utils.setIndent
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.ModelState
import com.krystianwsul.treeadapter.TreeNode
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.loader.ImageLoader
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

        val textWidths = mutableMapOf<Triple<Int, Boolean, Int>, Int>()
    }

    protected abstract val treeNode: TreeNode<NodeHolder>

    protected abstract val name: NameData?

    protected open val details: Pair<String, Int>? = null

    protected open val children: Pair<String, Int>? = null

    protected open val checkBoxVisibility = View.GONE

    protected open val checkBoxChecked = false

    protected open val avatarImage: NullableWrapper<String>? = null

    open fun checkBoxOnClickListener() = Unit

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
                checkBoxVisibility,
                checkBoxChecked,
                imageData?.imageState)

    protected open val colorBackground = GroupHolderNode.colorBackground

    data class State(
            val id: Any,
            val name: NameData?,
            val details: Pair<String, Int>?,
            val children: Pair<String, Int>?,
            val indentation: Int,
            val expandVisible: Boolean,
            val isExpanded: Boolean,
            val checkboxVisibility: Int,
            val checkboxChecked: Boolean,
            val imageState: ImageState?) : ModelState {

        override fun same(other: ModelState) = (other as State).id == id
    }

    private fun checkStale() {
        if (treeNode.treeNodeCollection.stale) {
            if (MyCrashlytics.enabled)
                MyCrashlytics.logException(StaleTreeNodeException())
            else
                throw StaleTreeNodeException()
        }
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

        checkStale()

        groupHolder.run {
            val taskImage = imageData

            if (taskImage != null) {
                rowContainer.visibility = View.GONE
                rowBigImageLayout!!.visibility = View.VISIBLE

                taskImage.imageState.load(rowBigImage!!)

                if (taskImage.showImage)
                    showImage(rowBigImage, taskImage)
            } else {
                rowContainer.visibility = View.VISIBLE
                rowBigImageLayout?.visibility = View.GONE

                val checkBoxVisibility = checkBoxVisibility
                val widthKey = Triple(indentation, checkBoxVisibility == View.GONE, rowContainer.orientation)

                rowContainer.setIndent(indentation)

                val textWidth = textWidths[widthKey]

                val minLines = 1 + (details?.let { 1 } ?: 0) + (children?.let { 1 } ?: 0)
                var remainingLines = TOTAL_LINES - minLines

                fun TextView.allocateLines() {
                    fun getWantLines(text: String) = Rect().run {
                        if (textWidth != null) {
                            val currentSize = textSize

                            Paint().let {
                                it.textSize = currentSize
                                it.getTextBounds(text, 0, text.length, this)
                            }

                            ceil(width().toDouble() / textWidth).toInt()
                        } else {
                            1
                        }
                    }

                    val wantLines = text.toString()
                            .split('\n')
                            .map {
                                getWantLines(it)
                            }.sum()

                    val lines = listOf(wantLines, remainingLines + 1).min()!!

                    remainingLines -= (lines - 1)

                    if (lines == 1) {
                        setSingleLine()
                    } else {
                        check(lines > 1)

                        isSingleLine = false
                        setLines(lines)
                    }
                }

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
                                allocateLines()
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

                            allocateLines()
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

                            allocateLines()
                        } else {
                            visibility = View.GONE
                        }
                    }
                }

                rowTextLayout.apply {
                    viewTreeObserver.addOnGlobalLayoutListener {
                        textWidths[widthKey] = measuredWidth
                    }
                }

                rowThumbnail.apply {
                    if (thumbnail != null) {
                        visibility = View.VISIBLE

                        thumbnail!!.load(this, true)
                    } else {
                        visibility = View.GONE
                    }
                }

                rowExpand.run {
                    visibility = if (treeNode.expandVisible) View.VISIBLE else View.INVISIBLE
                    setImageResource(if (treeNode.isExpanded) R.drawable.ic_expand_less_black_36dp else R.drawable.ic_expand_more_black_36dp)
                }

                rowCheckBox.run {
                    visibility = checkBoxVisibility
                    isChecked = checkBoxChecked
                }

                if (avatarImage != null) {
                    rowImage!!.run {
                        visibility = View.VISIBLE
                        loadPhoto(avatarImage!!.value)
                    }
                } else {
                    rowImage?.visibility = View.GONE
                }

                rowMargin.visibility = if (checkBoxVisibility == View.GONE && avatarImage == null) View.VISIBLE else View.GONE

                itemView.run {
                    setBackgroundColor(if (treeNode.isSelected && !(isPressed && startingDrag)) colorSelected else colorBackground)

                    @SuppressWarnings("TargetApi")
                    foreground = if (ripple && !isPressed) ContextCompat.getDrawable(context, R.drawable.item_background_material) else null
                }
            }

            rowSeparator.visibility = if (treeNode.separatorVisible) View.VISIBLE else View.INVISIBLE
        }
    }

    class StaleTreeNodeException : Exception()

    object MyImageLoader : ImageLoader<ImageState> {

        override fun loadImage(imageView: ImageView?, image: ImageState?) {
            image!!.load(imageView!!)
        }
    }
}
