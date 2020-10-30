package com.krystianwsul.checkme.gui.widgets

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.addOneShotGlobalLayoutListener
import kotlinx.android.synthetic.main.view_my_spinner.view.*

class MySpinner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_my_spinner, this)
    }

    private val text by lazy { mySpinnerText!! }

    private lateinit var items: List<*>

    fun setSelection(position: Int) {
        addOneShotGlobalLayoutListener {
            text.setText(text.adapter.getItem(position).toString())
        }
    }

    fun setItems(items: List<*>) {
        check(items.map { it.toString() }.distinct().size == items.size) // require unique for listener to work

        this.items = items

        text.setAdapter(object : ArrayAdapter<Any>(context, R.layout.cat_exposed_dropdown_popup_item, items) {

            override fun getFilter() = object : Filter() {

                override fun performFiltering(constraint: CharSequence?) = FilterResults().apply {
                    values = items
                    count = items.size
                }

                override fun publishResults(constraint: CharSequence?, results: FilterResults?) = notifyDataSetChanged()
            }
        })
    }

    fun addListener(listener: (Int) -> Unit) {
        addOneShotGlobalLayoutListener {
            text.addTextChangedListener(object : TextWatcher {

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                override fun afterTextChanged(s: Editable?) {
                    val stringValue = text.text.toString()

                    val item = items.single { it.toString() == stringValue }

                    listener(items.indexOf(item))
                }
            })
        }
    }

    fun setDense() = mySpinnerText.updatePadding(top = 0, bottom = 0)

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        mySpinnerLayout.isEnabled = enabled
        mySpinnerText.setTextColor(
            ContextCompat.getColor(
                context,
                if (enabled) R.color.textPrimary else R.color.textDisabled
            )
        )
    }
}