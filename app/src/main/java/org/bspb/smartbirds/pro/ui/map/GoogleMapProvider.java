package org.bspb.smartbirds.pro.ui.map;

import static org.bspb.smartbirds.pro.tools.Reporting.logException;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.UrlTileProvider;
import com.google.maps.android.SphericalUtil;

import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.SmartBirdsApplication;
import org.bspb.smartbirds.pro.backend.dto.BGAtlasCell;
import org.bspb.smartbirds.pro.backend.dto.Coordinate;
import org.bspb.smartbirds.pro.backend.dto.MapLayerItem;
import org.bspb.smartbirds.pro.backend.dto.Zone;
import org.bspb.smartbirds.pro.events.EEventBus;
import org.bspb.smartbirds.pro.events.LocationChangedEvent;
import org.bspb.smartbirds.pro.events.MapAttachedEvent;
import org.bspb.smartbirds.pro.events.MapClickedEvent;
import org.bspb.smartbirds.pro.events.MapDetachedEvent;
import org.bspb.smartbirds.pro.events.MapLongClickedEvent;
import org.bspb.smartbirds.pro.tools.AppExecutors;
import org.bspb.smartbirds.pro.tools.Reporting;
import org.bspb.smartbirds.pro.ui.utils.Constants;
import org.bspb.smartbirds.pro.ui.utils.KmlUtils;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.kml.KmlFeature;
import org.osmdroid.bonuspack.kml.KmlFolder;
import org.osmdroid.bonuspack.kml.KmlGeometry;
import org.osmdroid.bonuspack.kml.KmlLineString;
import org.osmdroid.bonuspack.kml.KmlMultiGeometry;
import org.osmdroid.bonuspack.kml.KmlPlacemark;
import org.osmdroid.bonuspack.kml.KmlPoint;
import org.osmdroid.bonuspack.kml.KmlPolygon;
import org.osmdroid.bonuspack.kml.Style;
import org.osmdroid.util.GeoPoint;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by dani on 14-11-6.
 */
