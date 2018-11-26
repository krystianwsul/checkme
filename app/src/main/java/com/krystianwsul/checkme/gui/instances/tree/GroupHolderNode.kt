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

    protected open val name: Triple<String, Int, Boolean>? = null

    protected open val details: Pair<String, Int>? = null

    protected open val children: Pair<String, Int>? = null

    protected open val expand: Pair<Int, View.OnClickListener>? = null

    protected open val checkBoxVisibility: Int get() = View.INVISIBLE

    protected open val checkBoxChecked: Boolean get() = throw UnsupportedOperationException()

    protected open val checkBoxOnClickListener: View.OnClickListener get() = throw UnsupportedOperationException()

    protected abstract val separatorVisibility: Int

    protected open val backgroundColor = Color.TRANSPARENT

    protected abstract val onClickListener: View.OnClickListener

    override val itemViewType: Int = GroupListFragment.GroupAdapter.TYPE_GROUP

    protected abstract fun getOnLongClickListener(viewHolder: RecyclerView.ViewHolder): View.OnLongClickListener

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        val groupHolder = viewHolder as GroupListFragment.GroupAdapter.GroupHolder

        groupHolder.run {
            groupRowContainer.setIndent(indentation)

            groupRowName.run {
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

            groupRowDetails.run {
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

            groupRowChildren.run {
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

            groupRowExpand.run {
                expand.let {
                    if (it != null) {
                        visibility = View.VISIBLE
                        setImageResource(it.first)
                        setOnClickListener(it.second)
                    } else {
                        visibility = View.INVISIBLE
                    }
                }
            }

            groupRowCheckBox.run {
                checkBoxVisibility.let {
                    visibility = it
                    if (it == View.VISIBLE) {
                        isChecked = checkBoxChecked
                        setOnClickListener(checkBoxOnClickListener)
                    }
                }
            }

            groupRowSeparator.visibility = separatorVisibility

            groupRow.run {
                setBackgroundColor(backgroundColor)
                setOnLongClickListener(getOnLongClickListener(viewHolder))
                setOnClickListener(onClickListener)
            }
        }
    }
}
