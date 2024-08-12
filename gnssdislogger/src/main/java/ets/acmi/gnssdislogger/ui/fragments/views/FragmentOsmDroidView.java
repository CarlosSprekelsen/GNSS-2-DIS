package ets.acmi.gnssdislogger.ui.fragments.views;
import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.slf4j.Logger;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import ets.acmi.gnssdislogger.R;
import ets.acmi.gnssdislogger.common.EventBusHook;
import ets.acmi.gnssdislogger.common.PreferenceHelper;
import ets.acmi.gnssdislogger.common.Session;
import ets.acmi.gnssdislogger.common.events.ServiceEvents;
import ets.acmi.gnssdislogger.common.slf4j.Logs;

public class FragmentOsmDroidView extends FragmentGenericView {
    private MapView mMapView;
    private IMapController mapController;
    private Marker mPositionMarker;
    private MyLocationNewOverlay mLocationOverlay;
    private final PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    private final Session session = Session.getInstance();
    private boolean firstPosition;
    private static final Logger LOG = Logs.of(FragmentOsmDroidView.class);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Configuration.getInstance().load(getContext(), getContext().getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE));

        View rootView = inflater.inflate(R.layout.fragment_osmdroid_view, container, false);
        mMapView = rootView.findViewById(R.id.osmdroidmap);
        mMapView.setTileSource(TileSourceFactory.MAPNIK);
        mMapView.setBuiltInZoomControls(true);
        mMapView.setMultiTouchControls(true);
        mapController = mMapView.getController();
        mapController.zoomTo(10);
        //mapController.setCenter(new GeoPoint(0, 0));

        setupLocationOverlay();

        return rootView;
    }


    private void setupLocationOverlay() {
        this.mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(getContext()), mMapView);
        mLocationOverlay.enableMyLocation();

        // Set custom icon
        Bitmap currentDraw = getDisIcon();
        mLocationOverlay.setPersonIcon(currentDraw);

        mLocationOverlay.enableFollowLocation();
        mMapView.getOverlays().add(mLocationOverlay);
    }

    public static FragmentOsmDroidView newInstance() {
        return new FragmentOsmDroidView();
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        mLocationOverlay.enableMyLocation();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
        mLocationOverlay.disableMyLocation();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDetach();
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LocationUpdate update) {
        if (update.location == null) return;
        GeoPoint newPosition = new GeoPoint(update.location.getLatitude(), update.location.getLongitude());
        if (mPositionMarker == null) {
            createPositionMarker(newPosition);
        } else {
            mPositionMarker.setPosition(newPosition);
        }

        mapController.animateTo(newPosition);

        if (firstPosition) {
            mMapView.getController().setZoom(18);
            firstPosition = false;
        }
    }

    private Bitmap getDisIcon() {
        // Load the bitmap from resources
        Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), preferenceHelper.getDisDrawable());

        // Create a mutable bitmap to apply changes
        Bitmap resultBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(preferenceHelper.getDisTint(), PorterDuff.Mode.SRC_ATOP));

        // Apply the color filter
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(originalBitmap, 0, 0, paint);

        return resultBitmap;
    }

    private void createPositionMarker(GeoPoint position) {
        mPositionMarker = new Marker(mMapView);
        mPositionMarker.setPosition(position);

        // Convert the Bitmap returned by getDisIcon to a Drawable
        Bitmap iconBitmap = getDisIcon();
        Drawable iconDrawable = new BitmapDrawable(getResources(), iconBitmap);

        mPositionMarker.setIcon(iconDrawable);
        mMapView.getOverlays().add(mPositionMarker);
    }
}