public class GoogleMapProvider implements MapProvider, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private static final String TAG = SmartBirdsApplication.TAG + ".GMap";
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private FragmentManager fragmentManager;
    private BspbMapFragment fragment;
    private double zoomFactor;
    private LatLng lastPosition;

    EEventBus eventBus = EEventBus.getInstance();
    private final Map<Long, MarkerHolder> markers = new HashMap<>();
    private ArrayList<LatLng> points;
    private Polyline path;
    private boolean positioned = false;
    private Marker lastMarker;
    private MapType mapType;
    private ArrayList<Zone> zones = new ArrayList<>();
    private boolean showZoneBackground;
    private List<Polygon> zonePolygons = new ArrayList<>();
    private MarkerClickListener markerClickListener;
    private List<Marker> localProjectsMarkers = new ArrayList<>();
    private boolean showLocalProjects;
    private boolean showBgAtlasCells;
    private boolean showKml;
    private ArrayList<BGAtlasCell> atlasCells = new ArrayList<>();
    private List<Polygon> atlasCellsPolygons = new ArrayList<>();
    private List<MapLayerItem> mapLayers;
    private Map<Integer, TileOverlay> mapLayerTiles = new HashMap<>();

    private List<Polygon> kmlPolygons = new ArrayList();
    private List<Marker> kmlMarkers = new ArrayList();

    private List<Circle> currentLocationCircles = new ArrayList();

    private boolean showCurrentLocationCircle = false;

    private boolean askedForMap = false;

    @Override
    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(android.os.Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    public void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null && fragment != null && !askedForMap) {
            // Try to obtain the map from the SupportMapFragment.
            this.askedForMap = true;
            fragment.getMapAsync(this);
        }
    }

    private void setUpMap() {
        addScaleBar();

        setMapType(mapType);
        mMap.setOnMapClickListener(this);
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.getUiSettings().setIndoorLevelPickerEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        if (ActivityCompat.checkSelfPermission(fragment.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        mMap.setBuildingsEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setTrafficEnabled(false);
        path = mMap.addPolyline(new PolylineOptions()
                .addAll(points)
                .width(5)
                .color(Color.BLUE)
                .geodesic(true)
        );
        mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                eventBus.post(new LocationChangedEvent(location));
            }
        });

        for (MarkerHolder markerHolder : markers.values()) {
            if (markerHolder.marker == null) {
                markerHolder.marker = addMarker(markerHolder.mapMarker);
            }
        }

        drawZones();
        drawLocalProjects(showLocalProjects);
        drawBgAtlasCells();
        showMapLayers();
        showKml();
        drawCurrentLocationCircle();

        fragment.getView().post(new Runnable() {
            @Override
            public void run() {
                updateCamera();
            }
        });

    }

    @Override
    public void setZoomFactor(double zoomFactor) {
        this.zoomFactor = zoomFactor;
    }

    @Override
    public void setShowZoneBackground(boolean showBackground) {
        this.showZoneBackground = showBackground;
    }

    @Override
    public void setShowLocalProjects(boolean showKml) {
        this.showLocalProjects = showKml;
        drawLocalProjects(showKml);
    }

    @Override
    public void setShowBgAtlasCells(boolean showBgAtlasCells) {
        this.showBgAtlasCells = showBgAtlasCells;
    }

    private void drawZones() {
        if (mMap == null || fragment == null || fragment.getContext() == null || (zonePolygons.size() == zones.size())) {
            return;
        }

        for (Polygon p : zonePolygons) {
            if (p.isVisible()) {
                if (fragment.getView() != null) {
                    fragment.getView().post(new Runnable() {
                        @Override
                        public void run() {
                            p.remove();
                        }
                    });
                }
            }
        }

        zonePolygons.clear();
        for (Zone zone : zones) {
            Polygon p = addZone(zone);
            if (p != null) {
                zonePolygons.add(p);
            }
        }
    }

    @Override
    public void drawPath(List<LatLng> points) {

    }

    @Override
    public void setShowCurrentLocationCircle(boolean showCurrentLocationCircle) {
        this.showCurrentLocationCircle = showCurrentLocationCircle;
        drawCurrentLocationCircle();
    }

    private void drawLocalProjects(boolean showKml) {
        if (fragment == null || fragment.getContext() == null) {
            return;
        }

        if (showKml) {
            if (localProjectsMarkers.size() > 0) {
                return;
            }

            List<SimpleMapMarker> localProjectsPoints = KmlUtils.readLocalProjectsPointsFromKml(fragment.getContext());
            for (SimpleMapMarker mapMarker : localProjectsPoints) {
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(new LatLng(mapMarker.getLatitude(), mapMarker.getLongitude()))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        .title(mapMarker.getTitle());
                localProjectsMarkers.add(mMap.addMarker(markerOptions));
            }
        } else {
            for (Marker marker : localProjectsMarkers) {
                marker.remove();
            }
            localProjectsMarkers.clear();
        }
    }

    private void drawBgAtlasCells() {
        if (mMap == null || fragment == null || fragment.getContext() == null || (atlasCellsPolygons.size() == atlasCells.size())) {
            return;
        }

        for (Polygon p : atlasCellsPolygons) {
            if (p.isVisible()) {
                if (fragment.getView() != null) {
                    fragment.getView().post(new Runnable() {
                        @Override
                        public void run() {
                            p.remove();
                        }
                    });
                }
            }
        }

        atlasCellsPolygons.clear();

        if (!showBgAtlasCells) {
            return;
        }

        for (BGAtlasCell cell : atlasCells) {
            Polygon p = addAtlasCell(cell);
            if (p != null) {
                atlasCellsPolygons.add(p);
            }
        }
    }

    private Polygon addAtlasCell(BGAtlasCell cell) {
        if (mMap == null) return null;

        PolygonOptions polygonOptions = new PolygonOptions();
        for (Coordinate coordinate : cell.getCoordinates()) {
            polygonOptions.add(new LatLng(coordinate.latitude, coordinate.longitude));
        }


        int color = getAtlasCellColor(cell);
        polygonOptions.strokeColor(color);
        if (showZoneBackground) {
            polygonOptions.fillColor(color);
        }
        polygonOptions.strokeWidth(6f);
        return mMap.addPolygon(polygonOptions);
    }

    private int getAtlasCellColor(BGAtlasCell cell) {
        float percent = cell.getSpecOld() > 0 ? (float) (100.0 * cell.getSpecKnown() / cell.getSpecOld()) : 0;
        if (percent < 30) {
            return fragment.getContext().getResources().getColor(R.color.atlas_cell_color_low);
        }
        if (percent < 65) {
            return fragment.getContext().getResources().getColor(R.color.atlas_cell_color_med);
        }

        return fragment.getContext().getResources().getColor(R.color.atlas_cell_color_high);
    }

    @Override
    public void showMap() {
        if (fragment == null) {
            fragment = new BspbMapFragment();
            positioned = false;
        }

        FragmentTransaction fragTransaction = fragmentManager.beginTransaction();
        fragTransaction.replace(R.id.map_container, fragment, "google");
        fragTransaction.commit();
    }

    @Override
    public void updateCamera() {
        if (mMap == null) return;
        try {
            if (zoomFactor > 0) {
                mMap.getUiSettings().setAllGesturesEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mMap.getUiSettings().setZoomControlsEnabled(false);

                if (lastPosition == null) return;

                LatLng southwest = SphericalUtil.computeOffset(lastPosition, zoomFactor, 225);
                LatLng northeast = SphericalUtil.computeOffset(lastPosition, zoomFactor, 45);
                CameraUpdate cameraUpdate;
                if (southwest.equals(northeast)) {
                    cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(lastPosition.latitude, lastPosition.longitude), 16);
                } else {
                    cameraUpdate = CameraUpdateFactory.newLatLngBounds(new LatLngBounds(southwest, northeast), 16);
                }
                if (!positioned) {
                    positioned = true;
                    mMap.animateCamera(cameraUpdate);
                } else {
                    mMap.moveCamera(cameraUpdate);
                }
            } else {
                mMap.getUiSettings().setAllGesturesEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                mMap.getUiSettings().setZoomControlsEnabled(true);
                if (!positioned && lastPosition != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPosition, 16));
                    positioned = true;
                }
            }
        } catch (Throwable t) {
            logException(t);
        }
    }

    @Override
    public void setMapType(MapType mapType) {
        this.mapType = mapType;
        if (mMap != null) {
            switch (mapType) {
                case NORMAL:
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    break;
                case SATELLITE:
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    break;
                case HYBRID:
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    break;
                default:
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    break;
            }
        }
    }

    @Override
    public Location getMyLocation() {
        return mMap.getMyLocation();
    }

    private Marker addMarker(EntryMapMarker newMapMarker) {
        if (mMap == null) return null;
        MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(newMapMarker.getLatitude(), newMapMarker.getLongitude())).title(newMapMarker.getTitle());
        lastMarker = mMap.addMarker(markerOptions);
        lastMarker.setTag(newMapMarker.getId());
        return lastMarker;
    }

    private Polygon addZone(Zone zone) {
        if (mMap == null) return null;

        PolygonOptions polygonOptions = new PolygonOptions();
        for (Coordinate coord : zone.coordinates) {
            polygonOptions.add(new LatLng(coord.latitude, coord.longitude));
        }

        polygonOptions.strokeColor(fragment.getContext().getResources().getColor(R.color.zone_stroke_color));
        if (showZoneBackground) {
            polygonOptions.fillColor(fragment.getContext().getResources().getColor(R.color.zone_fill_color));
        }

        polygonOptions.strokeWidth(6f);
        return mMap.addPolygon(polygonOptions);
    }

    @Override
    public void setFragmentManager(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    @Override
    public void setPosition(LatLng position) {
        lastPosition = position;
        drawCurrentLocationCircle();
    }

    @Override
    public void setMarkers(Iterable<EntryMapMarker> newMapMarkers) {
        for (MarkerHolder markerHolder : this.markers.values()) {
            if (markerHolder.marker != null) {
                markerHolder.marker.remove();
            }
        }

        for (EntryMapMarker mapMarker : newMapMarkers) {
            this.markers.put(mapMarker.getId(), new MarkerHolder(mapMarker, addMarker(mapMarker)));
        }
    }

    @Override
    public void setPath(ArrayList<LatLng> points) {
        this.points = points;
    }

    @Override
    public void setZones(Iterable<Zone> zones) {
        this.zones.clear();
        if (zones != null) {
            for (Zone zone : zones) {
                this.zones.add(zone);
            }
        }
        drawZones();
    }

    @Override
    public void setBgAtlasCells(List<BGAtlasCell> cells) {
        this.atlasCells.clear();
        if (cells != null) {
            this.atlasCells.addAll(cells);
        }
        drawBgAtlasCells();
    }

    @Override
    public void setShowMapLayers(List<MapLayerItem> mapLayers) {
        this.mapLayers = mapLayers;
        showMapLayers();
    }

    @Override
    public void setShowKml(boolean showKml) {
        this.showKml = showKml;
        showKml();
    }

    private void showMapLayers() {
        if (mMap == null) {
            return;
        }

        if (mapLayerTiles.size() > 0) {
            ArrayList<Integer> idsToRemove = new ArrayList<>();
            for (Integer id : mapLayerTiles.keySet()) {
                boolean found = false;
                for (MapLayerItem mapLayer : mapLayers) {
                    if (Objects.equals(mapLayer.getId(), id)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    idsToRemove.add(id);
                }
            }

            for (Integer id : idsToRemove) {
                mapLayerTiles.get(id).remove();
                mapLayerTiles.remove(id);
            }
        }
        for (MapLayerItem mapLayer : mapLayers) {
            if (!mapLayerTiles.containsKey(mapLayer.getId())) {
                TileProvider tileProvider = new UrlTileProvider(
                        mapLayer.getTileWidth() != null ? mapLayer.getTileWidth() : 256,
                        mapLayer.getTileHeight() != null ? mapLayer.getTileHeight() : 256
                ) {
                    @Override
                    public synchronized URL getTileUrl(int x, int y, int zoom) {
                        // The moon tile coordinate system is reversed.  This is not normal.
                        int reversedY = (1 << zoom) - y - 1;
                        String layerUrl = String.format(mapLayer.getUrl().get(fragment.getString(R.string.locale)), zoom, x, reversedY);

                        URL url = null;
                        try {
                            url = new URL(layerUrl);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                            throw new AssertionError(e);
                        }
                        return url;
                    }

                };

                TileOverlayOptions tileOverlayOptions = new TileOverlayOptions();
                tileOverlayOptions.tileProvider(tileProvider);
                mapLayerTiles.put(mapLayer.getId(), mMap.addTileOverlay(tileOverlayOptions));
            }
        }
    }

    @Override
    public void updatePath(ArrayList<LatLng> points) {
        this.points = points;
        path.setPoints(points);
    }

    @Override
    public void setOnMarkerClickListener(MarkerClickListener listener) {
        this.markerClickListener = listener;
    }

    private void showKml() {
        for (Polygon polygon : kmlPolygons) {
            polygon.remove();
        }
        for (Marker marker : kmlMarkers) {
            marker.remove();
        }
        kmlPolygons.clear();
        kmlMarkers.clear();

        if (showKml) {
            drawArea();
        }
    }

    void drawArea() {
        AppExecutors.background().execute(() -> {
            if (fragment == null || fragment.getContext() == null) {
                return;
            }

            KmlDocument kml = new KmlDocument();
            File file = new File(fragment.getContext().getExternalFilesDir(null), Constants.AREA_FILE_NAME);
            if (!file.exists()) {
                return;
            }

            try {
                kml.parseKMLFile(file);
                if (kml.mKmlRoot != null && kml.mKmlRoot.mItems != null && !kml.mKmlRoot.mItems.isEmpty()) {
                    displayArea(kml);
                }

            } catch (Throwable t) {
                Reporting.logException(t);
            }
        });
    }


    void displayArea(KmlDocument kml) {
        AppExecutors.mainThread().execute(() -> {
            // sometimes map is null
            if (mMap == null) {
                return;
            }
            displayItem(kml, kml.mKmlRoot);
        });
    }

    void displayItem(KmlDocument kml, KmlFeature item) {
        if (item instanceof KmlFolder) {
            KmlFolder folder = (KmlFolder) item;
            for (KmlFeature subitem : folder.mItems) {
                displayItem(kml, subitem);
            }
        } else if (item instanceof KmlPlacemark) {
            KmlPlacemark placemark = (KmlPlacemark) item;
            Style style = kml.getStyle(placemark.mStyle);

            displayGeometry(placemark.mGeometry, style, placemark.mName);
        }
    }

    private void displayGeometry(KmlGeometry geometry, Style style, String name) {
        if (geometry instanceof KmlPolygon || geometry instanceof KmlLineString) {
            ArrayList<GeoPoint> geopoints = geometry.mCoordinates;
            ArrayList<LatLng> points = new ArrayList<LatLng>();
            for (GeoPoint point : geopoints) {
                points.add(new LatLng(point.getLatitude(), point.getLongitude()));
            }
            PolygonOptions polygonOptions = new PolygonOptions();
            polygonOptions.addAll(points);

            if (style != null) {
                Paint paint = style.getOutlinePaint();
                polygonOptions.strokeColor(paint.getColor());
                polygonOptions.strokeWidth(Math.max(1f, paint.getStrokeWidth()));
                if (style.mPolyStyle != null) {
                    polygonOptions.fillColor(style.mPolyStyle.getFinalColor());
                }
            }

            kmlPolygons.add(mMap.addPolygon(polygonOptions));

        } else if (geometry instanceof KmlMultiGeometry) {
            KmlMultiGeometry multiGeometry = (KmlMultiGeometry) geometry;
            for (KmlGeometry subGeometry : multiGeometry.mItems) {
                displayGeometry(subGeometry, style, null);
            }
        } else if (geometry instanceof KmlPoint) {
            KmlPoint kmlPoint = (KmlPoint) geometry;
            ArrayList<GeoPoint> geopoints = kmlPoint.mCoordinates;
            for (GeoPoint point : geopoints) {
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(new LatLng(point.getLatitude(), point.getLongitude()))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        .title(name);
                kmlMarkers.add(mMap.addMarker(markerOptions));
            }
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        eventBus.post(new MapClickedEvent(latLng));
    }

    public void onEvent(MapAttachedEvent event) {
        setUpMapIfNeeded();
    }

    public void onEvent(MapDetachedEvent event) {
        mMap = null;
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        eventBus.post(new MapLongClickedEvent(latLng));
    }

    private void addScaleBar() {
        View mapContainer = fragment.getView();
        ScaleBar scaleBar = (ScaleBar) mapContainer.findViewById(R.id.scale_bar);
        if (scaleBar != null) return;

        FrameLayout scaleBarContainer = new FrameLayout(fragment.getContext());
        scaleBar = new ScaleBar(fragment.getActivity(), mMap);
        scaleBar.setId(R.id.scale_bar);

        FrameLayout.LayoutParams scaleBarContainerParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) scaleBar.getViewHeight());
        scaleBarContainerParams.gravity = Gravity.TOP;

        scaleBarContainer.setLayoutParams(scaleBarContainerParams);

        FrameLayout.LayoutParams scaleBarParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        scaleBarParams.gravity = Gravity.CENTER;

        scaleBar.setLayoutParams(scaleBarParams);
        scaleBarContainer.addView(scaleBar);

        if (mapContainer instanceof FrameLayout) {
            ((FrameLayout) mapContainer).addView(scaleBarContainer);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        this.askedForMap = false;
        // Check if we were successful in obtaining the map.
        if (mMap != null) {
            setUpMap();
        } else {
            if (fragment != null) {
                fragment.getMapAsync(this);
            }
        }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if (marker.getTag() == null) {
            return;
        }

        if (markerClickListener != null) {
            markerClickListener.onMarkerClicked(markers.get(marker.getTag()).mapMarker.getId(), markers.get(marker.getTag()).mapMarker.getEntryType());
        }
    }

    @Override
    public void setLifeCycle(Lifecycle lifecycle) {

    }

    static class MarkerHolder {
        EntryMapMarker mapMarker;
        Marker marker;

        public MarkerHolder(EntryMapMarker mapMarker, Marker marker) {
            this.mapMarker = mapMarker;
            this.marker = marker;
        }

        @Override
        public int hashCode() {
            return mapMarker.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;

            if (obj instanceof MarkerHolder) {
                return mapMarker.equals(((MarkerHolder) obj).mapMarker);
            }
            return mapMarker.equals((obj));
        }
    }

    @Override
    public void clearPositioned() {
        positioned = false;
    }

    private void drawCurrentLocationCircle() {
        if (mMap == null) {
            return;
        }

        if (!showCurrentLocationCircle) {
            clearCurrentLocationCircles();
            return;
        }

        if (currentLocationCircles.size() > 0 && Objects.equals(currentLocationCircles.get(0).getCenter(), lastPosition)) {
            return;
        }

        if (lastPosition == null) {
            return;
        }

        clearCurrentLocationCircles();

        for (int i = 0; i < NUMBER_OF_LOCATION_CIRCLES; i++) {
            final CircleOptions circle = new CircleOptions()
                    .center(new LatLng(lastPosition.latitude, lastPosition.longitude))
                    .radius(LOCATION_CIRCLE_RADIUS * (i + 1))
                    .strokeColor(Color.BLUE)
                    .strokeWidth(4f);

            if (i == NUMBER_OF_LOCATION_CIRCLES - 1) {
                circle.fillColor(Color.argb(30, 0, 0, 255));
            }

            currentLocationCircles.add(mMap.addCircle(circle));
        }
    }

    private void clearCurrentLocationCircles() {
        if (currentLocationCircles != null) {
            for (Circle circle : currentLocationCircles) {
                circle.remove();
            }
            currentLocationCircles.clear();
        }
    }
}
