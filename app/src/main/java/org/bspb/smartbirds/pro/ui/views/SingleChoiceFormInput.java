package org.bspb.smartbirds.pro.ui.views;

import static android.app.Dialog.BUTTON_NEGATIVE;
import static android.app.Dialog.BUTTON_NEUTRAL;
import static android.app.Dialog.BUTTON_POSITIVE;
import static android.text.TextUtils.isEmpty;
import static android.widget.AdapterView.INVALID_POSITION;
import static org.bspb.smartbirds.pro.tools.Reporting.logException;
import static org.bspb.smartbirds.pro.ui.utils.Configuration.ITEM_COUNT_FOR_FILTER;
import static org.bspb.smartbirds.pro.ui.utils.Configuration.MULTIPLE_CHOICE_DELIMITER;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewTreeLifecycleOwner;

import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.backend.dto.Nomenclature;
import org.bspb.smartbirds.pro.events.EEventBus;
import org.bspb.smartbirds.pro.events.NomenclaturesReadyEvent;
import org.bspb.smartbirds.pro.tools.AlphanumComparator;
import org.bspb.smartbirds.pro.ui.utils.SmartArrayAdapter;
import org.bspb.smartbirds.pro.utils.NomenclaturesManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by groupsky on 14-10-10.
 */
public class SingleChoiceFormInput extends TextViewFormInput implements SupportStorage, SupportReadOnly {

    private CharSequence key;
    private boolean mReadOnly = false;

    NomenclaturesManager nomenclatures = NomenclaturesManager.Companion.getInstance();

    EEventBus bus = EEventBus.getInstance();

    private SmartArrayAdapter<NomenclatureItem> mAdapter;
    /**
     * The currently selected item.
     */
    NomenclatureItem mSelectedItem = null;

    OnSelectionChangeListener onSelectionChangeListener;
    private boolean settingSelection;

    public SingleChoiceFormInput(Context context) {
        this(context, null);
    }

