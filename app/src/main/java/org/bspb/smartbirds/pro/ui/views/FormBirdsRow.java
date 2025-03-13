package org.bspb.smartbirds.pro.ui.views;

import static android.text.TextUtils.isEmpty;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.ui.utils.FormUtils;

/**
 * Created by groupsky on 15.12.16.
 */

public class FormBirdsRow extends LinearLayout implements SingleChoiceFormInput.OnSelectionChangeListener {

    private OnDeleteListener onDeleteListener;
    private OnPopulatedListener onPopulatedListener;
    private FormUtils.FormModel model;

    protected SingleChoiceFormInput species;
    protected SingleChoiceFormInput countUnits;
    protected SingleChoiceFormInput countType;
    protected PositiveDecimalNumberFormInput count;
    protected DecimalNumberFormInput countMin;
    protected DecimalNumberFormInput countMax;

    private boolean alreadyInflated = false;

    public static FormBirdsRow build(Context context) {
        FormBirdsRow instance = new FormBirdsRow(context);
        instance.onFinishInflate();
        return instance;
    }

    public FormBirdsRow(Context context) {
        super(context);
    }

    public FormBirdsRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FormBirdsRow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        if (!alreadyInflated) {
            alreadyInflated = true;
            inflate(getContext(), R.layout.partial_form_birds_row, this);
            init();
        }
        super.onFinishInflate();
    }

    protected void init() {
        species = findViewById(R.id.field_name);
        countUnits = findViewById(R.id.form_birds_count_units);
        countType = findViewById(R.id.form_birds_count_type);
        count = findViewById(R.id.form_birds_count);
        countMin = findViewById(R.id.form_birds_count_min);
        countMax = findViewById(R.id.form_birds_count_max);

        View btnDelete = findViewById(R.id.btn_delete);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(view -> doOnDelete());
        }

        if (species != null) {
            species.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    doOnSpeciesChange();
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });
        }

        setGravity(Gravity.BOTTOM);
        setOrientation(HORIZONTAL);
        model = FormUtils.traverseForm(this);
        species.setOnSelectionChangeListener(this);
    }

    protected void doOnDelete() {
        if (onDeleteListener != null)
            onDeleteListener.onDelete(this);
    }

    protected void doOnSpeciesChange() {
        if (onPopulatedListener != null && isPopulated())
            onPopulatedListener.onPopulate(this);
    }

    public boolean isPopulated() {
        return !isEmpty(species.getText());
    }

    public OnDeleteListener getOnDeleteListener() {
        return onDeleteListener;
    }

    public void setOnDeleteListener(OnDeleteListener onDeleteListener) {
        this.onDeleteListener = onDeleteListener;
    }

    public OnPopulatedListener getOnPopulatedListener() {
        return onPopulatedListener;
    }

    public void setOnPopulatedListener(OnPopulatedListener onPopulatedListener) {
        this.onPopulatedListener = onPopulatedListener;
    }

    public FormUtils.FormModel getModel() {
        return model;
    }

    @Override
    public void onSelectionChange(SingleChoiceFormInput view) {
        count.requestFocus();
    }

    public interface OnDeleteListener {
        void onDelete(FormBirdsRow row);
    }

    public interface OnPopulatedListener {
        void onPopulate(FormBirdsRow row);
    }
}
