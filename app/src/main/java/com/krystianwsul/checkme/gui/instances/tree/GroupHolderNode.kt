package com.krystianwsul.checkme.gui.instances.tree

import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.setIndent
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
    }

    protected abstract val treeNode: TreeNode

    protected abstract val name: Triple<String, Int, Boolean>?

    protected open val details: Pair<String, Int>? = null

    protected open val children: Pair<String, Int>? = null

    protected open val checkBoxVisibility = View.GONE

    protected open val checkBoxChecked = false

    protected open fun checkBoxOnClickListener() = Unit

    protected open fun onLongClick(viewHolder: RecyclerView.ViewHolder) = treeNode.onLongClick()

    override val itemViewType: Int = GroupListFragment.GroupAdapter.TYPE_GROUP

    protected open val textSelectable = false

    open val ripple = false

    final override val state get() = State(id, name, details, children, indentation, treeNode.expandVisible, treeNode.isExpanded, checkBoxVisibility, checkBoxChecked)

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
            val checkboxChecked: Boolean) : ModelState {

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

        var lines = 0

        groupHolder.run {
            rowContainer.setIndent(indentation)

            var nameHasEllipsis: Boolean? = null
            var detailsHasEllipsis: Boolean? = null
            var childrenHasEllipsis: Boolean? = null

            fun ellipsisCallback() {
                if (nameHasEllipsis != null && detailsHasEllipsis != null && childrenHasEllipsis != null) {
                    fun TextView.maxify() {
                        setSingleLine(false)
                        maxLines = TOTAL_LINES - lines + 1
                    }

                    if (lines < TOTAL_LINES) {
                        when {
                            nameHasEllipsis!! -> rowName.maxify()
                            detailsHasEllipsis!! -> rowDetails.maxify()
                            childrenHasEllipsis!! -> rowChildren.maxify()
                        }
                    }
                }
            }

            fun TextView.getEllipsis(action: (Boolean) -> Unit) {
                viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

                    override fun onGlobalLayout() {
                        layout?.let {
                            viewTreeObserver.removeOnGlobalLayoutListener(this)

                            action(it.lineCount > 0 && it.getEllipsisCount(1) > 0)

                            ellipsisCallback()
                        }
                    }
                })
            }

            rowName.run {
                setSingleLine()

                name.let {
                    if (it != null) {
                        visibility = View.VISIBLE
                        text = it.first
                        setTextColor(it.second)
                        setSingleLine(it.third)

                        lines++
                        getEllipsis { nameHasEllipsis = it }
                    } else {
                        visibility = View.INVISIBLE

                        nameHasEllipsis = false
                        ellipsisCallback()
                    }

                    setTextIsSelectable(textSelectable)
                }
            }

            rowDetails.run {
                setSingleLine()

                details.let {
                    if (it != null) {
                        visibility = View.VISIBLE
                        text = it.first
                        setTextColor(it.second)

                        lines++
                        getEllipsis { detailsHasEllipsis = it }
                    } else {
                        visibility = View.GONE

                        detailsHasEllipsis = false
                        ellipsisCallback()
                    }
                }
            }

            rowChildren.run {
                setSingleLine()

                children.let {
                    if (it != null) {
                        visibility = View.VISIBLE
                        text = it.first
                        setTextColor(it.second)

                        lines++
                        getEllipsis { childrenHasEllipsis = it }
                    } else {
                        visibility = View.GONE

                        childrenHasEllipsis = false
                        ellipsisCallback()
                    }
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

    private class StaleTreeNodeException : Exception()
}
