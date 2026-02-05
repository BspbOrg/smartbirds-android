package org.bspb.smartbirds.pro.ui;

import static android.text.TextUtils.isEmpty;
import static org.bspb.smartbirds.pro.tools.Reporting.logException;
import static org.bspb.smartbirds.pro.ui.utils.Constants.VIEWTYPE_LIST;
import static org.bspb.smartbirds.pro.ui.utils.Constants.VIEWTYPE_MAP;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.reflect.TypeToken;

import org.bspb.smartbirds.pro.BuildConfig;
import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.SmartBirdsApplication;
import org.bspb.smartbirds.pro.backend.dto.BGAtlasCell;
import org.bspb.smartbirds.pro.backend.dto.MapLayerItem;
import org.bspb.smartbirds.pro.beans.EntriesToMapMarkersConverter;
import org.bspb.smartbirds.pro.collections.IterableConverter;
import org.bspb.smartbirds.pro.content.MonitoringEntry;
import org.bspb.smartbirds.pro.enums.EntryType;
import org.bspb.smartbirds.pro.events.ActiveMonitoringEvent;
import org.bspb.smartbirds.pro.events.CancelMonitoringEvent;
import org.bspb.smartbirds.pro.events.EEventBus;
import org.bspb.smartbirds.pro.events.FinishMonitoringEvent;
import org.bspb.smartbirds.pro.events.LocationChangedEvent;
import org.bspb.smartbirds.pro.events.MapClickedEvent;
import org.bspb.smartbirds.pro.events.MapLongClickedEvent;
import org.bspb.smartbirds.pro.events.MonitoringResumedEvent;
import org.bspb.smartbirds.pro.events.PauseMonitoringEvent;
import org.bspb.smartbirds.pro.events.QueryActiveMonitoringEvent;
import org.bspb.smartbirds.pro.events.ResumeMonitoringEvent;
import org.bspb.smartbirds.pro.events.UndoLastEntry;
import org.bspb.smartbirds.pro.prefs.MonitoringPrefs;
import org.bspb.smartbirds.pro.prefs.SmartBirdsPrefs;
import org.bspb.smartbirds.pro.prefs.UserPrefs;
import org.bspb.smartbirds.pro.service.DataService;
import org.bspb.smartbirds.pro.service.TrackingService;
import org.bspb.smartbirds.pro.tools.SBGsonParser;
import org.bspb.smartbirds.pro.ui.fragment.MonitoringEntryListFragment;
import org.bspb.smartbirds.pro.ui.map.EntryMapMarker;
import org.bspb.smartbirds.pro.ui.map.GoogleMapProvider;
import org.bspb.smartbirds.pro.ui.map.MapProvider;
import org.bspb.smartbirds.pro.ui.map.OsmMapProvider;
import org.bspb.smartbirds.pro.viewmodel.MonitoringViewModel;
import org.osmdroid.config.Configuration;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Created by dani on 14-11-4.
 */
public class MonitoringActivity extends BaseActivity implements MonitoringEntryListFragment.Listener, MapProvider.MarkerClickListener {

    public static Intent newIntent(Context context) {
        return new Intent(context, MonitoringActivity.class);
    }

    private static final String TAG = SmartBirdsApplication.TAG + ".MonitoringActivity";

    private static final int REQUEST_NEW_ENTRY = 1001;

    private static final int REQUEST_FINISH_MONITORING = 1002;

    @NonNull
    MapProvider.ProviderType providerType = MapProvider.ProviderType.GOOGLE;
    @NonNull
    MapProvider.MapType mapType = MapProvider.MapType.NORMAL;

    MapProvider googleMap = new GoogleMapProvider();
    MapProvider osmMap = new OsmMapProvider();

    MapProvider currentMap;

    MenuItem menuNewEntry;
    MenuItem menuUndoEntry;

    EEventBus eventBus = EEventBus.getInstance();

    @NonNull
    ArrayList<LatLng> points = new ArrayList<LatLng>();


    MenuItem menuZoom;
    MenuItem menuZoomFree;
    MenuItem menuZoom1km;
    MenuItem menuZoom500m;
    MenuItem menuZoom250m;
    MenuItem menuZoom100m;
    MenuItem menuCrash;

