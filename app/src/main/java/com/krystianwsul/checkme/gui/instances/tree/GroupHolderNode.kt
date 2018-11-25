package com.krystianwsul.checkme.gui.instances.tree

import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.view.View

import com.krystianwsul.treeadapter.ModelNode

abstract class GroupHolderNode(protected val density: Float, protected val indentation: Int) : ModelNode {

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
        val groupHolder = viewHolder as NodeHolder

        val padding = 48 * indentation

        groupHolder.run {
            rowContainer.setPadding((padding * density + 0.5f).toInt(), 0, 0, 0)

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

            rowCheckBox.run {
                checkBoxVisibility.let {
                    visibility = it
                    if (it == View.VISIBLE) {
                        isChecked = checkBoxChecked
                        setOnClickListener(checkBoxOnClickListener)
                    }
                }
            }

            rowSeparator.visibility = separatorVisibility

            itemView.run {
                setBackgroundColor(backgroundColor)
                setOnLongClickListener(getOnLongClickListener(viewHolder))
                setOnClickListener(onClickListener)
            }
        }
    }
}
