package com.krystianwsul.checkme.gui.tree.delegates.multiline

import android.text.Layout
import android.text.StaticLayout
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.gui.tree.NodeDelegate
import io.reactivex.rxjava3.kotlin.addTo

class MultiLineDelegate(private val modelNode: MultiLineModelNode) : NodeDelegate {

    companion object {

        const val TOTAL_LINES = 3

        private val textWidths = InitMap<Pair<Int, WidthKey>, BehaviorRelay<Int>> { BehaviorRelay.create() }
    }

    override val state get() = modelNode.run { State(rows) }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        (viewHolder as MultiLineHolder).apply {
            val widthKey = Pair(
                    rowName.context
                            .resources
                            .configuration
                            .orientation,
                    modelNode.widthKey,
            )

            val textWidthRelay = textWidths[widthKey]

            val minLines = modelNode.run { 1 + (details?.let { 1 } ?: 0) + (children?.let { 1 } ?: 0) }

            fun allocateLines(textViews: List<TextView>) {
                var remainingLines = TOTAL_LINES - minLines

                textViews.forEach { textView ->
                    fun getWantLines(text: String): Int {
                        return if (textWidthRelay.value != null) {
                            StaticLayout.Builder
                                    .obtain(text, 0, text.length, textView.paint, textWidthRelay.value!!)
                                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                    .setLineSpacing(textView.lineSpacingExtra, textView.lineSpacingMultiplier)
                                    .setIncludePad(textView.includeFontPadding)
                                    .build()
                                    .lineCount
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

                    // ellipsize is stupid when measured width is close to max width
                    textView.ellipsize = if (lines == wantLines) null else TextUtils.TruncateAt.END
                }
            }

            val allocateTextViews = mutableListOf<TextView>()

            fun TextView.displayRow(row: MultiLineRow?) {
                when (row) {
                    is MultiLineRow.Visible -> {
                        visibility = View.VISIBLE
                        text = row.text
                        setTextColor(ContextCompat.getColor(context, row.colorId))

                        allocateTextViews += this
                    }
                    MultiLineRow.Invisible -> {
                        visibility = View.INVISIBLE

                        setSingleLine()
                    }
                    null -> visibility = View.GONE
                }
            }

            val rows = modelNode.rows

            rowName.displayRow(rows.getOrNull(0))
            rowDetails.displayRow(rows.getOrNull(1))
            rowChildren.displayRow(rows.getOrNull(2))

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

    data class State(val rows: List<MultiLineRow>)

    data class WidthKey(
            val indentation: Int,
            val checkboxOrAvatarVisible: Boolean,
            val thumbnailVisible: Boolean,
            val expandVisible: Boolean,
            val dialog: Boolean = false,
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