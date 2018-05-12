package com.krystianwsul.checkme.gui.instances.tree

import android.support.v7.widget.RecyclerView
import android.view.View

import com.krystianwsul.treeadapter.ModelNode

abstract class GroupHolderNode(protected val density: Float, protected val indentation: Int) : ModelNode {

    protected open val name: Triple<String, Int, Boolean>? = null

    protected open val details: Pair<String, Int>? = null

    protected open val children: Pair<String, Int>? = null

    protected open val expand: Pair<Int, View.OnClickListener>? = null

    protected abstract val checkBoxVisibility: Int

    protected abstract val checkBoxChecked: Boolean

    protected abstract val checkBoxOnClickListener: View.OnClickListener

    protected abstract val separatorVisibility: Int

    protected abstract val backgroundColor: Int

    protected abstract val onClickListener: View.OnClickListener?

    override val itemViewType: Int = GroupListFragment.GroupAdapter.TYPE_GROUP

    protected abstract fun getOnLongClickListener(viewHolder: RecyclerView.ViewHolder): View.OnLongClickListener?

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        val groupHolder = viewHolder as GroupListFragment.GroupAdapter.GroupHolder

        val padding = 48 * indentation

        groupHolder.run {
            groupRowContainer.setPadding((padding * density + 0.5f).toInt(), 0, 0, 0)

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
