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

    protected open val expand: Pair<Boolean, () -> Unit>? = null

    protected open val checkBoxVisibility = View.GONE

    protected open val checkBoxChecked: Boolean get() = throw UnsupportedOperationException()

    protected open val checkBoxOnClickListener: () -> Unit get() = throw UnsupportedOperationException()

    protected open val separatorVisible = false

    protected open val backgroundColor = Color.TRANSPARENT

    protected open val onClickListener: (() -> Unit)? = null

    protected open fun getOnLongClickListener(viewHolder: RecyclerView.ViewHolder): (() -> Boolean)? = null // todo can be changed to treeNode.etc

    override val itemViewType: Int = GroupListFragment.GroupAdapter.TYPE_GROUP

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
                expand.let {
                    if (it != null) {
                        visibility = View.VISIBLE
                        setImageResource(if (it.first) R.drawable.ic_expand_less_black_36dp else R.drawable.ic_expand_more_black_36dp)
                        setOnClickListener { _ -> it.second() }
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
                        setOnClickListener { checkBoxOnClickListener() }
                    }
                }
            }

            rowSeparator.visibility = if (separatorVisible) View.VISIBLE else View.INVISIBLE

            itemView.run {
                setBackgroundColor(backgroundColor)
                val onLongClickListener = getOnLongClickListener(viewHolder)
                if (onLongClickListener != null)
                    setOnLongClickListener { onLongClickListener() }
                else
                    setOnClickListener(null)

                onClickListener.let {
                    if (it != null)
                        setOnClickListener { it() }
                    else
                        setOnClickListener(null)
                }
            }
        }
    }
}
