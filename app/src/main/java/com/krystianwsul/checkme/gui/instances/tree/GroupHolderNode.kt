package com.krystianwsul.checkme.gui.instances.tree

import android.graphics.Color
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.setIndent

import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.ModelState
import com.krystianwsul.treeadapter.TreeNode

abstract class GroupHolderNode(protected val indentation: Int) : ModelNode {

    companion object {

        private fun getColor(@ColorRes color: Int) = ContextCompat.getColor(MyApplication.instance, color)

        @JvmStatic
        protected val colorPrimary by lazy { getColor(R.color.textPrimary) }

        @JvmStatic
        protected val colorSecondary by lazy { getColor(R.color.textSecondary) }

        @JvmStatic
        protected val colorDisabled by lazy { getColor(R.color.textDisabled) }

        @JvmStatic
        protected val colorSelected by lazy { getColor(R.color.selected) }
    }

    protected abstract val treeNode: TreeNode

    protected abstract val name: Triple<String, Int, Boolean>?

    protected open val details: Pair<String, Int>? = null

    protected open val children: Pair<String, Int>? = null

    protected open val checkBoxVisibility = View.GONE

    protected open val checkBoxChecked = false

    protected open fun checkBoxOnClickListener() = Unit

    protected open fun onLongClickListener(viewHolder: RecyclerView.ViewHolder) = treeNode.onLongClickListener()

    override val itemViewType: Int = GroupListFragment.GroupAdapter.TYPE_GROUP

    final override val state get() = State(id, name, details, children, indentation, treeNode.expandVisible, treeNode.isExpanded, checkBoxVisibility, checkBoxChecked)

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

    final override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        val groupHolder = viewHolder as NodeHolder

        groupHolder.run {
            rowContainer.setIndent(indentation)

            rowName.run {
                name.let {
                    if (it != null) {
                        visibility = View.VISIBLE
                        text = it.first
                        setTextColor(it.second)
                        setSingleLine(it.third)
                    } else {
                        visibility = View.INVISIBLE
                    }
                }
            }

            rowDetails.run {
                details.let {
                    if (it != null) {
                        visibility = View.VISIBLE
                        text = it.first
                        setTextColor(it.second)
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
                    } else {
                        visibility = View.GONE
                    }
                }
            }

            rowExpand.run {
                visibility = if (treeNode.expandVisible) View.VISIBLE else View.INVISIBLE
                setImageResource(if (treeNode.isExpanded) R.drawable.ic_expand_less_black_36dp else R.drawable.ic_expand_more_black_36dp)
                setOnClickListener { treeNode.expandListener() }
            }

            rowCheckBox.run {
                visibility = checkBoxVisibility
                isChecked = checkBoxChecked
                setOnClickListener {
                    setOnClickListener(null)
                    checkBoxOnClickListener()
                }
            }

            rowSeparator.visibility = if (treeNode.separatorVisible) View.VISIBLE else View.INVISIBLE

            itemView.run {
                setBackgroundColor(if (treeNode.isSelected) colorSelected else Color.TRANSPARENT)
                setOnLongClickListener { onLongClickListener(viewHolder) }
                setOnClickListener { treeNode.onClickListener() }
            }
        }
    }
}
