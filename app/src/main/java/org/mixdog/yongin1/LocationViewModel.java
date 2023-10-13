package org.mixdog.yongin1;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.location.FusedLocationProviderClient;

// ViewModelProvider를 이용해 액티비티와 프레그먼트 간 데이터 공유가 쉬워짐
public class LocationViewModel extends ViewModel {
    private FusedLocationProviderClient providerClient;

    public FusedLocationProviderClient getProviderClient() {
        return providerClient;
    }

    public void setProviderClient(FusedLocationProviderClient providerClient) {
        this.providerClient = providerClient;
    }

    /*
    // 포어그라운드 서비스 관련
    // 뷰와 서비스 사이에 viewModel로는 데이터 공유가 어려워서 뺐다.
    private final MutableLiveData<String> action = new MutableLiveData<>();

    public void setAction(String newAction) {
        action.setValue(newAction);
    }

    public LiveData<String> getAction() {
        return action;
    }

     */
}