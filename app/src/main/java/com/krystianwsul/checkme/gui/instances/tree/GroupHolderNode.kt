package com.krystianwsul.checkme.gui.instances.tree

import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.*
import com.krystianwsul.checkme.upload.Uploader
import com.krystianwsul.checkme.utils.loadPhoto
import com.krystianwsul.checkme.utils.setIndent
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.ModelState
import com.krystianwsul.treeadapter.TreeNode


abstract class GroupHolderNode(protected val indentation: Int) : ModelNode {

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

    protected abstract val treeNode: TreeNode

    protected abstract val name: Triple<String, Int, Boolean>?

    protected open val details: Pair<String, Int>? = null

    protected open val children: Pair<String, Int>? = null

    protected open val checkBoxVisibility = View.GONE

    protected open val checkBoxChecked = false

    protected open val image: NullableWrapper<String>? = null

    protected open fun checkBoxOnClickListener() = Unit

    protected open fun onLongClick(viewHolder: RecyclerView.ViewHolder) = treeNode.onLongClick()

    override val itemViewType: Int = GroupListFragment.GroupAdapter.TYPE_GROUP

    protected open val textSelectable = false

    open val ripple = false

    protected open val imageData: ImageNode.Data? = null

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
                imageData)

    protected open val colorBackground = GroupHolderNode.colorBackground

    data class State(
            val id: Any,
            val name: Triple<String, Int, Boolean>?,
            val details: Pair<String, Int>?,
            val children: Pair<String, Int>?,
            val indentation: Int,
            val expandVisible: Boolean,
            val isExpanded: Boolean,
            val checkboxVisibility: Int,
            val checkboxChecked: Boolean,
            val imageData: ImageNode.Data?) : ModelState {

        override fun same(other: ModelState) = (other as State).id == id
    }

    final override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, startingDrag: Boolean) {
        val groupHolder = viewHolder as NodeHolder

        fun checkStale() {
            if (treeNode.treeNodeCollection.stale) {
                if (MyCrashlytics.enabled)
                    throw StaleTreeNodeException()
                else
                    MyCrashlytics.logException(StaleTreeNodeException())
            }
        }

        checkStale()

        groupHolder.run {
            if (imageData != null) {
                rowContainer.visibility = View.GONE
                rowBigImage.visibility = View.VISIBLE

                GlideApp.with(itemView)
                        .load(Uploader.getReference(imageData as ImageNode.Data.Remote)) // todo image local
                        .into(rowBigImage)
            } else {
                rowContainer.visibility = View.VISIBLE
                rowBigImage.visibility = View.GONE

                val checkBoxVisibility = checkBoxVisibility
                val widthKey = Triple(indentation, checkBoxVisibility == View.GONE, rowContainer.orientation)

                rowContainer.setIndent(indentation)

                textWidth = textWidths[widthKey]

                val minLines = 1 + (details?.let { 1 } ?: 0) + (children?.let { 1 } ?: 0)
                var remainingLines = TOTAL_LINES - minLines

                fun TextView.allocateLines() {
                    val wantLines = Rect().run {
                        val currentSize = textSize
                        Log.e("asdf", "lines currentSize $currentSize")

                        Paint().let {
                            it.textSize = currentSize
                            it.getTextBounds(text.toString(), 0, text.length, this)
                        }

                        Log.e("asdf", "lines bounds width " + width())
                        Log.e("asdf", "lines textView width $textWidth")
                        Log.e("asdf", "result lines " + Math.ceil(width().toDouble() / (textWidth
                                ?: 0)).toInt())

                        Math.ceil(width().toDouble() / (textWidth ?: 0)).toInt()
                    }

                    val lines = listOf(wantLines, remainingLines + 1).min()!!

                    remainingLines -= (lines - 1)

                    if (lines == 1) {
                        setSingleLine()
                    } else {
                        setSingleLine(false)
                        maxLines = lines
                    }
                }

                rowName.run {
                    name.let {
                        if (it != null) {
                            visibility = View.VISIBLE
                            text = it.first
                            setTextColor(it.second)

                            allocateLines()
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
                        val width = measuredWidth
                        textWidth = width
                        textWidths[widthKey] = width
                    }
                }

                rowExpand.run {
                    visibility = if (treeNode.expandVisible) View.VISIBLE else View.INVISIBLE
                    setImageResource(if (treeNode.isExpanded) R.drawable.ic_expand_less_black_36dp else R.drawable.ic_expand_more_black_36dp)

                    setOnClickListener {
                        checkStale()

                        Preferences.logLineHour("expanding node " + this@GroupHolderNode)
                        treeNode.onExpandClick()
                    }
                }

                rowCheckBox.run {
                    visibility = checkBoxVisibility
                    isChecked = checkBoxChecked

                    setOnClickListener {
                        checkStale()

                        setOnClickListener(null)
                        checkBoxOnClickListener()
                    }
                }

                rowImage.run {
                    if (image != null) {
                        visibility = View.VISIBLE

                        loadPhoto(image!!.value)
                    } else {
                        visibility = View.GONE
                    }
                }

                rowMargin.visibility = if (checkBoxVisibility == View.GONE && image == null) View.VISIBLE else View.GONE

                rowSeparator.visibility = if (treeNode.separatorVisible) View.VISIBLE else View.INVISIBLE

                itemView.run {
                    setBackgroundColor(if (treeNode.isSelected && !(isPressed && startingDrag)) colorSelected else colorBackground)

                    setOnLongClickListener {
                        checkStale()

                        onLongClick(viewHolder)
                        true
                    }
                    setOnClickListener {
                        checkStale()

                        Preferences.logLineHour("clicking node " + this@GroupHolderNode)
                        treeNode.onClick()
                    }

                    @SuppressWarnings("TargetApi")
                    foreground = if (ripple && !isPressed) ContextCompat.getDrawable(context, R.drawable.item_background_material) else null
                }
            }
        }
    }

    private class StaleTreeNodeException : Exception()
}
