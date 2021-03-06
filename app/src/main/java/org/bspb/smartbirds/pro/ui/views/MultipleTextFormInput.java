package org.bspb.smartbirds.pro.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.MultiAutoCompleteTextView;

import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView;

import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.SmartBirdsApplication;
import org.bspb.smartbirds.pro.ui.exception.ViewValidationException;

/**
 * Created by groupsky on 14-10-17.
 */
public class MultipleTextFormInput extends AppCompatMultiAutoCompleteTextView implements SupportRequiredView {

    protected final String TAG = SmartBirdsApplication.TAG + '.' + getClass().getSimpleName();

    private boolean mRequired;

    public MultipleTextFormInput(Context context) {
        this(context, null);
    }

    public MultipleTextFormInput(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.multipleTextFormInputStyle);
    }

    public MultipleTextFormInput(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SupportRequiredView, defStyle, 0);
        try {
            mRequired = a.getBoolean(R.styleable.SupportRequiredView_required, false);
        } finally {
            a.recycle();
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(getClass().getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(getClass().getName());
    }

    @Override
    public void checkRequired() throws ViewValidationException {
        if (mRequired && isEnabled()) {
            if (TextUtils.isEmpty(getText())) {
                setError(getContext().getString(R.string.required_field));
                throw new ViewValidationException();
            }
        } else {
            setError(null);
        }
    }

    public boolean isRequired() {
        return mRequired;
    }

    public void setRequired(boolean required) {
        this.mRequired = required;
    }
}
