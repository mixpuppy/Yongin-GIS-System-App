package org.mixdog.yongin1;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback {
    // ↳ 지도가 준비되면 원하는 작업 수행하도록 구현

    // 프레그먼트 네비게이션 사용
    private NavController navController;
    // onConnected() 메소드를 사용할 수 있을때 사용하는 변수
    private FusedLocationProviderClient providerClient;
    // 지도 객체
    private GoogleMap mMap;
    // 지도 ui 객체
    private UiSettings mUi;
    // 위도 경도
    private double mLat, mLng;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    public static Marker nonStartMarker;
    // 시작버튼 활성화 여부 (true 시 활성화)
    public static boolean isInitialMarkerSet = false;

    /**
     * 액티비티가 실행되고 처음 시작될 때 호출
     * @param savedInstanceState : 이전에 저장된 상태, 액티비티가 종료되었던 이전 상태정보 복원
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // NavHostFragment 가져오기
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        // NavController 설정
        navController = navHostFragment.getNavController();

        // 지도 호출
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapView);
        Log.d("mixpuppy", "MainActivity Oncreate if 전");
        if (mapFragment != null) {
            Log.d("mixpuppy", "MainActivity Oncreate if 실행");
            mapFragment.getMapAsync(this); // 비동기적으로 Google Maps API 지도를 로드
        } else {
            Log.d("mixpuppy", "MainActivity Oncreate else로 빠짐");
        }

        // 신규 방법 위치권한 확인 및 요청
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // 위치 권한이 이미 허용되어 있는 경우
            initializeLocation();
        } else {
            // 위치 권한을 요청하는 메소드
            requestLocationPermission();
        }

    }

    // 인터페이스 구현체
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d("mixpuppy", "onMapReady: 지도 설정 성공적");
        // 컨트롤러 올리기
        mUi = mMap.getUiSettings();

        if (mMap != null) {
            Log.d("MyApp", "onMapReady: 초기 좌표와 줌 레벨 설정");
            // 초기 좌표와 줌 레벨 설정
            LatLng initLatLng = new LatLng(37.2214872, 127.2218612);
            float zoomLevel = 14.0f;

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initLatLng, zoomLevel));
        }
        // 나침반 추가
        mUi.setCompassEnabled(true);
        // 확대축소 컨트롤 추가
        mUi.setZoomControlsEnabled(true);
    }

    private void moveMap(double latitude, double longitude){
        if(mMap != null) {
        LatLng latLng = new LatLng(latitude, longitude);
        // 중심 좌표 생성
        CameraPosition position = CameraPosition.builder()
                .target(latLng)
                .zoom(17f)
                .build();
        // 지도 중심 이동
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));
        setCurrentMarker(latLng);
        }
    }

    // mMap 변수의 fragment 사용 위한 메소드
    public GoogleMap getMap() {
        return mMap;
    }

    // 위치 서비스를 초기화하고 위치 업데이트를 요청하는 역할
    private void initializeLocation() {
        // 위치 서비스를 관리하는 클라이언트
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // 위치 업데이트에 대한 요청을 정의
        locationRequest = new LocationRequest();
        // 5~10초 간격으로 위치 업데이트 (배터리 소모를 줄이거나 정확도를 조절)
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000);
        // 정확도를 우선시하여 위치 업데이트를 요청
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //ViewModel로 providerClient 를 전달
        //(Android Architecture Components 라이브러리에서 제공하는 ViewModelProvider를 사용)
        LocationViewModel viewModel = new ViewModelProvider(this).get(LocationViewModel.class);
        viewModel.setProviderClient(fusedLocationClient);

        // 위치 업데이트 결과를 수행하는 역할 (인스턴스)
        locationCallback = new LocationCallback() {
            // 위치 업데이트에 대한 반복되는 부분
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    Location location = locationResult.getLastLocation();
                    mLat = location.getLatitude();
                    mLng = location.getLongitude();
                    Log.d("mixpuppy", "MainActivity 위치정보 업데이트 | 위도 : " + mLat + ", 경도 : " + mLng);
                    moveMap(mLat, mLng);
                }
            }
        };
        // 위치 업데이트를 시작
        startLocationUpdates();
    }

    // 위치권한 요청하고 처리하는 메소드
    private void requestLocationPermission() {
        // ActivityResultLauncher : 권한 요청 결과를 처리하는 데 사용
        ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                // 권한 요청 결과로 전달되는 맵(Map) 형태의 변수
                // 이 맵은 권한 이름(문자열)과 권한이 허용되었는지 여부(Boolean)를 포함
                permissions -> {
                    // 권한이 허용되었는지 확인
                    if (permissions.values().stream().allMatch(Boolean::booleanValue)) {
                        // 권한이 허용된 경우
                        initializeLocation();
                    } else { // 거부된 경우 메시지를 표시
                        Toast.makeText(this,
                                R.string.user_location_permission_not_granted,
                                Toast.LENGTH_LONG).show();
                    }
                });

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // 권한 요청 이유를 설명하는 다이얼로그를 표시 가능
            Toast.makeText(this,
                    R.string.user_location_permission_required,
                    Toast.LENGTH_LONG).show();
        } else {
            // 권한을 직접 요청
            requestPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
        }
    }

    // 위치 업데이트를 시작하는 역할
    private void startLocationUpdates() {
        // 만약 권한이 부여되어 있다면
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            /**
             * locationRequest : 위치 업데이트의 주기와 우선순위를 설정한 객체
             * locationCallback : 위치 업데이트 이벤트를 처리하는 콜백 함수를 설정한 객체
             * null : Looper 를 지정하는데 사용. 이 경우 null로 지정하여 기본
             */
            // 위치 업데이트를 요청 시작 (트리거)
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    // 메모리 누수를 방지 (베터리 소모 방지)
    // 액티비티가 파괴될 때 호출되는 메소드
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }
    // 위치 업데이트를 중지
    private void stopLocationUpdates() {
        // 객체가 초기화되었는지 확인
        if (fusedLocationClient != null && locationCallback != null) {
            // 위치 업데이트 중지
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    public void setCurrentMarker(LatLng latLng) {

        if (nonStartMarker == null) {   // 연한 트럭 마커를 한 번도 생성한 적 없을 때 (앱 최초 실행)
            // 마커 속성을 설정하는 객체
            if(!isInitialMarkerSet) {   // start 버튼을 누른 상태가 아니라면
                Log.d("hanaBBun", "연한 트럭 마커 찍기");
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(latLng)
                        .title("내 위치\n")
                        .snippet("GPS로 확인한 위치");

                // 마커 이미지 사이즈 조절
                //markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.location_pin));
                Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.clean_truck_non);
                int newWidth = 100;
                int newHeight = 100;
                // false : 크기 조절에 빠르고 효율. 약간 품질 저하될 수 있어도 대부분의 이미지 크기 조절 작업에 적합.
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, false);

                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));
                nonStartMarker = mMap.addMarker(markerOptions);
                // Z 인덱스 설정
                nonStartMarker.setZIndex(1.7f);
            }
        } else { // 연한 트럭 마커를 이전에 생성한 적 있을 때
            if(!isInitialMarkerSet) {
                Log.d("hanaBBun", "연한 트럭 마커 이동");
                nonStartMarker.setPosition(latLng);
            }
        }
    }


}