    public SingleChoiceFormInput(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.singleChoiceFormInputStyle);
    }

    public SingleChoiceFormInput(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SingleChoiceFormInput, defStyle, 0);
        try {
            key = a.getText(R.styleable.SingleChoiceFormInput_entriesType);
            boolean readonly = a.getBoolean(R.styleable.SingleChoiceFormInput_readonly, false);
            SmartArrayAdapter<NomenclatureItem> adapter = new SmartArrayAdapter<>(context,
                    R.layout.item_dialog_single_choice, new ArrayList<NomenclatureItem>());
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            setAdapter(adapter);

            // Apply readonly state if specified in XML
            if (readonly) {
                setReadOnly(true);
            }
        } finally {
            a.recycle();
        }

        loadData();
    }

    public void setKey(CharSequence key) {
        this.key = key;
        loadData();
    }

    public void onEventMainThread(NomenclaturesReadyEvent e) {
        Log.d(TAG, "nomenclatures loaded, loading data...");
        loadData();
        setText(getText());
        try {
            bus.unregister(this);
        } catch (Throwable t) {
            logException(t);
        }
    }

    void loadData() {
        if (key != null && !isInEditMode()) {
            if (nomenclatures.isLoading()) {
                Log.d(TAG, "nomenclatures not loaded, waiting to load...");
                try {
                    bus.registerSticky(this);
                } catch (Throwable t) {
                    logException(t);
                }
                return;
            }

            List<Nomenclature> values = nomenclatures.getNomenclature(key.toString());
            mAdapter.clear();
            for (Nomenclature value : values) {
                if (isEmpty(value.localeLabel)) continue;
                mAdapter.add(new NomenclatureItem(value));
            }

            mAdapter.notifyDataSetChanged();
            if (mAdapter.getCount() == 1) {
                setSelection(mAdapter.getItem(0));
            }
        }
    }

    /**
     * The Adapter is used to provide the data which backs this Spinner.
     * It also provides methods to transform spinner items based on their position
     * relative to the selected item.
     *
     * @param adapter The SpinnerAdapter to use for this Spinner
     */
    public void setAdapter(SmartArrayAdapter<NomenclatureItem> adapter) {
        mAdapter = adapter;
        setText(getText());
    }

    public String getSelection() {
        return mSelectedItem != null ? mSelectedItem.getLabel() : null;
    }

    public Nomenclature getSelectedItem() {
        return mSelectedItem != null
                ? mSelectedItem.getNomenclature()
                : null;
    }

    public void setSelection(String label) {
        setSelection(new NomenclatureItem(label));
    }

    public void setSelectionIfAvailable(NomenclatureItem item) {
        if (item == null) {
            setSelection((NomenclatureItem) null);
        } else {
            if (mAdapter == null || mAdapter.getCount() == 0) return;
            int position = mAdapter.getPosition(item);
            if (position != INVALID_POSITION) {
                setSelection(mAdapter.getItem(position));
            } else {
                setSelection((NomenclatureItem) null);
            }
        }
    }

    public void setSelection(NomenclatureItem item) {
        if (mAdapter == null || mAdapter.getCount() == 0) {
            if (!settingSelection) {
                settingSelection = true;
                try {
                    // in some cases item can be null
                    setText(item != null ? item.getLabel() : null);
                } finally {
                    settingSelection = false;
                }
            }
            return;
        }
        // find our item that has nomenclature inside
        if (item != null) {
            int idx = mAdapter.getPosition(item);
            if (idx != INVALID_POSITION) {
                item = mAdapter.getItem(idx);
            } else {
                item = null;
            }
        }

        if (item != null) {
            if (item.equals(mSelectedItem)) return;
        } else if (mSelectedItem == null) return;

        mSelectedItem = item;
        if (item != null) {
            setText(item.getLabel());
            setError(null);
        } else {
            setText("");
        }

        if (onSelectionChangeListener != null) onSelectionChangeListener.onSelectionChange(this);
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        mReadOnly = readOnly;
        if (readOnly) {
            // Prevent interaction with the view
            setFocusable(false);
            setClickable(false);
        } else {
            // Restore normal interaction
            setFocusable(true);
            setClickable(true);
        }
    }

    @Override
    public boolean isReadOnly() {
        return mReadOnly;
    }

    @Override
    public boolean performClick() {
        // Block clicks when readonly
        if (mReadOnly) {
            return false;
        }
        super.performClick();
        new PopupDialog().show();
        return true;
    }

    @Override
    public void serializeToStorage(Map<String, String> storage, String fieldName) {
        storage.put(fieldName, getText().toString().replace("\n", MULTIPLE_CHOICE_DELIMITER));
        String locale = getContext().getString(R.string.locale);
        if (mSelectedItem != null) {
            if (mSelectedItem.getNomenclature().label.hasValue(locale)) {
                storage.put(fieldName + "." + locale, mSelectedItem.getNomenclature().label.get(locale));
            }
            storage.put(fieldName + ".en", mSelectedItem.getNomenclature().label.get("en"));
        } else {
            storage.put(fieldName + "." + locale, "");
            storage.put(fieldName + ".en", "");
        }
    }

    @Override
    public void restoreFromStorage(Map<String, String> storage, String fieldName) {
        String value = storage.get(fieldName);
        setText(value);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (mAdapter != null && text != null) {
            setSelection(new NomenclatureItem(text.toString()));
        }
        super.setText(text, type);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        return new InstanceState(super.onSaveInstanceState(), getText());
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof InstanceState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        InstanceState ss = (InstanceState) state;
        super.onRestoreInstanceState(((InstanceState) state).getSuperState());

        setText(ss.text);
    }

    public OnSelectionChangeListener getOnSelectionChangeListener() {
        return onSelectionChangeListener;
    }

    public void setOnSelectionChangeListener(OnSelectionChangeListener onSelectionChangeListener) {
        this.onSelectionChangeListener = onSelectionChangeListener;
    }

    private class PopupDialog implements DialogInterface.OnClickListener, TextWatcher, DialogInterface.OnCancelListener, Filter.FilterListener, DialogInterface.OnDismissListener {

        private AlertDialog mPopup;
        private NomenclatureItem mLastSelected = null;
        private boolean needFilter;

        private final DataSetObserver datasetObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                if (mPopup == null) return;
                if (mLastSelected != null) return;
                if (mSelectedItem == null) return;
                mPopup.getListView().setItemChecked(mAdapter.getPosition(mSelectedItem), true);
            }
        };

        public void show() {
            loadData();

            needFilter = mAdapter.getCount() >= ITEM_COUNT_FOR_FILTER;

            View searchView = null;
            if (needFilter) {
                LifecycleOwner lifecycleOwner = ViewTreeLifecycleOwner.get(SingleChoiceFormInput.this);
                if (lifecycleOwner != null) {
                    nomenclatures.getRecentNomenclatures(key.toString()).observe(lifecycleOwner, nomenclatures -> {
                        if (!nomenclatures.isEmpty()) {
                            mAdapter.sort((o1, o2) -> {
                                int idx1 = nomenclatures.indexOf(o1.getNomenclature());
                                int idx2 = nomenclatures.indexOf(o2.getNomenclature());
                                if (idx1 >= 0 && idx2 >= 0) return idx1 - idx2;
                                if (idx1 >= 0) return -1;
                                if (idx2 >= 0) return 1;
                                return AlphanumComparator.compareStrings(o1.getLabel(), o2.getLabel());
                            });

                            mAdapter.getFilter().filter(null);
                        }
                    });
                }

                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                searchView = inflater.inflate(R.layout.nomenclature_dialog_filter, null);
                EditText searchEdit = searchView.findViewById(R.id.dialog_search_edit);
                searchEdit.setHint(getHint());
                searchEdit.addTextChangedListener(this);
            }

            setSelection(new NomenclatureItem(getText().toString()));

            mAdapter.registerDataSetObserver(datasetObserver);

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                    .setTitle(getHint())
                    .setSingleChoiceItems(mAdapter, mSelectedItem != null ? mAdapter.getPosition(mSelectedItem) : INVALID_POSITION, this)
                    .setCancelable(true)
                    .setOnCancelListener(this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .setNeutralButton(R.string.clear, this);
            if (needFilter) {
                builder.setCustomTitle(searchView);
            }
            mPopup = builder.create();
            mPopup.setOnDismissListener(this);

            final ListView listView = mPopup.getListView();
            listView.setTextDirection(getTextDirection());
            listView.setTextAlignment(getTextAlignment());

            mPopup.show();

            mPopup.getButton(BUTTON_POSITIVE).setEnabled(mSelectedItem != null);
            if (needFilter) {
                mPopup.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            }
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case BUTTON_NEGATIVE:
                    mPopup.dismiss();
                    ((Filterable) mAdapter).getFilter().filter(null, this);
                    break;
                case BUTTON_POSITIVE:
                    if (mLastSelected == null) return;
                    setSelection(mLastSelected);
                    mPopup.dismiss();
                    ((Filterable) mAdapter).getFilter().filter(null, this);

                    if (needFilter) {
                        nomenclatures.addRecentNomenclature(getSelectedItem());
                    }
                    break;
                case BUTTON_NEUTRAL:
                    setSelection((NomenclatureItem) null);
                    mPopup.dismiss();
                    ((Filterable) mAdapter).getFilter().filter(null, this);
                    break;
                default:
                    mLastSelected = mAdapter.getItem(which);
                    onClick(dialog, BUTTON_POSITIVE);
                    break;
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            ((Filterable) mAdapter).getFilter().filter(s);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            ((Filterable) mAdapter).getFilter().filter(null);
        }

        @Override
        public void onFilterComplete(int count) {
            setSelection(new NomenclatureItem(getText().toString()));
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            // sometimes the observer is not registered
            try {
                mAdapter.unregisterDataSetObserver(datasetObserver);
            } catch (IllegalStateException e) {
                logException(e);
            }

        }
    }

    public static class InstanceState extends BaseSavedState {
        private final String text;

        public InstanceState(Parcelable superState, CharSequence text) {
            this(superState, text != null ? text.toString() : null);
        }

        public InstanceState(Parcelable superState, String text) {
            super(superState);
            this.text = text;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(this.text);
        }

        protected InstanceState(Parcel source, ClassLoader loader) {
            super(source);
            this.text = source.readString();
        }

        public static final ClassLoaderCreator<InstanceState> CREATOR = new ClassLoaderCreator<InstanceState>() {

            @Override
            public InstanceState createFromParcel(Parcel source, ClassLoader loader) {
                return new InstanceState(source, loader);
            }

            @Override
            public InstanceState createFromParcel(Parcel source) {
                return createFromParcel(source, null);
            }

            @Override
            public InstanceState[] newArray(int size) {
                return new InstanceState[size];
            }
        };
    }

    public interface OnSelectionChangeListener {
        void onSelectionChange(SingleChoiceFormInput view);
    }
}