    // InstanceState fields
    @Nullable
    EntryType entryType = null;
    int zoomFactor = 500;
    LatLng lastPosition;
    String monitoringCode;

    MonitoringPrefs monitoringPrefs;
    SmartBirdsPrefs prefs;
    UserPrefs userPrefs;

    private boolean canceled = false;
    boolean stayAwake;
    boolean showZoneBackground;
    boolean showLocalProjects;
    boolean showBgAtlasCells;
    boolean showKml;

    boolean showCurrentLocationCircle;
    List<MapLayerItem> mapLayers;

    MonitoringEntryListFragment listFragment;

    View mapContainer;
    View listContainer;

    EntriesToMapMarkersConverter mapMarkerConverter = EntriesToMapMarkersConverter.Companion.getInstance();

    private Set<String> formsEnabled;
    private Menu menu;
    private MonitoringViewModel viewModel;
    private List<MonitoringEntry> monitoringEntries = new ArrayList<>();

    private boolean stateRestored = false;

    private final ActivityResultLauncher<Intent> editCurrentCommonFormActivityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        onFinishConfirm(result.getResultCode(), result.getData());
    });

    private final ActivityResultLauncher<Intent> newEntryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        onNewEntry(result.getResultCode(), result.getData());
    });

    private ServiceConnection trackingServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, String.format(Locale.ENGLISH, "service %s connected", name));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, String.format(Locale.ENGLISH, "service %s disconnected", name));
        }
    };

    private ServiceConnection dataServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, String.format(Locale.ENGLISH, "service %s connected", name));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, String.format(Locale.ENGLISH, "service %s disconnected", name));
        }
    };

    protected void initProviders() {
        googleMap.setFragmentManager(getSupportFragmentManager());
        osmMap.setFragmentManager(getSupportFragmentManager());
        osmMap.setLifeCycle(getLifecycle());
    }

    protected void fetchMonitoringCode() {
        eventBus.postSticky(new QueryActiveMonitoringEvent());
    }

    void init() {
        monitoringPrefs = new MonitoringPrefs(this);
        prefs = new SmartBirdsPrefs(this);
        userPrefs = new UserPrefs(this);

        restoreSavedInstanceState(getIntent().getExtras());
        listFragment = (MonitoringEntryListFragment) getSupportFragmentManager().findFragmentById(R.id.list_container);
        // bind views
        mapContainer = findViewById(R.id.root_map_container);
        listContainer = findViewById(R.id.list_container);

        findViewById(R.id.fab).setOnClickListener(v -> onNewEntry());

        initProviders();
        fetchMonitoringCode();
        setupList();
    }

    private void setupList() {
        if (isEmpty(monitoringCode)) return;
        if (listFragment == null) {
            listFragment = MonitoringEntryListFragment.Companion.newInstance(monitoringCode);
            getSupportFragmentManager().beginTransaction().replace(R.id.list_container, listFragment).commit();
        } else {
            listFragment.setMonitoringCode(monitoringCode);
        }
        initViewModel();
    }

    private void updateViewType() {
        String viewType = prefs.getMonitoringViewType();
        mapContainer.setVisibility(!VIEWTYPE_LIST.equals(viewType) ? View.VISIBLE : View.GONE);
        if (getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT && !VIEWTYPE_LIST.equals(viewType)) {
            listContainer.setVisibility(View.GONE);
        } else {
            listContainer.setVisibility(!VIEWTYPE_MAP.equals(viewType) ? View.VISIBLE : View.GONE);
        }

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_monitoring);

        init();

        try {
            bindService(DataService.Companion.intent(this), trackingServiceConnection, BIND_AUTO_CREATE);
            bindService(new Intent(this, TrackingService.class), dataServiceConnection, BIND_ABOVE_CLIENT);
        } catch (Throwable t) {
            logException(t);
        }
        initViewModel();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                pauseMonitoring();
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("zoomFactor", zoomFactor);
        bundle.putParcelable("lastPosition", lastPosition);
        bundle.putSerializable("entryType", entryType);
        bundle.putString("monitoringCode", monitoringCode);
    }

    private void restoreSavedInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        zoomFactor = savedInstanceState.getInt("zoomFactor");
        lastPosition = savedInstanceState.getParcelable("lastPosition");
        entryType = ((EntryType) savedInstanceState.getSerializable("entryType"));
        monitoringCode = savedInstanceState.getString("monitoringCode");
    }

    private void initViewModel() {
        if (TextUtils.isEmpty(monitoringCode)) {
            return;
        }
        viewModel = new ViewModelProvider(this).get(MonitoringViewModel.class);
        viewModel.init(monitoringCode);
        viewModel.getEntries().observe(this, entries -> {
            monitoringEntries.clear();
            monitoringEntries.addAll(entries);
            if (currentMap != null) currentMap.setMarkers(getMarkers());
        });
        loadZones();
    }

    private void loadZones() {
        viewModel.getZones().observe(this, (zones) -> {
            if (currentMap != null) currentMap.setZones(zones);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreState();
        showCurrentMap();
        updateViewType();
        currentMap.setUpMapIfNeeded();
    }

    @Override
    protected void onStart() {
        super.onStart();
        eventBus.registerSticky(this);
        eventBus.register(osmMap);
        eventBus.register(googleMap);
    }

    @Override
    protected void onStop() {
        super.onStop();
        eventBus.unregister(this);
        eventBus.unregister(osmMap);
        eventBus.unregister(googleMap);
    }

    @Override
    protected void onDestroy() {
        try {
            unbindService(trackingServiceConnection);
            unbindService(dataServiceConnection);
        } catch (Throwable t) {
            logException(t);
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.monitoring, menu);
        menuInflater.inflate(R.menu.debug_menu, menu);
        this.menuNewEntry = menu.findItem(R.id.action_new_entry);
        this.menuUndoEntry = menu.findItem(R.id.action_undo_last_entry);
        this.menuZoom = menu.findItem(R.id.menu_zoom);
        this.menuZoomFree = menu.findItem(R.id.action_zoom_free);
        this.menuZoom1km = menu.findItem(R.id.action_zoom_1km);
        this.menuZoom500m = menu.findItem(R.id.action_zoom_500m);
        this.menuZoom250m = menu.findItem(R.id.action_zoom_250m);
        this.menuZoom100m = menu.findItem(R.id.action_zoom_100m);
        this.menuCrash = menu.findItem(R.id.action_crash);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        this.menu = menu;
        menuCrash.setVisible(BuildConfig.DEBUG);
        switch (zoomFactor) {
            case 1000:
                menuZoom.setTitle(R.string.menu_monitoring_zoom_1000);
                menuZoom1km.setChecked(true);
                break;
            case 500:
                menuZoom.setTitle(R.string.menu_monitoring_zoom_500);
                menuZoom500m.setChecked(true);
                break;
            case 250:
                menuZoom.setTitle(R.string.menu_monitoring_zoom_250);
                menuZoom250m.setChecked(true);
                break;
            case 100:
                menuZoom.setTitle(R.string.menu_monitoring_zoom_100);
                menuZoom100m.setChecked(true);
                break;
            case -1:
                menuZoom.setTitle(R.string.menu_monitoring_zoom_free);
                menuZoomFree.setChecked(true);
                break;
        }

        updateTypeOfObservationMenus(menu);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_new_entry) {
            onNewEntry();
            return true;
        }
        if (itemId == R.id.action_common_form) {
            onCommonForm();
            return true;
        }
        if (itemId == R.id.menu_settings) {
            openSettings();
            return true;
        }
        if (itemId == R.id.action_finish) {
            onFinish();
            return true;
        }
        if (itemId == android.R.id.home) {
            onUp();
            return true;
        }
        if (itemId == R.id.action_zoom_1km) {
            setZoom1km(item);
            return true;
        }
        if (itemId == R.id.action_zoom_500m) {
            setZoom500m(item);
            return true;
        }
        if (itemId == R.id.action_zoom_250m) {
            setZoom250m(item);
            return true;
        }
        if (itemId == R.id.action_zoom_100m) {
            setZoom100m(item);
            return true;
        }
        if (itemId == R.id.action_zoom_free) {
            setZoomFree(item);
            return true;
        }
        if (
                (itemId == R.id.action_form_type_birds) ||
                        (itemId == R.id.action_form_type_cbm) ||
                        (itemId == R.id.action_form_type_ciconia) ||
                        (itemId == R.id.action_form_type_humid) ||
                        (itemId == R.id.action_form_type_herptile) ||
                        (itemId == R.id.action_form_type_mammal) ||
                        (itemId == R.id.action_form_type_invertebrates) ||
                        (itemId == R.id.action_form_type_plants) ||
                        (itemId == R.id.action_form_type_threats) ||
                        (itemId == R.id.action_form_type_pylons) ||
                        (itemId == R.id.action_form_type_pylons_casualties) ||
                        (itemId == R.id.action_form_type_birds_migrations) ||
                        (itemId == R.id.action_form_type_fish) ||
                        (itemId == R.id.action_form_type_bats) ||
                        (itemId == R.id.action_form_type_bears)
        ) {
            setFormType(item);
            return true;
        }
        if (itemId == R.id.action_undo_last_entry) {
            undoLastEntry();
            return true;
        }
        if (itemId == R.id.action_crash) {
            crash();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateTypeOfObservationMenus(Menu menu) {
        if (menu == null) {
            return;
        }

        // hide all types
        for (EntryType form : EntryType.values()) {
            MenuItem menuItem = menu.findItem(form.menuActionId);
            if (menuItem != null) {
                menuItem.setVisible(false);
            }
        }

        // show enabled types
        if (formsEnabled != null) {
            for (String formName : formsEnabled) {
                try {
                    EntryType form = EntryType.valueOf(formName);
                    MenuItem item = menu.findItem(form.menuActionId);
                    if (item != null) {
                        item.setVisible(true);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        if (entryType == null) return;
        MenuItem item = menu.findItem(entryType.menuActionId);
        if (item == null) {
            throw new IllegalStateException("Unhandled entry type: " + entryType);
        }
        item.setChecked(true);
    }

    private void showCurrentMap() {
        switch (providerType) {
            case GOOGLE:
            default:
                if (currentMap != null && currentMap != googleMap) {
                    currentMap.clearPositioned();
                }
                currentMap = googleMap;
                break;
            case OSM:
                if (currentMap != null && currentMap != osmMap) {
                    currentMap.clearPositioned();
                }
                currentMap = osmMap;
                break;
        }
        if (currentMap == null) {
            return;
        }

        currentMap.setPosition(lastPosition);
        currentMap.setZoomFactor(zoomFactor);
        currentMap.setMarkers(getMarkers());
        currentMap.setPath(points);
        currentMap.setMapType(mapType);
        if (viewModel != null && viewModel.getZones().getValue() != null) {
            currentMap.setZones(viewModel.getZones().getValue());
        }
        currentMap.setShowZoneBackground(showZoneBackground);
        currentMap.setShowLocalProjects(showLocalProjects);
        currentMap.setOnMarkerClickListener(this);
        currentMap.showMap();
        currentMap.setBgAtlasCells(readAtlasCells());
        currentMap.setShowBgAtlasCells(showBgAtlasCells);
        currentMap.setShowKml(showKml);
        currentMap.setShowMapLayers(mapLayers);
        currentMap.setShowCurrentLocationCircle(showCurrentLocationCircle);
        currentMap.updateCamera();
    }

    private List<BGAtlasCell> readAtlasCells() {
        Type listType = new TypeToken<List<BGAtlasCell>>() {
        }.getType();
        return SBGsonParser.createParser().fromJson(userPrefs.getBgAtlasCells(), listType);
    }

    void onNewEntry() {
        if (currentMap.getMyLocation() != null) {
            startNewEntryWithoutAsking(new LatLng(currentMap.getMyLocation().getLatitude(), currentMap.getMyLocation().getLongitude()), currentMap.getMyLocation().getAccuracy());
        }
    }

    void onCommonForm() {
        startActivity(EditCurrentCommonFormActivity.intent(this));
    }

    void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    void onNewEntry(int resultCode, Intent data) {
        if (resultCode != RESULT_OK)
            return;
        if (menuUndoEntry != null) {
            menuUndoEntry.setEnabled(true);
        }
    }

    void onFinishConfirm(int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        clearPrefs();
        eventBus.post(new FinishMonitoringEvent());
        finish();
    }

    void onFinish() {
        editCurrentCommonFormActivityLauncher.launch(EditCurrentCommonFormActivity.intent(this, true));
    }

    void onUp() {
        pauseMonitoring();
    }

    void pauseMonitoring() {
        eventBus.post(new PauseMonitoringEvent());
        finish();
    }

    void setZoom1km(MenuItem sender) {
        setZoomFactor(1000);
        sender.setChecked(true);
        menuZoom.setTitle(sender.getTitle());
    }

    void setZoom500m(MenuItem sender) {
        setZoomFactor(500);
        sender.setChecked(true);
        menuZoom.setTitle(sender.getTitle());
    }

    void setZoom250m(MenuItem sender) {
        setZoomFactor(250);
        sender.setChecked(true);
        menuZoom.setTitle(sender.getTitle());
    }

    void setZoom100m(MenuItem sender) {
        setZoomFactor(100);
        sender.setChecked(true);
        menuZoom.setTitle(sender.getTitle());
    }

    void setZoomFree(MenuItem sender) {
        setZoomFactor(-1);
        sender.setChecked(true);
        menuZoom.setTitle(sender.getTitle());
    }

    public void setZoomFactor(int zoomFactor) {
        this.zoomFactor = zoomFactor;
        currentMap.setZoomFactor(zoomFactor);
        currentMap.updateCamera();
        prefs.setZoomFactor(zoomFactor);
    }

    //    private void setShowZoneBackground(boolean showBackground) {
//        this.showZoneBackground = showBackground;
//        if (currentMap != null) {
//            currentMap.setShowZoneBackground(showBackground);
//            currentMap.updateCamera();
//        }
//    }
//
//    private void setShowLocalProjects(boolean showLocalProjects) {
//        this.showLocalProjects = showLocalProjects;
//        if (currentMap != null) {
//            currentMap.setShowLocalProjects(showLocalProjects);
//            currentMap.updateCamera();
//        }
//    }
//
//    private void setShowBgAtlasCells(boolean showBgAtlasCells) {
//        this.showBgAtlasCells = showBgAtlasCells;
//        if (currentMap != null) {
//            currentMap.setShowBgAtlasCells(showBgAtlasCells);
//            currentMap.updateCamera();
//        }
//    }
//
//    private void setShowKml(boolean showKml) {
//        this.showKml = showKml;
//        if (currentMap != null) {
//            currentMap.setShowKml(showKml);
//            currentMap.updateCamera();
//        }
//    }
//
//    private void setShowMapLayers(List<MapLayerItem> mapLayers) {
//        this.mapLayers = mapLayers;
//        if (currentMap != null) {
//            currentMap.setShowMapLayers(mapLayers);
//            currentMap.updateCamera();
//        }
//    }
//
    private void setStayAwake(boolean stayAwake) {
        this.stayAwake = stayAwake;
        if (stayAwake) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
//
//    private void setShowCurrentLocationCircle(boolean showCurrentLocationCircle) {
//        this.showCurrentLocationCircle = showCurrentLocationCircle;
//        if (currentMap != null) {
//            currentMap.setShowCurrentLocationCircle(showCurrentLocationCircle);
//            currentMap.updateCamera();
//        }
//    }

    void setFormType(MenuItem sender) {
        final int senderId = sender.getItemId();
        for (EntryType entryType : EntryType.values())
            if (senderId == entryType.menuActionId) {
                setEntryType(entryType);
                return;
            }
    }

    public void setEntryType(@Nullable EntryType entryType) {
        this.entryType = entryType;
        if (entryType != null)
            monitoringPrefs.setEntryType(entryType.name());
        else
            monitoringPrefs.edit().remove(MonitoringPrefs.KEY_ENTRY_TYPE).apply();
    }

    public void onEvent(LocationChangedEvent event) {
        Location location = event.location;
        lastPosition = new LatLng(location.getLatitude(), location.getLongitude());

        if (currentMap != null) {
            currentMap.setPosition(lastPosition);
            currentMap.updateCamera();
        }

        if (menuNewEntry != null) {
            menuNewEntry.setEnabled(true);
        }
        monitoringPrefs.setLastPositionLat((float) lastPosition.latitude);
        monitoringPrefs.setLastPositionLon((float) lastPosition.longitude);

        addPoint(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    private void addPoint(LatLng point) {
        // Check if state is restored to avoid losing previous points or adding points after canceling
        if (!stateRestored || canceled) {
            return;
        }

        points.add(point);
        if (currentMap != null) {
            currentMap.updatePath(points);
        }
        persistPoints();
    }

    public void onEvent(MapClickedEvent event) {
        startNewEntryWithoutAsking(event.position, -1);
    }

    public void onEvent(MapLongClickedEvent event) {
        startNewEntryAsking(event.position, -1);
    }

    public void onEventMainThread(ActiveMonitoringEvent event) {
        if (event.monitoring == null) {
            logException(new IllegalStateException("Receive null monitoring as active monitoring."));
            finish();
            return;
        }
        this.monitoringCode = event.monitoring.code;
        setupList();
    }

    public void onEvent(MonitoringResumedEvent event) {
        eventBus.removeStickyEvent(ResumeMonitoringEvent.class);
        eventBus.removeStickyEvent(MonitoringResumedEvent.class);
    }

    public void onEvent(CancelMonitoringEvent event) {
        logException(new IllegalStateException("Receive cancelMonitoring event while in monitoring"));
        finish();
    }

    void startNewEntryWithoutAsking(final LatLng position, double accuracy) {
        if (entryType == null) {
            startNewEntryAsking(position, accuracy);
            return;
        }

        if (currentMap.getMyLocation() != null) {
            eventBus.postSticky(new LocationChangedEvent(currentMap.getMyLocation()));
        }

        Intent newEntryIntent;
        if (position != null) {
            newEntryIntent = NewMonitoringEntryActivity.newIntent(this, entryType, position.latitude, position.longitude, accuracy);
        } else {
            newEntryIntent = NewMonitoringEntryActivity.newIntent(this, entryType);
        }

        newEntryLauncher.launch(newEntryIntent);
    }

    protected void startNewEntryAsking(final LatLng position, double accuracy) {
        final List<EntryType> enabledEntryTypes = new ArrayList();
        final List<String> typeTitles = new ArrayList<>();

        if (formsEnabled != null) {
            EntryType[] allEntries = EntryType.values();
            for (EntryType form : allEntries) {
                if (formsEnabled.contains(form.name())) {
                    enabledEntryTypes.add(form);
                }
            }
        }

        for (EntryType form : enabledEntryTypes) {
            typeTitles.add(getString(form.titleId));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.menu_monitoring_new_entry)
                .setSingleChoiceItems(typeTitles.toArray(new String[]{}), entryType != null ? entryType.ordinal() : -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        setEntryType(enabledEntryTypes.get(i));
                        startNewEntryWithoutAsking(position, accuracy);
                        dialogInterface.cancel();
                    }
                })
                .setCancelable(true);

        builder.create().show();

    }

    void undoLastEntry() {
        menuUndoEntry.setEnabled(false);
        eventBus.post(new UndoLastEntry());
    }

    void crash() {
        throw new RuntimeException("Test Crash");
    }

    @Override
    protected void onPause() {
        persistState();
        super.onPause();
    }

    private void persistState() {
        Log.d(TAG, "persisting state");
        if (!canceled) {

            if (lastPosition != null) {
                monitoringPrefs.setLastPositionLat((float) lastPosition.latitude);
                monitoringPrefs.setLastPositionLon((float) lastPosition.longitude);
            } else {
                monitoringPrefs
                        .edit()
                        .remove(MonitoringPrefs.KEY_LAST_POSITION_LAT)
                        .remove(MonitoringPrefs.KEY_LAST_POSITION_LON)
                        .apply();
            }
            prefs.setZoomFactor(zoomFactor);
            if (entryType != null) {
                monitoringPrefs.setEntryType(entryType.name());
            } else {
                monitoringPrefs.edit().remove(MonitoringPrefs.KEY_ENTRY_TYPE).apply();
            }

            persistPoints();
        }
    }

    private void persistPoints() {
        SharedPreferences pointsPrefs = getSharedPreferences(SmartBirdsApplication.PREFS_MONITORING_POINTS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pointsPrefs.edit();
        editor.clear();
        try {
            if (points.isEmpty()) {
                monitoringPrefs.edit().remove(MonitoringPrefs.KEY_POINTS_COUNT).apply();
                return;
            }
            monitoringPrefs.setPointsCount(points.size());
            for (int i = 0; i < points.size(); i++) {
                editor.putFloat("lat_" + i, (float) points.get(i).latitude);
                editor.putFloat("lon_" + i, (float) points.get(i).longitude);
            }
        } finally {
            editor.apply();
        }
    }

    private void restoreState() {
        Log.d(TAG, "restoring state");

        formsEnabled = prefs.getFormsEnabled(new HashSet<>(Arrays.asList(getResources().getStringArray(R.array.monitoring_form_values))));
        providerType = MapProvider.ProviderType.GOOGLE;
        try {
            final String providerTypeName = prefs.getProviderType();
            if (!isEmpty(providerTypeName))
                providerType = MapProvider.ProviderType.valueOf(providerTypeName);
        } catch (IllegalArgumentException ignored) {
        }
        mapType = MapProvider.MapType.NORMAL;
        try {
            final String mapTypeName = prefs.getMapType();
            if (!isEmpty(mapTypeName))
                mapType = MapProvider.MapType.valueOf(mapTypeName);
        } catch (IllegalArgumentException ignored) {
        }

        if (monitoringPrefs.contains(MonitoringPrefs.KEY_LAST_POSITION_LAT) && monitoringPrefs.contains(MonitoringPrefs.KEY_LAST_POSITION_LON)) {
            lastPosition = new LatLng(monitoringPrefs.getLastPositionLat(), monitoringPrefs.getLastPositionLon());
        } else {
            lastPosition = null;
        }

        zoomFactor = prefs.getZoomFactor(500);
        entryType = null;
        try {
            final String entryTypeName = monitoringPrefs.getEntryType();
            if (!isEmpty(entryTypeName) && formsEnabled.contains(entryTypeName))
                entryType = EntryType.valueOf(entryTypeName);
        } catch (IllegalArgumentException ignored) {
        }

        setStayAwake(prefs.getStayAwake());
        this.showZoneBackground = prefs.getShowZoneBackground();
        this.showLocalProjects = prefs.getShowLocalProjects();
        this.showBgAtlasCells = prefs.getShowBgAtlasCells();
        this.showKml = prefs.getShowUserKml();
        this.showCurrentLocationCircle = prefs.getShowCurrentLocationCircle();

        Type listType = new TypeToken<List<MapLayerItem>>() {
        }.getType();
        List<MapLayerItem> mapLayers = SBGsonParser.createParser().fromJson(prefs.getMapLayers(), listType);
        Set<String> enabledLayerIds = prefs.getEnabledMapLayers();
        List<MapLayerItem> enabledLayers = new ArrayList<>();
        for (String layerId : enabledLayerIds) {
            for (MapLayerItem mapLayer : mapLayers) {
                if (layerId.equals(String.valueOf(mapLayer.getId()))) {
                    enabledLayers.add(mapLayer);
                }
            }
        }

        this.mapLayers = enabledLayers;

        restorePoints();

        updateTypeOfObservationMenus(menu);
        this.stateRestored = true;
    }

    private void restorePoints() {
        int pointsCount = monitoringPrefs.getPointsCount();
        if (pointsCount <= 0) {
            return;
        }

        SharedPreferences pointsPrefs = getSharedPreferences(SmartBirdsApplication.PREFS_MONITORING_POINTS, Context.MODE_PRIVATE);

        points.clear();
        for (int i = 0; i < pointsCount; i++) {
            final double lat = pointsPrefs.getFloat("lat_" + i, 0);
            final double lon = pointsPrefs.getFloat("lon_" + i, 0);
            points.add(new LatLng(lat, lon));
        }
    }

    private void clearPrefs() {
        Log.d(TAG, "clearing monitoring prefs");
        canceled = true;
        monitoringPrefs.edit().clear().apply();
        getSharedPreferences(SmartBirdsApplication.PREFS_MONITORING_POINTS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    @Override
    public void onMonitoringEntrySelected(long id, EntryType entryType) {
        startActivity(EditMonitoringEntryActivity.newIntent(this, id, entryType));
    }

    private Iterable<EntryMapMarker> getMarkers() {
        return new IterableConverter<>(monitoringEntries, mapMarkerConverter);
    }

    @Override
    public void onMarkerClicked(long entryId, EntryType type) {
        onMonitoringEntrySelected(entryId, type);
    }
}
