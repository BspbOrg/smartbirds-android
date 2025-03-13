package org.bspb.smartbirds.pro.ui.views;

import static android.text.TextUtils.isEmpty;

import android.content.Context;
import android.text.Editable;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;

import org.bspb.smartbirds.pro.R;

import java.util.regex.Pattern;

/**
 * Input field for numbers that will be summed together.
 * Ime actions Go / Next are replaced with + sign. When a number is chosen then pressing the
 * + sign (action button) will append a plus at the end in the edit field. When the + sign is
 * clicked twice then the sum calculated then entered in the field and
 * the action Go / Next is executed.
 * <p>
 * Created by Ilian Georgiev.
 */
public class SumDecimalNumberFormInput extends PositiveDecimalNumberFormInput {

    private static final String PLUS = "+";
    private static final String PLUS_PATTERN = Pattern.quote(PLUS);

    /**
     * Tracks key event to make a sum after second time action button next is pressed and
     * there was no key event before.
     */
    private boolean keyEventOccurred = false;

    public SumDecimalNumberFormInput(Context context) {
        this(context, null);
    }

    public SumDecimalNumberFormInput(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.sumDecimalNumberFormInputStyle);
    }

    public SumDecimalNumberFormInput(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // we have to manually set here, because the theme specified theme doesn't seem to affect these values
        setImeOptions(EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NO_FULLSCREEN | EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NAVIGATE_NEXT);
        setImeActionLabel(context.getText(R.string.sum_decimal_number_form_input_plus), EditorInfo.IME_ACTION_NEXT);
        setKeyListener(DigitsKeyListener.getInstance("0123456789+"));

        setIncludeZero(false);
    }

    @Override
    public void performValidation() {
        CharSequence text = getText();
        if (isEmpty(text)) return;

        // user validation as a way to trigger calculation
        int sum = 0;
        String[] split = text.toString().split(PLUS_PATTERN);

        for (String number : split) {
            if (!isEmpty(number)) {
                sum += Integer.valueOf(number);
            }
        }

        setText(String.valueOf(sum));

        super.performValidation();
    }

    @Override
    public void onEditorAction(int actionCode) {
        Editable text = getText();
        // handle the action button as adding plus first time
        if (!isEmpty(text) && keyEventOccurred) {
            text.append(PLUS);
            if (getSelectionStart() < text.length())
                setSelection(text.length());

            // The key event is handled then set the flag to false in order
            // to check it when we want to calculate the sum and leave the field
            keyEventOccurred = false;

            return;
        }

        // otherwise as focus next
        super.onEditorAction(EditorInfo.IME_ACTION_NEXT);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        // whenever the text changes reset the state of the action button
        keyEventOccurred = true;
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
    }

}
