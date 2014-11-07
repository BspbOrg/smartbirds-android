package org.bspb.smartbirds.pro.ui.map;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.events.EEventBus;
import org.bspb.smartbirds.pro.events.LocationChangedEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dani on 14-11-6.
 */
@EBean
public class GoogleMapProvider implements MapProvider {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private FragmentManager fragmentManager;
    private SupportMapFragment fragment;
    private double zoomFactor;
    private LatLng lastPosition;

    @Bean
    EEventBus eventBus;
    private ArrayList<MapMarker> markers;
    private ArrayList<LatLng> points;
    private Polyline path;
    private boolean positioned = false;

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
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = fragment.getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        mMap.getUiSettings().setIndoorLevelPickerEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.setMyLocationEnabled(true);
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

        if (markers != null && !markers.isEmpty()) {
            for (MapMarker marker : markers) {
                MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(marker.getLatitude(), marker.getLongitude())).title(marker.getTitle());
                mMap.addMarker(markerOptions);
            }
        }

        updateCamera();
    }

    @Override
    public void setZoomFactor(double zoomFactor) {
        this.zoomFactor = zoomFactor;
    }

    @Override
    public void drawPath(List<LatLng> points) {

    }

    @Override
    public void showMap() {
        positioned = false;
        if (fragment == null) {
            fragment = new SupportMapFragment() {
                @Override
                public void onViewCreated(View view, Bundle savedInstanceState) {
                    super.onViewCreated(view, savedInstanceState);
                    getView().post(new Runnable() {
                        @Override
                        public void run() {
                            setUpMapIfNeeded();
                        }
                    });
                }

                @Override
                public void onDetach() {
                    super.onDetach();
                    mMap = null;
                }
            };
        }

        FragmentTransaction fragTransaction = fragmentManager.beginTransaction();
        fragTransaction.replace(R.id.map_container, fragment, "google");
        fragTransaction.commit();

    }

    @Override
    public void updateCamera() {
        if (zoomFactor > 0) {
            mMap.getUiSettings().setAllGesturesEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.getUiSettings().setZoomControlsEnabled(false);

            if (lastPosition == null) return;

            LatLng southwest = SphericalUtil.computeOffset(lastPosition, zoomFactor, 225);
            LatLng northeast = SphericalUtil.computeOffset(lastPosition, zoomFactor, 45);
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(southwest, northeast), 16));
            positioned = true;
        } else {
            mMap.getUiSettings().setAllGesturesEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            if (!positioned && lastPosition != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPosition, 16));
                positioned = true;
            }
        }
    }

    @Override
    public void setMapType(MapType mapType) {
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

    @Override
    public Location getMyLocation() {
        return mMap.getMyLocation();
    }

    @Override
    public void addMarker(MapMarker marker) {
        MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(marker.getLatitude(), marker.getLongitude())).title(marker.getTitle());
        mMap.addMarker(markerOptions);
    }

    @Override
    public void setFragmentManager(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    @Override
    public void setPosition(LatLng position) {
        lastPosition = position;
    }

    @Override
    public void setMarkers(ArrayList<MapMarker> markers) {
        this.markers = markers;
    }

    @Override
    public void setPath(ArrayList<LatLng> points) {
        this.points = points;
    }

    @Override
    public void updatePath(ArrayList<LatLng> points) {
        this.points = points;
        path.setPoints(points);
    }
}
