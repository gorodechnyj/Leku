package com.schibstedspain.leku.utils;

import android.location.Location;
import android.support.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by gorodechnyj
 * Created at 04.06.17.
 */

public class GeoConverter {

    @Nullable
    public static LatLng toLatLng(@Nullable Location location) {
        if (location != null) {
            return new LatLng(location.getLatitude(), location.getLongitude());
        } else {
            return null;
        }
    }

    @Nullable
    public static Location toLocation(@Nullable LatLng latLng) {
        if (latLng != null) {
            Location result = new Location("");
            result.setLatitude(latLng.latitude);
            result.setLongitude(latLng.longitude);
            return result;
        } else {
            return null;
        }
    }
}
