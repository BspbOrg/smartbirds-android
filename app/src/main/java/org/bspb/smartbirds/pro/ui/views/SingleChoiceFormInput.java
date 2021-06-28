package org.bspb.smartbirds.pro.ui.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ListView;

import androidx.annotation.VisibleForTesting;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EView;
import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.backend.dto.Nomenclature;
import org.bspb.smartbirds.pro.events.EEventBus;
import org.bspb.smartbirds.pro.events.NomenclaturesReadyEvent;
import org.bspb.smartbirds.pro.tools.AlphanumComparator;
import org.bspb.smartbirds.pro.ui.utils.Configuration;
import org.bspb.smartbirds.pro.ui.utils.NomenclaturesBean;
import org.bspb.smartbirds.pro.ui.utils.SmartArrayAdapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.app.Dialog.BUTTON_NEGATIVE;
import static android.app.Dialog.BUTTON_NEUTRAL;
import static android.app.Dialog.BUTTON_POSITIVE;
import static android.text.TextUtils.isEmpty;
import static android.text.TextUtils.join;
import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;
import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI;
import static android.view.inputmethod.EditorInfo.TYPE_TEXT_VARIATION_FILTER;
import static android.widget.AdapterView.INVALID_POSITION;
import static org.bspb.smartbirds.pro.tools.Reporting.logException;
import static org.bspb.smartbirds.pro.ui.utils.Configuration.ITEM_COUNT_FOR_FILTER;
import static org.bspb.smartbirds.pro.ui.utils.Configuration.MULTIPLE_CHOICE_DELIMITER;
import static org.bspb.smartbirds.pro.ui.utils.Configuration.MULTIPLE_CHOICE_SPLITTER;

/**
 * Created by groupsky on 14-10-10.
 */
@EView
public class SingleChoiceFormInput extends TextViewFormInput implements SupportStorage {

    private CharSequence key;

    @Bean
    NomenclaturesBean nomenclatures;
    @Bean
    EEventBus bus;

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
            SmartArrayAdapter<NomenclatureItem> adapter = new SmartArrayAdapter<>(context,
                    R.layout.item_dialog_single_choice, new ArrayList<NomenclatureItem>());
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            setAdapter(adapter);
        } finally {
            a.recycle();
        }
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

    @AfterInject
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
    public boolean performClick() {
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
                mPopup.getListView().setSelection(mAdapter.getPosition(mSelectedItem));
            }
        };

        public void show() {
            loadData();

            needFilter = mAdapter.getCount() >= ITEM_COUNT_FOR_FILTER;

            View searchView = null;
            if (needFilter) {
                final List<Nomenclature> recentItems = nomenclatures.isLoading() ?
                        new ArrayList<Nomenclature>() :
                        nomenclatures.getRecentNomenclatures(key.toString());
                if (!recentItems.isEmpty()) {
                    mAdapter.sort(new Comparator<NomenclatureItem>() {
                        @Override
                        public int compare(NomenclatureItem o1, NomenclatureItem o2) {
                            int idx1 = recentItems.indexOf(o1.getNomenclature());
                            int idx2 = recentItems.indexOf(o2.getNomenclature());
                            if (idx1 >= 0 && idx2 >= 0) return idx1 - idx2;
                            if (idx1 >= 0) return -1;
                            if (idx2 >= 0) return 1;
                            return AlphanumComparator.compareStrings(o1.getLabel(), o2.getLabel());
                        }
                    });
                }

                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                searchView = inflater.inflate(R.layout.nomenclature_dialog_filter, null);
                EditText searchEdit = searchView.findViewById(R.id.dialog_search_edit);
                searchEdit.setHint(getHint());
                searchEdit.addTextChangedListener(this);
            }

            mAdapter.getFilter().filter(null);

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
