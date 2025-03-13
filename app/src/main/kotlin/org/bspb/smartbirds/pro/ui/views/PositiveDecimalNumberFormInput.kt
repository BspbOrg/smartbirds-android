package org.bspb.smartbirds.pro.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import org.bspb.smartbirds.pro.R

open class PositiveDecimalNumberFormInput : TextFormInput {

    var includeZero = true

    constructor(context: Context) : this(context, null)


    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.attr.positiveDecimalNumberFormInputStyle
    )

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        context.withStyledAttributes(
            attrs,
            R.styleable.PositiveDecimalNumberFormInput,
            defStyleAttr,
            0
        ) {
            includeZero = getBoolean(R.styleable.PositiveDecimalNumberFormInput_includeZero, true)
        }
    }

    override fun onTextChanged(
        text: CharSequence?,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int,
    ) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        text?.let {
            if (it.contentEquals("0") && !includeZero) {
                setText("")
            }
        }
    }
}