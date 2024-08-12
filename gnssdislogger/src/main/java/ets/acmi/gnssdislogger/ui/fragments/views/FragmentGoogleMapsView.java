package ets.acmi.gnssdislogger.ui.fragments.views;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.slf4j.Logger;

import java.util.Objects;

import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.EventBusHook;
import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.Session;
import ets.acmi.gnssdislogger.common.Systems;
import ets.acmi.gnssdislogger.common.events.CommandEvents;
import ets.acmi.gnssdislogger.common.events.ServiceEvents;
import ets.acmi.gnssdislogger.common.slf4j.Logs;

public class FragmentGoogleMapsView extends FragmentGenericView {

    private MapView mMapView;
    private GoogleMap googleMap;
    private Marker mPositionMarker;
    private final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private final Session session = Session.getInstance();
    private boolean firstPosition;
    private static final Logger LOG = Logs.of(FragmentGoogleMapsView.class);

    public static FragmentGoogleMapsView newInstance() {
        return new FragmentGoogleMapsView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_maps_view, container, false);

        mMapView = rootView.findViewById(R.id.googlemapView);
        mMapView.onCreate(savedInstanceState);
        firstPosition = true;
        mMapView.onResume(); // needed to get the map to display immediately


        try {
            MapsInitializer.initialize(requireActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;
                if (!Systems.locationPermissionsGranted(getActivity())) {
                    LOG.error("User has not yet granted permission to access location services. Will not continue!");
                } else {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    googleMap.setMyLocationEnabled(true);
                }
            }
        });
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }


    @EventBusHook
    public void onEventMainThread(ServiceEvents.LocationUpdate update) {

        if (update.location == null)
            return;
        if (mPositionMarker == null) {
            createPositionMarker();
        }
        LatLng newPosition = new LatLng(update.location.getLatitude(), update.location.getLongitude());
        mPositionMarker.setPosition(newPosition);
        mPositionMarker.setRotation(update.location.getBearing());
        mPositionMarker.setSnippet(update.location.getAltitude() + "/" + update.location.getSpeed());
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(newPosition)); //center
        if (firstPosition) {
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(12)); //zoom
            mPositionMarker.setVisible(true);
            firstPosition = false;
        }
    }


    @EventBusHook
    public void onEventMainThread(ServiceEvents.LoggingStatus loggingStatus) {
        if (googleMap != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            googleMap.setMyLocationEnabled(!session.isStarted());
        }
        if (mPositionMarker != null) {
            mPositionMarker.setVisible(session.isStarted());
        }
    }


    /*
     * Helper functions
     * */

    private BitmapDescriptor getDisIcon() {
        Bitmap markerBitmap = BitmapFactory.decodeResource(getResources(), preferenceHelper.getDisDrawable());
        Bitmap resultBitmap = Bitmap.createBitmap(markerBitmap, 0, 0, markerBitmap.getWidth() - 1, markerBitmap.getHeight() - 1);
        Paint markerPaint = new Paint();
        markerPaint.setColorFilter(new PorterDuffColorFilter(preferenceHelper.getDisTint(), PorterDuff.Mode.SRC_ATOP));
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(resultBitmap, 0, 0, markerPaint);
        return BitmapDescriptorFactory.fromBitmap(resultBitmap);
    }

    private void createPositionMarker() {
        if (googleMap != null) {
            if (mPositionMarker != null) {
                mPositionMarker.remove();
            }
            mPositionMarker = googleMap.addMarker(new MarkerOptions()
                    .flat(false)
                    .title(preferenceHelper.getDISMarking())
                    .icon(getDisIcon())
                    .position(new LatLng(0, 0))
            );
            mPositionMarker.setVisible(false);
            mPositionMarker.showInfoWindow();
        }
    }

    @EventBusHook
    public void onEventMainThread(CommandEvents.disPreferenceChanged preferenceKey) {
        if (mPositionMarker != null) {
            createPositionMarker();
            firstPosition = true;
            //TODO: replace marker after preference changed without fragment change
        }
    }

}


