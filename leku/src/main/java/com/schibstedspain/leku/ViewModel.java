package com.schibstedspain.leku;

import android.location.Address;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.schibstedspain.leku.ui.search.AddressListAdapter;
import com.schibstedspain.leku.utils.Intents;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by petrg on 14.12.2017.
 */

public class ViewModel implements AddressListAdapter.OnAddressSelectedListener {

    // external dependencies
    private final LocationPickerActivity activity;
    private final LocationPickerContracts.View viewCallbacks;
    private final LocationPickerContracts.Interactor interactor;

    // view references
    private EditText edSearchView;
    private ViewGroup layoutLocationInfo;
    private TextView tvAddressSecondaryLine, tvAddressPrimaryLine;
    private ProgressBar pgSearchProgress;
    private RecyclerView rvSearchResults;
    private ImageView btnClearSearch, btnVoiceSearch;
    private FloatingActionButton btnMyLocation, btnAcceptLocation;

    private Address selectedAddress;

    private CompositeDisposable compositeDisposable;

    public ViewModel(LocationPickerActivity activity,
                     LocationPickerContracts.View viewCallbacks,
                     LocationPickerContracts.Interactor interactor) {
        this.activity = activity;
        this.viewCallbacks = viewCallbacks;
        this.interactor = interactor;
    }

    public void init(AddressListAdapter adapter) {
        compositeDisposable = new CompositeDisposable();
        findViews();

        rvSearchResults.setLayoutManager(new LinearLayoutManager(this.activity));
        rvSearchResults.setAdapter(adapter);

        setSearchProgressVisible(false);
        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(view -> {
                setSearchText("");
            });
        }
        if (btnVoiceSearch != null) {
            btnVoiceSearch.setOnClickListener(v -> {
                Intents.startVoiceRecognitionActivity(this.activity);
            });
        }

