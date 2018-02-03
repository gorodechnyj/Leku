package com.schibstedspain.leku.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.schibstedspain.leku.LocationPicker;
import com.schibstedspain.leku.R;
import com.schibstedspain.leku.tracker.TrackEvents;

/**
 * Created by petrg on 19.12.2017.
 */

public class Intents {

    public static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    public static final int REQUEST_PLACE_PICKER = 6655;

    public static void startVoiceRecognitionActivity(Activity context) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                context.getString(R.string.voice_search_promp));
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                context.getString(R.string.voice_search_extra_language));

        if (checkPlayServices(context)) {
            try {
                context.startActivityForResult(intent, REQUEST_PLACE_PICKER);
            } catch (ActivityNotFoundException e) {
                LocationPicker.getTracker().onEventTracked(TrackEvents.startVoiceRecognitionActivityFailed);
            }
        }
    }

    public static boolean checkPlayServices(Activity context) {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(context);
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(context, result, CONNECTION_FAILURE_RESOLUTION_REQUEST).show();
            }
            return false;
        }
        return true;
    }
}
