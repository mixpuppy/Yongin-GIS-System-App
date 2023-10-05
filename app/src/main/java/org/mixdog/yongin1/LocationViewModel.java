package org.mixdog.yongin1;

import androidx.lifecycle.ViewModel;

import com.google.android.gms.location.FusedLocationProviderClient;

public class LocationViewModel extends ViewModel {
    private FusedLocationProviderClient providerClient;

    public FusedLocationProviderClient getProviderClient() {
        return providerClient;
    }

    public void setProviderClient(FusedLocationProviderClient providerClient) {
        this.providerClient = providerClient;
    }
}