        if (edSearchView != null) {
            edSearchView.setOnEditorActionListener((v, actionId, event) -> {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    interactor.queryLocation(v.getText().toString());
                    KeyboardUtil.hideKeyboard(this.activity);
                    handled = true;
                }
                return handled;
            });
            compositeDisposable.add(RxTextView.textChanges(edSearchView)
                    .skip(1)
                    .doOnNext(text -> {
                        if (TextUtils.isEmpty(text)) {
                            viewCallbacks.clearSearchResults();
                            setClearSearchButtonVisible(false);
                            showLocationInfoLayout();
                        } else {
                            setClearSearchButtonVisible(true);
                        }
                        viewCallbacks.onSearchTextChanged(text);
                    })
                    .debounce(300, TimeUnit.MILLISECONDS, Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(text -> {
                        if (!TextUtils.isEmpty(text)) {
                            interactor.queryLocation(text);
                        }
                    }, e -> {
                        viewCallbacks.onSearchTextChanged("");
                        Log.e(ViewModel.class.getSimpleName(), "", e);
                    }));
        }
    }

    private void findViews() {
        edSearchView = activity.findViewById(R.id.leku_search);
        pgSearchProgress = activity.findViewById(R.id.leku_loading_progress_bar);
        layoutLocationInfo = activity.findViewById(R.id.leku_location_info);
        tvAddressPrimaryLine = activity.findViewById(R.id.leku_address_primary_line);
        tvAddressSecondaryLine = activity.findViewById(R.id.leku_address_secondary_line);
        btnClearSearch = activity.findViewById(R.id.leku_control_clear);
        btnVoiceSearch = activity.findViewById(R.id.leku_voice_search);
        btnMyLocation = activity.findViewById(R.id.leku_control_user_location);
        btnAcceptLocation = activity.findViewById(R.id.leku_control_accept);
        rvSearchResults = activity.findViewById(R.id.leku_suggestions_search_result);
        Toolbar toolbar = activity.findViewById(R.id.leku_map_search_toolbar);
        activity.setSupportActionBar(toolbar);
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    public void onDestroy() {
        if (compositeDisposable != null
                && !compositeDisposable.isDisposed()) {
            compositeDisposable.dispose();
        }
    }

    public void setTitle(String title) {
        if (activity.getSupportActionBar() != null) {
            if (TextUtils.isEmpty(title)) {
                activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
            } else {
                activity.getSupportActionBar().setDisplayShowTitleEnabled(true);
                activity.getSupportActionBar().setTitle(title);
            }
        }
    }

    public void setSearchProgressVisible(boolean visible) {
        if (pgSearchProgress != null) {
            pgSearchProgress.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public void setClearSearchButtonVisible(boolean visible) {
        if (btnClearSearch != null) {
            btnClearSearch.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        if (btnVoiceSearch != null) {
            btnVoiceSearch.setVisibility(visible ? View.GONE : View.VISIBLE);
        }
    }

    public void setSearchText(String text) {
        if (edSearchView != null) {
            edSearchView.setText(text);
        }
    }

    public String getSearchText() {
        if (edSearchView != null) {
            return edSearchView.getText().toString();
        } else {
            return "";
        }
    }

    // -- btnSatellite

    public void showLocationInfoLayout() {
        setSelectedLocationVisibility(View.VISIBLE);
    }

    public void setSelectedLocationVisibility(int visibility) {
        layoutLocationInfo.setVisibility(visibility);
    }

    public void setUserLocationButtonClickListener(View.OnClickListener listener) {
        if (btnMyLocation != null) {
            btnMyLocation.setOnClickListener(listener);
        }
    }

    public void setAcceptLocationButtonClickListener(View.OnClickListener listener) {
        if (btnAcceptLocation != null) {
            btnAcceptLocation.setOnClickListener(listener);
        }
    }

    public void setCoordinatesInfo(LatLng latLng) {
        if (tvAddressSecondaryLine != null) {
            tvAddressSecondaryLine.setText(interactor.getCoordinatesString(latLng));
        }
    }

    public void setEmptyLocation() {
        setSelectedLocationVisibility(View.VISIBLE);
    }

    public void setLocationInfo(@NonNull LekuPoi poi) {
        this.selectedAddress = new Address(Locale.getDefault());
        this.selectedAddress.setFeatureName(poi.getTitle());
        this.selectedAddress.setAddressLine(0, poi.getAddress());
        setPrimaryAddressLine(poi.getAddress());
        setSecondaryAddressLine(interactor.getCoordinatesString(poi.getLocation()));
        setSelectedLocationVisibility(View.VISIBLE);
    }

    public void setLocationInfo(@NonNull Address address) {
        this.selectedAddress = address;
        setPrimaryAddressLine(interactor.getAddressString(address));
        setSecondaryAddressLine(interactor.getCoordinatesString(address));
        setSelectedLocationVisibility(View.VISIBLE);
    }

    public void setResultListVisibility(int visibility) {
        if (rvSearchResults != null) {
            rvSearchResults.setVisibility(visibility);
        }
        if (btnMyLocation != null) {
            btnMyLocation.setVisibility((visibility == View.VISIBLE) ? View.GONE : View.VISIBLE);
        }
    }

    public String getLocationAddressString() {
        if (this.selectedAddress != null) {
            return interactor.getAddressString(this.selectedAddress);
        } else {
            return "";
        }
    }

    public void setPrimaryAddressLine(String text) {
        if (this.tvAddressPrimaryLine != null) {
            this.tvAddressPrimaryLine.setText(text);
        }
    }

    public void setSecondaryAddressLine(String text) {
        if (this.tvAddressSecondaryLine != null) {
            this.tvAddressSecondaryLine.setText(text);
        }
    }

    public Address getSelectedAddress() {
        return this.selectedAddress;
    }

    @Override
    public void onAddressSelected(Address address) {
        this.selectedAddress = address;
        viewCallbacks.onSearchResultSelected(address);
        setResultListVisibility(View.GONE);
        KeyboardUtil.hideKeyboard(this.activity);
    }

    public void setSelectedAddress(Address selectedAddress) {
        this.selectedAddress = selectedAddress;
    }
}
