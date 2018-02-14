package com.schibstedspain.leku;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.v4.util.ObjectsCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.schibstedspain.leku.geocoder.AndroidGeocoderDataSource;
import com.schibstedspain.leku.geocoder.GeocoderPresenter;
import com.schibstedspain.leku.geocoder.GeocoderRepository;
import com.schibstedspain.leku.geocoder.GeocoderViewInterface;
import com.schibstedspain.leku.geocoder.GoogleGeocoderDataSource;
import com.schibstedspain.leku.geocoder.api.AddressBuilder;
import com.schibstedspain.leku.geocoder.api.NetworkClient;
import com.schibstedspain.leku.tracker.TrackEvents;
import com.schibstedspain.leku.ui.search.AddressListAdapter;
import com.schibstedspain.leku.utils.GeoConverter;
import com.schibstedspain.leku.utils.Intents;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.disposables.CompositeDisposable;
import pl.charmas.android.reactivelocation2.ReactiveLocationProvider;

import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL;

public class LocationPickerActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        GoogleMap.OnCameraIdleListener,
        GoogleMap.OnCameraMoveListener,
        GeocoderViewInterface,
        LocationPickerContracts.View {

    public static final String ARG_LATITUDE = "latitude";
    public static final String ARG_LONGITUDE = "longitude";
    public static final String ARG_SEARCH_ZONE = "search_zone";
    public static final String ARG_BACK_PRESSED_RETURN_OK = "back_pressed_return_ok";
    public static final String ARG_POIS_LIST = "pois_list";
    public static final String ARG_GEOLOC_API_KEY = "geoloc_api_key";
    public static final String ARG_TITLE = "ARG_TITLE";
    public static final String ARG_RADIUS = "ARG_RADIUS";

    public static final String ADDRESS = "address";
    public static final String LOCATION_ADDRESS = "location_address";
    public static final String TRANSITION_BUNDLE = "transition_bundle";
    public static final String ENABLE_LOCATION_PERMISSION_REQUEST = "enable_location_permission_request";
    public static final String LEKU_POI = "leku_poi";
    private static final String LOCATION_KEY = "location_key";
    private static final String LAST_LOCATION_QUERY = "last_location_query";

    private static final int DEFAULT_ZOOM = 17;
    private static final int WIDER_ZOOM = 6;

    private GoogleMap map;
    private Circle radiusCircle;
    private GoogleApiClient googleApiClient;
    private Location currentLocation;
    private LekuPoi currentLekuPoi;
    private GeocoderPresenter geocoderPresenter;

    private AddressListAdapter adapter;

    private boolean hasWiderZoom = false;
    private Bundle bundle = new Bundle();
    private boolean isLocationInformedFromBundle = false;

    private boolean shouldReturnOkOnBackPressed = false;
    private boolean enableLocationPermissionRequest = true;
    private String searchZone;
    private List<LekuPoi> poisList;
    private Map<String, LekuPoi> lekuPoisMarkersMap;
    private GoogleGeocoderDataSource apiInteractor;

    private ViewModel viewModel;
    private LocationPickerContracts.Interactor interactor;
    private RxPermissions permissionsSolver;
    private CompositeDisposable compositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        compositeDisposable = new CompositeDisposable();

        setContentView(getLayoutResourceId());

        adapter = new AddressListAdapter(this);
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        apiInteractor = new GoogleGeocoderDataSource(new NetworkClient(), new AddressBuilder());
        GeocoderRepository geocoderRepository = new GeocoderRepository(new AndroidGeocoderDataSource(geocoder), apiInteractor);
        geocoderPresenter = new GeocoderPresenter(new ReactiveLocationProvider(getApplicationContext()), geocoderRepository);
        geocoderPresenter.setViewCallbacks(this);

        interactor = new LocationPickerInteractor(geocoderPresenter, searchZone);

        viewModel = new ViewModel(this, this, interactor);
        adapter.setOnAddressSelectedListener(viewModel);
        viewModel.init(adapter);
        updateValuesFromBundle(savedInstanceState);

        setUpMapIfNeeded();
        setUpFloatingButtons();
        buildGoogleApiClient();

        permissionsSolver = new RxPermissions(this);
        compositeDisposable.add(permissionsSolver.request(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(isGranted -> {
                    if (isGranted
                            && currentLocation == null) {
                        geocoderPresenter.getLastKnownLocation();
                    }
                }, e -> {
                    Log.e("LocationPicker", "", e);
                }));

        track(TrackEvents.didLoadLocationPicker);
    }

    protected int getLayoutResourceId() {
        return R.layout.activity_location_picker;
    }

    protected void track(TrackEvents event) {
        LocationPicker.getTracker().onEventTracked(event);
    }

    private void setUpFloatingButtons() {
        viewModel.setUserLocationButtonClickListener(v -> {
            geocoderPresenter.getLastKnownLocation();
            track(TrackEvents.didLocalizeMe);
        });

        viewModel.setAcceptLocationButtonClickListener(v -> returnCurrentPosition());
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Bundle transitionBundle = getIntent().getExtras();
        if (transitionBundle != null) {
            getTransitionBundleParams(transitionBundle);
        }
        if (savedInstanceState != null) {
            getSavedInstanceParams(savedInstanceState);
        }
    }

    private void setUpMapIfNeeded() {
        if (map == null) {
            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.leku_map)).getMapAsync(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Intents.REQUEST_PLACE_PICKER:
                if (resultCode == Activity.RESULT_OK) {
                    ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    interactor.queryLocation(matches.get(0));
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
        geocoderPresenter.setViewCallbacks(this);
    }

    @Override
    protected void onStop() {
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        geocoderPresenter.stop();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    protected void onDestroy() {
        viewModel.onDestroy();
        if (googleApiClient != null) {
            googleApiClient.unregisterConnectionCallbacks(this);
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (!shouldReturnOkOnBackPressed || isLocationInformedFromBundle) {
            setResult(RESULT_CANCELED);
            track(TrackEvents.CANCEL);
            finish();
        } else {
            returnCurrentPosition();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (map == null) {
            map = googleMap;
            setDefaultMapSettings();
            if (currentLocation != null) {
                setCurrentPositionLocation(currentLocation);
            }
            setPois();
            if (radiusCircle == null
                    && viewModel.getRadius() != 0) {
                onRadiusChanged(viewModel.getRadius());
            }
        }
    }

    @Override
    public void onConnected(Bundle savedBundle) {
        if (currentLocation == null) {
            geocoderPresenter.getLastKnownLocation();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, Intents.CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                track(TrackEvents.googleApiConnectionFailed);
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if (currentLocation != null) {
            savedInstanceState.putParcelable(LOCATION_KEY, currentLocation);
        }
        savedInstanceState.putString(LAST_LOCATION_QUERY, String.valueOf(viewModel.getSearchText()));
        if (bundle.containsKey(TRANSITION_BUNDLE)) {
            savedInstanceState.putBundle(TRANSITION_BUNDLE, bundle.getBundle(TRANSITION_BUNDLE));
        }
        if (poisList != null) {
            savedInstanceState.putParcelableArrayList(ARG_POIS_LIST, new ArrayList<>(poisList));
        }
        savedInstanceState.putBoolean(ENABLE_LOCATION_PERMISSION_REQUEST, enableLocationPermissionRequest);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String lastQuery = savedInstanceState.getString(LAST_LOCATION_QUERY, "");
        if (!"".equals(lastQuery)) {
            interactor.queryLocation(lastQuery);
        }
        currentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
        if (currentLocation != null) {
            setCurrentPositionLocation(currentLocation);
        }
        if (savedInstanceState.containsKey(TRANSITION_BUNDLE)) {
            bundle.putBundle(TRANSITION_BUNDLE, savedInstanceState.getBundle(TRANSITION_BUNDLE));
        }
        if (savedInstanceState.containsKey(ARG_POIS_LIST)) {
            poisList = savedInstanceState.getParcelableArrayList(ARG_POIS_LIST);
        }
        if (savedInstanceState.containsKey(ENABLE_LOCATION_PERMISSION_REQUEST)) {
            enableLocationPermissionRequest = savedInstanceState.getBoolean(ENABLE_LOCATION_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onCameraIdle() {
        currentLekuPoi = null;
        setCurrentPositionLocationLatLng(getMap().getCameraPosition().target);
        track(TrackEvents.simpleDidLocalizeByPoi);
    }

    @Override
    public void onCameraMove() {
        if (!ObjectsCompat.equals(getMap().getCameraPosition().target, GeoConverter.toLatLng(currentLocation))) {
            moveRadius();
        }
    }

    private void moveRadius() {
        if (this.map != null && this.currentLocation != null) {
            if (radiusCircle != null) {
                radiusCircle.remove();
                radiusCircle = null;
            }
            radiusCircle = this.map.addCircle(getRadiusCircle(getMap().getCameraPosition().target, viewModel.getRadius()));
        }
    }

    @Override
    public void willLoadLocation() {
        viewModel.setSearchProgressVisible(true);
        viewModel.setResultListVisibility(View.GONE);
    }

    @Override
    public void showLocations(List<Address> addresses) {
        if (addresses != null) {
            adapter.setItems(addresses);
        }
    }

    @Override
    public void didLoadLocation() {
        viewModel.setSearchProgressVisible(false);

        viewModel.setResultListVisibility(adapter.getItemCount() >= 1 ? View.VISIBLE : View.GONE);

        if (adapter.getItemCount() == 1 && adapter.getItem(0) != null) {
            viewModel.setSelectedLocationVisibility(View.VISIBLE);
        } else {
            viewModel.setSelectedLocationVisibility(View.GONE);
        }
        track(TrackEvents.didSearchLocations);
    }

    @Override
    public void showLoadLocationError() {
        viewModel.setSearchProgressVisible(false);
        viewModel.setResultListVisibility(View.GONE);
        Toast.makeText(this, R.string.load_location_error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void willGetLocationInfo(LatLng latLng) {
//        viewModel.setSelectedLocationVisibility(View.VISIBLE);
//        viewModel.setCoordinatesInfo(latLng);
    }

    @Override
    public void showLastLocation(Location location) {
        currentLocation = location;
    }

    @Override
    public void didGetLastLocation() {
        if (currentLocation != null) {
            if (!Geocoder.isPresent()) {
                Toast.makeText(this, R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
                return;
            }
            setUpMapIfNeeded();
        }
        setUpDefaultMapLocation();
    }

    @Override
    public void showLocationInfo(List<Address> addresses) {
        if (addresses != null) {
            if (addresses.size() > 0 && addresses.get(0) != null) {
                viewModel.setLocationInfo(addresses.get(0));
            } else {
                viewModel.setEmptyLocation();
            }
        }
    }

    @Override
    public void didGetLocationInfo() {
        showLocationInfoLayout();
    }

    @Override
    public void showGetLocationInfoError() {
        viewModel.setEmptyLocation();
    }

    private void showLocationInfoLayout() {
        viewModel.setSelectedLocationVisibility(View.VISIBLE);
    }

    private void getSavedInstanceParams(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(TRANSITION_BUNDLE)) {
            bundle.putBundle(TRANSITION_BUNDLE, savedInstanceState.getBundle(TRANSITION_BUNDLE));
        } else {
            bundle.putBundle(TRANSITION_BUNDLE, savedInstanceState);
        }
        if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
            currentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
        }
        setUpDefaultMapLocation();
        if (savedInstanceState.keySet().contains(ARG_GEOLOC_API_KEY)) {
            apiInteractor.setApiKey(savedInstanceState.getString(ARG_GEOLOC_API_KEY));
        }
        if (savedInstanceState.keySet().contains(ARG_SEARCH_ZONE)) {
            searchZone = savedInstanceState.getString(ARG_SEARCH_ZONE);
        }
        if (savedInstanceState.keySet().contains(ARG_POIS_LIST)) {
            poisList = savedInstanceState.getParcelableArrayList(ARG_POIS_LIST);
        }
        if (savedInstanceState.keySet().contains(ENABLE_LOCATION_PERMISSION_REQUEST)) {
            enableLocationPermissionRequest = savedInstanceState.getBoolean(ENABLE_LOCATION_PERMISSION_REQUEST);
        }
    }

    private void getTransitionBundleParams(Bundle transitionBundle) {
        bundle.putBundle(TRANSITION_BUNDLE, transitionBundle);
        if (transitionBundle.keySet().contains(ARG_LATITUDE)
                && transitionBundle.keySet().contains(ARG_LONGITUDE)) {
            setLocationFromBundle(transitionBundle.getDouble(ARG_LATITUDE), transitionBundle.getDouble(ARG_LONGITUDE));
        }
        if (transitionBundle.keySet().contains(ARG_SEARCH_ZONE)) {
            searchZone = transitionBundle.getString(ARG_SEARCH_ZONE);
        }
        if (transitionBundle.keySet().contains(ARG_BACK_PRESSED_RETURN_OK)) {
            shouldReturnOkOnBackPressed = transitionBundle.getBoolean(ARG_BACK_PRESSED_RETURN_OK);
        }
        if (transitionBundle.keySet().contains(ENABLE_LOCATION_PERMISSION_REQUEST)) {
            enableLocationPermissionRequest = transitionBundle.getBoolean(ENABLE_LOCATION_PERMISSION_REQUEST);
        }
        if (transitionBundle.keySet().contains(ARG_POIS_LIST)) {
            poisList = transitionBundle.getParcelableArrayList(ARG_POIS_LIST);
        }
        if (transitionBundle.keySet().contains(ARG_GEOLOC_API_KEY)) {
            apiInteractor.setApiKey(transitionBundle.getString(ARG_GEOLOC_API_KEY));
        }
        if (transitionBundle.keySet().contains(ARG_TITLE)) {
            viewModel.setTitle(transitionBundle.getString(ARG_TITLE));
        }
        viewModel.setRadius(transitionBundle.getInt(ARG_RADIUS));
    }

    private void setLocationFromBundle(double latitude, double longitude) {
        if (currentLocation == null) {
            currentLocation = new Location(getString(R.string.network_resource));
        }
        currentLocation.setLatitude(latitude);
        currentLocation.setLongitude(longitude);

        Address selectedAddress = new Address(Locale.getDefault());
        selectedAddress.setLatitude(latitude);
        selectedAddress.setLongitude(longitude);
        viewModel.setSelectedAddress(selectedAddress);

        setCurrentPositionLocation(currentLocation);
        isLocationInformedFromBundle = true;
    }

    private void animateCamera(LatLng latLng) {
        if (map != null) {
            CameraPosition cameraPosition =
                    new CameraPosition.Builder().target(latLng)
                            .zoom(getDefaultZoom())
                            .build();
            hasWiderZoom = false;
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    private int getDefaultZoom() {
        int zoom;
        if (hasWiderZoom) {
            zoom = WIDER_ZOOM;
        } else {
            zoom = DEFAULT_ZOOM;
        }
        return zoom;
    }

    protected void returnCurrentPosition() {
        String addressString = viewModel.getLocationAddressString();

        if (currentLekuPoi != null) {
            Intent returnIntent = new Intent();
            returnIntent.putExtra(ARG_LATITUDE, currentLekuPoi.getLocation().getLatitude());
            returnIntent.putExtra(ARG_LONGITUDE, currentLekuPoi.getLocation().getLongitude());
            if (!TextUtils.isEmpty(addressString)) {
                returnIntent.putExtra(LOCATION_ADDRESS, addressString);
            }
            returnIntent.putExtra(TRANSITION_BUNDLE, bundle.getBundle(TRANSITION_BUNDLE));
            returnIntent.putExtra(LEKU_POI, currentLekuPoi);
            returnIntent.putExtra(ARG_RADIUS, viewModel.getRadius());
            setResult(RESULT_OK, returnIntent);
            track(TrackEvents.RESULT_OK);
        } else if (currentLocation != null) {
            Intent returnIntent = new Intent();
            returnIntent.putExtra(ARG_LATITUDE, currentLocation.getLatitude());
            returnIntent.putExtra(ARG_LONGITUDE, currentLocation.getLongitude());
            returnIntent.putExtra(ARG_RADIUS, viewModel.getRadius());
            if (!TextUtils.isEmpty(addressString)) {
                returnIntent.putExtra(LOCATION_ADDRESS, addressString);
            }
            returnIntent.putExtra(ADDRESS, viewModel.getSelectedAddress());
            returnIntent.putExtra(TRANSITION_BUNDLE, bundle.getBundle(TRANSITION_BUNDLE));
            setResult(RESULT_OK, returnIntent);
            track(TrackEvents.RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
            track(TrackEvents.CANCEL);
        }
        finish();
    }

    private void setDefaultMapSettings() {
        if (map != null) {
            map.setMapType(MAP_TYPE_NORMAL);
            map.setOnCameraIdleListener(this);
            map.setOnCameraMoveListener(this);
            map.getUiSettings().setCompassEnabled(false);
            map.getUiSettings().setMyLocationButtonEnabled(true);
            map.getUiSettings().setMapToolbarEnabled(false);
        }
    }

    private void setUpDefaultMapLocation() {
        if (currentLocation != null) {
            setCurrentPositionLocation(currentLocation);
        } else {
            interactor.queryLocation(Locale.getDefault().getDisplayCountry());
            hasWiderZoom = true;
        }
    }

    private void setCurrentPositionLocation(@NonNull Location location) {
        LatLng point = GeoConverter.toLatLng(location);
        animateCamera(point);
        geocoderPresenter.getInfoFromLocation(point);
    }

    private void setCurrentPositionLocationLatLng(LatLng latLng) {
        if (currentLocation == null) {
            currentLocation = new Location(getString(R.string.network_resource));
        }
        currentLocation.setLatitude(latLng.latitude);
        currentLocation.setLongitude(latLng.longitude);
        geocoderPresenter.getInfoFromLocation(GeoConverter.toLatLng(currentLocation));
    }

    private void setPois() {
        if (poisList != null && !poisList.isEmpty()) {
            lekuPoisMarkersMap = new HashMap<>();
            for (LekuPoi lekuPoi : poisList) {
                Location location = lekuPoi.getLocation();
                if (location != null && lekuPoi.getTitle() != null) {
                    Marker marker = addPoiMarker(new LatLng(location.getLatitude(), location.getLongitude()),
                            lekuPoi.getTitle(), lekuPoi.getAddress());
                    lekuPoisMarkersMap.put(marker.getId(), lekuPoi);
                }
            }

            map.setOnMarkerClickListener(marker -> {
                LekuPoi lekuPoi = lekuPoisMarkersMap.get(marker.getId());
                if (lekuPoi != null) {
                    this.currentLekuPoi = lekuPoi;
                    viewModel.setLocationInfo(lekuPoi);
                    centerToPoi(lekuPoi);
                    track(TrackEvents.simpleDidLocalizeByLekuPoi);
                }
                return true;
            });
        }
    }

    private void centerToPoi(LekuPoi lekuPoi) {
        if (map != null) {
            Location location = lekuPoi.getLocation();
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude())).zoom(getDefaultZoom()).build();
            hasWiderZoom = false;
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    private synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    private Marker addMarker(LatLng latLng) {
        return map.addMarker(getPickerMarker(latLng));
    }

    private Marker addPoiMarker(LatLng latLng, String title, String address) {
        return map.addMarker(getPoiMarker(latLng, title, address));
    }

    public MarkerOptions getPickerMarker(LatLng point) {
        return new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                .position(point)
                .draggable(true);
    }

    public MarkerOptions getPoiMarker(LatLng point, String title, String address) {
        return new MarkerOptions()
                .position(point)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title(title)
                .snippet(address);
    }

    private void setNewLocation(Address address) {
        if (currentLocation == null) {
            currentLocation = new Location(getString(R.string.network_resource));
        }

        currentLocation.setLatitude(address.getLatitude());
        currentLocation.setLongitude(address.getLongitude());
        viewModel.setLocationInfo(address);
        viewModel.setSearchText(address);
        CameraPosition cameraPosition =
                new CameraPosition.Builder().target(new LatLng(address.getLatitude(), address.getLongitude()))
                        .zoom(viewModel.getZoomForMap(viewModel.getRadius()))
                        .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        viewModel.setResultListVisibility(View.GONE);
    }

    @Override
    public void clearSearchResults() {
        adapter.clear();
    }

    @Override
    public void onSearchTextChanged(CharSequence text) {
    }

    @Override
    public void onSearchResultSelected(@NonNull Address address) {
        setNewLocation(address);
    }

    @Override
    public void showSearchResult(@NonNull Address address) {

    }

    @Override
    public void hideSearchResult() {

    }

    @Override
    public void onRadiusChanged(int radius) {
        if (this.map != null && this.currentLocation != null) {
            if (radiusCircle != null) {
                radiusCircle.remove();
                radiusCircle = null;
            }
            radiusCircle = this.map.addCircle(getRadiusCircle(GeoConverter.toLatLng(currentLocation), radius));

            changeZoom(radius);
        }
    }

    private void changeZoom(int radius) {
        CameraPosition cameraPosition =
                new CameraPosition.Builder().target(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()))
                        .zoom(viewModel.getZoomForMap(radius))
                        .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    public CircleOptions getRadiusCircle(LatLng center, int radius) {
        return new CircleOptions()
                .center(center)
                .radius(radius)
                .fillColor(Color.argb(51, 255, 0, 0))
                .strokeWidth(getResources().getDimensionPixelSize(R.dimen.map_marker_circle_stroke_width))
                .strokeColor(Color.argb(51, 255, 0, 0));
    }

    @Override
    public GoogleMap getMap() {
        return map;
    }


    public static class Builder {
        private Double locationLatitude;
        private Double locationLongitude;
        private String locationSearchZone;
        private boolean shouldReturnOkOnBackPressed = false;
        private List<LekuPoi> lekuPois;
        private String geolocApiKey = null;
        private String title = null;
        private int radius = 0;

        public Builder() {
        }

        public Builder withLocation(double latitude, double longitude) {
            this.locationLatitude = latitude;
            this.locationLongitude = longitude;
            return this;
        }

        public Builder withLocation(LatLng latLng) {
            if (latLng != null) {
                this.locationLatitude = latLng.latitude;
                this.locationLongitude = latLng.longitude;
            }
            return this;
        }

        public Builder withSearchZone(String searchZone) {
            this.locationSearchZone = searchZone;
            return this;
        }

        public Builder shouldReturnOkOnBackPressed() {
            this.shouldReturnOkOnBackPressed = true;
            return this;
        }

        public Builder withPois(List<LekuPoi> pois) {
            this.lekuPois = pois;
            return this;
        }

        public Builder withGeolocApiKey(String apiKey) {
            this.geolocApiKey = apiKey;
            return this;
        }

        public Builder withRadius(int radius) {
            this.radius = radius;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Intent build() {
            Intent intent = new Intent("com.schibstedspain.leku.PICK_LOCATION");

            if (locationLatitude != null) {
                intent.putExtra(ARG_LATITUDE, locationLatitude);
            }
            if (locationLongitude != null) {
                intent.putExtra(ARG_LONGITUDE, locationLongitude);
            }
            if (locationSearchZone != null) {
                intent.putExtra(ARG_SEARCH_ZONE, locationSearchZone);
            }
            intent.putExtra(ARG_BACK_PRESSED_RETURN_OK, shouldReturnOkOnBackPressed);
            if (lekuPois != null && !lekuPois.isEmpty()) {
                intent.putExtra(ARG_POIS_LIST, new ArrayList<>(lekuPois));
            }
            if (geolocApiKey != null) {
                intent.putExtra(ARG_GEOLOC_API_KEY, geolocApiKey);
            }
            if (title != null) {
                intent.putExtra(ARG_TITLE, this.title);
            }
            if (radius != 0) {
                intent.putExtra(ARG_RADIUS, this.radius);
            }

            return intent;
        }
    }
}
