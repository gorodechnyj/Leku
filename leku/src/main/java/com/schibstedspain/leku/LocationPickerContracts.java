package com.schibstedspain.leku;

import android.location.Address;
import android.location.Location;
import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by petrg on 15.12.2017.
 */

public interface LocationPickerContracts {

    interface Interactor {

        void getLocationFromDefaultZone(String query);

        void getLocationFromZone(String query, String zoneKey);

        void queryLocation(CharSequence text);

        boolean isStreetEqualsCity(Address address);

        String getAddressString(@NonNull Address address);

        String getCoordinatesString(@NonNull Address address);

        String getCoordinatesString(@NonNull LatLng latLng);

        String getCoordinatesString(@NonNull Location location);
    }

    interface MapInteractor extends OnMapReadyCallback,
            GoogleMap.OnMapClickListener,
            GoogleMap.OnMapLongClickListener,
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener {

        void initMap();

        void moveToPosition(LatLng point);

        void placeMarker(LatLng point);

        void onStart();

        void onStop();

        void onDestroy();
    }

    interface View {

        void clearSearchResults();

        void onSearchTextChanged(CharSequence text);

        void onSearchResultSelected(@NonNull Address address);

        void showSearchResult(@NonNull Address address);

        void hideSearchResult();

        void onRadiusChanged(int radius);

        GoogleMap getMap();
    }
}
