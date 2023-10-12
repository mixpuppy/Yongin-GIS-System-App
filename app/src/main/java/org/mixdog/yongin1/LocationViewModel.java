package org.mixdog.yongin1;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
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

    // 포어그라운드 서비스 관련
    private final MutableLiveData<String> action = new MutableLiveData<>();

    public void setAction(String newAction) {
        action.setValue(newAction);
    }

    public LiveData<String> getAction() {
        return action;
    }
}