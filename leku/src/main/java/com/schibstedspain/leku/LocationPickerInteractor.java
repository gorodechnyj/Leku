package com.schibstedspain.leku;

import android.location.Address;
import android.location.Location;
import android.support.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.schibstedspain.leku.geocoder.GeocoderPresenter;

import java.util.Locale;

/**
 * Created by petrg on 15.12.2017.
 */

public class LocationPickerInteractor implements LocationPickerContracts.Interactor {
    private final GeocoderPresenter geocoderPresenter;
    private final String searchZone;

    public LocationPickerInteractor(GeocoderPresenter geocoderPresenter, String searchZone) {
        this.geocoderPresenter = geocoderPresenter;
        this.searchZone = searchZone;
    }

    public void getLocationFromDefaultZone(String query) {
        if (CountryLocaleRect.getDefaultLowerLeft() != null) {
            geocoderPresenter.getFromLocationName(query, CountryLocaleRect.getDefaultLowerLeft(),
                    CountryLocaleRect.getDefaultUpperRight());
        } else {
            geocoderPresenter.getFromLocationName(query);
        }
    }

    @Override
    public void queryLocation(CharSequence text) {
        if (searchZone != null && !searchZone.isEmpty()) {
            getLocationFromZone(text.toString(), searchZone);
        } else {
            getLocationFromDefaultZone(text.toString());
        }
    }

    public void getLocationFromZone(String query, String zoneKey) {
        Locale locale = new Locale(zoneKey);
        if (CountryLocaleRect.getLowerLeftFromZone(locale) != null) {
            geocoderPresenter.getFromLocationName(query, CountryLocaleRect.getLowerLeftFromZone(locale),
                    CountryLocaleRect.getUpperRightFromZone(locale));
        } else {
            geocoderPresenter.getFromLocationName(query);
        }
    }

    @Override
    public boolean isStreetEqualsCity(Address address) {
        return address.getAddressLine(0).equals(address.getLocality());
    }

    @Override
    public String getAddressString(@NonNull Address address) {
        return StringUtils.joinNullable(", ",
                address.getLocality(),
                address.getThoroughfare(),
                address.getSubThoroughfare());
    }

    @Override
    public String getCoordinatesString(@NonNull Address address) {
        return StringUtils.joinNullable(", ",
                "" + address.getLatitude(),
                "" + address.getLongitude());
    }

    @Override
    public String getCoordinatesString(@NonNull LatLng latLng) {
        return StringUtils.joinNullable(", ",
                "" + latLng.latitude,
                "" + latLng.longitude);
    }

    @Override
    public String getCoordinatesString(@NonNull Location location) {
        return StringUtils.joinNullable(", ",
                "" + location.getLatitude(),
                "" + location.getLongitude());
    }
}
