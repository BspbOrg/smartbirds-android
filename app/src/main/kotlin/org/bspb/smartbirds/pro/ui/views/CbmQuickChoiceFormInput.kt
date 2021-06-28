package org.bspb.smartbirds.pro.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.prefs.CbmPrefs_
import org.bspb.smartbirds.pro.ui.utils.NomenclaturesBean
import org.bspb.smartbirds.pro.ui.utils.NomenclaturesBean_
import org.bspb.smartbirds.pro.utils.debugLog

class CbmQuickChoiceFormInput : FrameLayout {

    var onItemSelected: ((item: NomenclatureItem?) -> Unit)? = null

    private var entriesKey: CharSequence
    private var hint: CharSequence?

    private lateinit var hintView: TextView
    private lateinit var btnQuick1: Button
    private lateinit var btnQuick2: Button
    private lateinit var btnQuick3: Button
    private lateinit var btnQuick4: Button
    private lateinit var btnQuick5: Button
    private lateinit var btnQuick6: Button

    private lateinit var nomenclatures: NomenclaturesBean
    private lateinit var prefs: CbmPrefs_
    private val currentValues = mutableMapOf<Int, NomenclatureItem?>()

    private val quickButtonsClickListener = object : OnClickListener {
        override fun onClick(v: View?) {
            v ?: return

            val value = currentValues[v.id]
            if (value != null) {
                onItemSelected?.invoke(value)
            } else {
                openChooser(v)
            }
        }
    }

    private val quickButtonsLongClickListener = object : OnLongClickListener {
        override fun onLongClick(v: View?): Boolean {
            v ?: return false
            openChooser(v)
            return true
        }
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.attr.quickChoiceInputStyle
    )

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : this(
        context,
        attrs,
        defStyle,
        0
    )

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyle,
        defStyleRes
    ) {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.QuickChoiceFormInput,
            defStyle,
            defStyleRes
        )
        try {
            hint = a.getText(R.styleable.QuickChoiceFormInput_hint)
            entriesKey = a.getText(R.styleable.QuickChoiceFormInput_entriesType)
        } finally {
            a.recycle()
        }

        initView()
    }

    private fun initView() {
        inflate(context, R.layout.cbm_input_quick_choice, this)

        nomenclatures = NomenclaturesBean_.getInstance_(context)
        prefs = CbmPrefs_(context)

        loadValues()

        hintView = findViewById(R.id.quick_choice_hint)

        btnQuick1 = findViewById(R.id.quick_1)
        btnQuick2 = findViewById(R.id.quick_2)
        btnQuick3 = findViewById(R.id.quick_3)
        btnQuick4 = findViewById(R.id.quick_4)
        btnQuick5 = findViewById(R.id.quick_5)
        btnQuick6 = findViewById(R.id.quick_6)

        btnQuick1.setOnClickListener(quickButtonsClickListener)
        btnQuick2.setOnClickListener(quickButtonsClickListener)
        btnQuick3.setOnClickListener(quickButtonsClickListener)
        btnQuick4.setOnClickListener(quickButtonsClickListener)
        btnQuick5.setOnClickListener(quickButtonsClickListener)
        btnQuick6.setOnClickListener(quickButtonsClickListener)

        btnQuick1.setOnLongClickListener(quickButtonsLongClickListener)
        btnQuick2.setOnLongClickListener(quickButtonsLongClickListener)
        btnQuick3.setOnLongClickListener(quickButtonsLongClickListener)
        btnQuick4.setOnLongClickListener(quickButtonsLongClickListener)
        btnQuick5.setOnLongClickListener(quickButtonsLongClickListener)
        btnQuick6.setOnLongClickListener(quickButtonsLongClickListener)

        hintView.hint = hint

        updateButtons()
    }

    private fun openChooser(button: View) {
        val chooser = NomenclatureChooser(context, entriesKey as String, hint as String)
        chooser.showPopup { nomenclatureItem ->
            debugLog(nomenclatureItem?.toString())
            setValue(button.id, nomenclatureItem)
        }
    }

    private fun loadValues() {
        var items = nomenclatures.getNomenclature(entriesKey.toString())
        val quick1Pref = prefs.speciesQuick1().get()
        val quick2Pref = prefs.speciesQuick2().get()
        val quick3Pref = prefs.speciesQuick3().get()
        val quick4Pref = prefs.speciesQuick4().get()
        val quick5Pref = prefs.speciesQuick5().get()
        val quick6Pref = prefs.speciesQuick6().get()

        for (item in items) {
            if (item.label.labelId.equals(quick1Pref)) {
                currentValues[R.id.quick_1] = NomenclatureItem(item)
            }

            if (item.label.labelId.equals(quick2Pref)) {
                currentValues[R.id.quick_2] = NomenclatureItem(item)
            }

            if (item.label.labelId.equals(quick3Pref)) {
                currentValues[R.id.quick_3] = NomenclatureItem(item)
            }

            if (item.label.labelId.equals(quick4Pref)) {
                currentValues[R.id.quick_4] = NomenclatureItem(item)
            }

            if (item.label.labelId.equals(quick5Pref)) {
                currentValues[R.id.quick_5] = NomenclatureItem(item)
            }

            if (item.label.labelId.equals(quick6Pref)) {
                currentValues[R.id.quick_6] = NomenclatureItem(item)
            }
        }
    }

    private fun setValue(resourceId: Int, nomenclatureItem: NomenclatureItem?) {
        currentValues[resourceId] = nomenclatureItem
        when (resourceId) {
            R.id.quick_1 -> prefs.speciesQuick1()
                .put(nomenclatureItem?.nomenclature?.label?.labelId)
            R.id.quick_2 -> prefs.speciesQuick2()
                .put(nomenclatureItem?.nomenclature?.label?.labelId)
            R.id.quick_3 -> prefs.speciesQuick3()
                .put(nomenclatureItem?.nomenclature?.label?.labelId)
            R.id.quick_4 -> prefs.speciesQuick4()
                .put(nomenclatureItem?.nomenclature?.label?.labelId)
            R.id.quick_5 -> prefs.speciesQuick5()
                .put(nomenclatureItem?.nomenclature?.label?.labelId)
            R.id.quick_6 -> prefs.speciesQuick6()
                .put(nomenclatureItem?.nomenclature?.label?.labelId)
        }
        updateButtons()
    }

    private fun updateButtons() {
        btnQuick1.text = prepareLabel(currentValues[R.id.quick_1])
        btnQuick2.text = prepareLabel(currentValues[R.id.quick_2])
        btnQuick3.text = prepareLabel(currentValues[R.id.quick_3])
        btnQuick4.text = prepareLabel(currentValues[R.id.quick_4])
        btnQuick5.text = prepareLabel(currentValues[R.id.quick_5])
        btnQuick6.text = prepareLabel(currentValues[R.id.quick_6])
    }

    private fun prepareLabel(item: NomenclatureItem?): String {
        var res = context.getString(R.string.quick_choice_button_empty)

        item?.nomenclature?.apply {
            val rawLabel = if (label.get(context.getString(R.string.locale)).isNotBlank()) {
                label.get(context.getString(R.string.locale))
            } else {
                label.get("la")
            }

            if (rawLabel.isNotBlank()) {
                res = ""
                val words = rawLabel.split("\\s+".toRegex())
                for (word in words) {
                    if (res.isNotEmpty()) {
                        res += " "
                    }
                    res += if (word.length > 3) {
                        word.substring(0, 3)
                    } else {
                        word
                    }
                }
            } else {
                res = context.getString(R.string.quick_choice_button_empty)
            }
        }

        return res
    }
}