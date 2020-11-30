package com.krystianwsul.checkme.gui.tree.multiline

import android.graphics.Rect
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.gui.tree.NodeDelegate
import io.reactivex.rxkotlin.addTo
import kotlin.math.ceil

class MultiLineDelegate(private val modelNode: MultiLineModelNode) : NodeDelegate {

    companion object {

        const val TOTAL_LINES = 3

        private val textWidths = InitMap<Pair<Int, WidthKey>, BehaviorRelay<Int>> { BehaviorRelay.create() }
    }

    override val state get() = modelNode.run { State(name, details, children) }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        (viewHolder as MultiLineHolder).apply {
            val widthKey = Pair(
                    rowName.context
                            .resources
                            .configuration
                            .orientation,
                    modelNode.widthKey
            )

            val textWidthRelay = textWidths[widthKey]

            val minLines = modelNode.run { 1 + (details?.let { 1 } ?: 0) + (children?.let { 1 } ?: 0) }

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
                modelNode.name.let {
                    when (it) {
                        is MultiLineNameData.Visible -> {
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
                        MultiLineNameData.Invisible -> {
                            visibility = View.INVISIBLE

                            setSingleLine()
                        }
                        MultiLineNameData.Gone -> visibility = View.GONE
                    }

                    setTextIsSelectable(modelNode.textSelectable)
                }
            }

            rowDetails.run {
                modelNode.details.let {
                    if (it != null) {
                        check(!modelNode.name.unlimitedLines)

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
                modelNode.children.let {
                    if (it != null) {
                        check(!modelNode.name.unlimitedLines)

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
                        .addTo(compositeDisposable)
            }

            rowTextLayout.apply {
                viewTreeObserver.addOnGlobalLayoutListener {
                    textWidths[widthKey].accept(measuredWidth)
                }
            }
        }
    }

    data class State(val name: MultiLineNameData, val details: Pair<String, Int>?, val children: Pair<String, Int>?)

    data class WidthKey(
            val indentation: Int,
            val checkBoxVisible: Boolean,
            val avatarVisible: Boolean,
            val thumbnailVisible: Boolean,
    )

    private class InitMap<T, U>(private val initializer: (T) -> U) {

        private val map = mutableMapOf<T, U>()

        operator fun get(key: T): U {
            if (!map.containsKey(key))
                map[key] = initializer(key)
            return map.getValue(key)
        }
    }
}