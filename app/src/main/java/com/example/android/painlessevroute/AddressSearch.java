package com.example.android.painlessevroute;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.Style;
import com.mapbox.maps.extension.observable.eventdata.MapIdleEventData;
import com.mapbox.maps.plugin.delegates.listeners.OnMapIdleListener;
import com.mapbox.search.CompletionCallback;
import com.mapbox.search.Country;
import com.mapbox.search.autofill.AddressAutofill;
import com.mapbox.search.autofill.AddressAutofillOptions;
import com.mapbox.search.autofill.AddressAutofillSuggestion;
import com.mapbox.search.autofill.AddressComponents;
import com.mapbox.search.autofill.Query;
import com.mapbox.search.ui.adapter.autofill.AddressAutofillUiAdapter;
import com.mapbox.search.ui.view.CommonSearchViewConfiguration;
import com.mapbox.search.ui.view.DistanceUnitType;
import com.mapbox.search.ui.view.SearchResultsView;

import java.util.ArrayList;
import java.util.List;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class AddressSearch extends AppCompatActivity implements TextWatcher,
        LocationEngineCallback
                <LocationEngineResult> {

    AddressAutofill addressAutofill = null;
    AddressAutofillUiAdapter searchEngineUiAdapter = null;
    Boolean ignoreNextQueryTextUpdate = false;
    AddressAutofillSuggestion currentSuggestion = null;
    LocationEngine locationEngine = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_search);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        MapView mapView = (MapView) findViewById(R.id.map);
        MapboxMap mapboxMap = mapView.getMapboxMap();
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS);

        addressAutofill = AddressAutofill.create(getString(R.string.mapbox_access_token));
        EditText searchText = (EditText) findViewById(R.id.query_text);

        searchText.addTextChangedListener(this);

        SearchResultsView searchResultsView = (SearchResultsView) findViewById(R.id.search_results_view);
        searchResultsView.initialize(new SearchResultsView.Configuration(new CommonSearchViewConfiguration(DistanceUnitType.METRIC)));
        searchResultsView.setVisibility(View.GONE);

        locationEngine = LocationEngineProvider.getBestLocationEngine(getApplicationContext());
        searchEngineUiAdapter = new AddressAutofillUiAdapter(
                searchResultsView,
                addressAutofill,
                locationEngine
        );

        searchEngineUiAdapter.addSearchListener(new AddressAutofillUiAdapter.SearchListener() {
            @Override
            public void onSuggestionsShown(@NonNull List<AddressAutofillSuggestion> list) {

            }

            @Override
            public void onSuggestionSelected(@NonNull AddressAutofillSuggestion suggestion) {
                showAddressAutofillSuggestion(suggestion, false);
            }

            @Override
            public void onError(@NonNull Exception e) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                super.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence text, int start, int before, int count) {
        if (ignoreNextQueryTextUpdate) {
            ignoreNextQueryTextUpdate = false;
            return;
        }

        Query query = Query.create(text.toString());
        if (query != null) {
            searchEngineUiAdapter.search(query, new Continuation<Unit>() {
                @NonNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object o) {
                    return;
                }
            });
        }

        SearchResultsView searchResultsView = (SearchResultsView) findViewById(R.id.search_results_view);
        searchResultsView.setVisibility(query != null ? View.VISIBLE : View.GONE);
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }

    private void showAddressAutofillSuggestion(AddressAutofillSuggestion suggestion, Boolean fromReverseGeocoding) {
        currentSuggestion = suggestion;
        AddressComponents address = suggestion.result().getAddress();

        TextView fullAddress = (TextView) findViewById((R.id.full_address));
        fullAddress.setVisibility(View.VISIBLE);
        fullAddress.setText(suggestion.getFormattedAddress());

        TextView pinCorrectionNote = (TextView) findViewById((R.id.pin_correction_note));
        pinCorrectionNote.setVisibility(View.VISIBLE);

        if (!fromReverseGeocoding) {
            ImageView mapPin = (ImageView) findViewById(R.id.map_pin);
            MapView mapView = (MapView) findViewById(R.id.map);
            mapView.getMapboxMap().setCamera(
                    new CameraOptions.Builder()
                            .center(suggestion.getCoordinate())
                            .zoom(16.0)
                            .build()
            );
            mapPin.setVisibility(View.VISIBLE);
        }

        ignoreNextQueryTextUpdate = true;
        EditText searchText = (EditText) findViewById(R.id.query_text);
        searchText.setText(address.getHouseNumber() + " " + address.getStreet());
        searchText.clearFocus();
        SearchResultsView searchResultsView = (SearchResultsView) findViewById(R.id.search_results_view);
        searchResultsView.setVisibility(View.GONE);

        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void GoSearch(View view) {
        if( currentSuggestion == null) {
            super.onBackPressed();
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            super.onBackPressed();
            return;
        }
        locationEngine.getLastLocation(this);
    }

    @Override
    public void onSuccess(LocationEngineResult currentLocation) {
        if(currentLocation != null && currentSuggestion != null ) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putFloat("startLongitude", (float) (currentLocation.getLastLocation().getLongitude()));
            editor.putFloat("startLatitude", (float) (currentLocation.getLastLocation().getLatitude()));
            editor.putFloat("destinationLongitude", (float) (currentSuggestion.getCoordinate().longitude()));
            editor.putFloat("destinationLatitude", (float) (currentSuggestion.getCoordinate().latitude()));
            editor.apply();
        }
        super.onBackPressed();
    }

    @Override
    public void onFailure(@NonNull Exception exception) {
        super.onBackPressed();
    }
}