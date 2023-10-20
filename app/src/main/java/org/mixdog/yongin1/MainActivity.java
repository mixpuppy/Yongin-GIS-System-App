package org.mixdog.yongin1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

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
import org.mixdog.yongin1.permission.PermissionSupport;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback {
    // ↳ 지도가 준비되면 원하는 작업 수행하도록 구현

    // 프레그먼트 네비게이션 사용
    private NavController navController;

    // 지도 객체
    private GoogleMap mMap;
    // 지도 ui 객체
    private UiSettings mUi;
    // 위도 경도
    public static double mLat, mLng;

    // 위치 정보를 가진 객체. LocationViewModel에 set되어 Fragment와 위치 정보가 공유되게 된다.
    public static FusedLocationProviderClient fusedLocationClient;
    public static LocationRequest locationRequest;
    public static LocationCallback locationCallback;
    public static Marker nonStartMarker;
    // 시작버튼 활성화 여부 (true 시 활성화)
    public static boolean isInitialMarkerSet;

    //// 권한 처리 관련 클래스 선언
    private PermissionSupport permission;

    /**
     * 액티비티가 실행되고 처음 시작될 때 호출
     * @param savedInstanceState : 이전에 저장된 상태, 액티비티가 종료되었던 이전 상태정보 복원
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("hanaBBun", "--------------------앱 시작-------------------------");
        //locationViewModel = new ViewModelProvider(this).get(LocationViewModel.class);

        // PermissionSupport 초기화
        permission = new PermissionSupport(this, this);

        Log.d("hanaBBun", "onCreate 위치/알림 권한 허용 상태 : " + permission.locationPermissionGranted + "/" + permission.notificationPermissionGranted);
        Log.d("hanaBBun", "onCreate 위치/알림 권한 거절 횟수 : " + permission.locationDeniedCount + "/" + permission.notificationDeniedCount);
        // 권한 체크
        permissionCheck();

        // 앱 재실행을 위한 초기화 작업들
        isInitialMarkerSet = false;
        Log.d("hanaBBun", "locationRequest == null? : " + (locationRequest == null));

        // 앱을 종료하고 다시 실행했을 때도 지도 상 marker 기록이 남아있어서 초기화 시켜준다.
        nonStartMarker = null;

        // NavHostFragment 가져오기
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        // NavController 설정
        navController = navHostFragment.getNavController();

        // 지도 호출
//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
//                .findFragmentById(R.id.mapView);
        SupportMapFragment mapFragment = new SupportMapFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mapView, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);

        Log.d("mixpuppy", "MainActivity Oncreate if mapFragment != null 전");
//        if (mapFragment != null) {
//            Log.d("mixpuppy", "MainActivity Oncreate if 실행");
//            mapFragment.getMapAsync(this); // 비동기적으로 Google Maps API 지도를 로드
//        } else {
//            Log.d("mixpuppy", "MainActivity Oncreate else로 빠짐");
//        }

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
            if (mLat == 0.0 && mLng == 0.0) {
                // 초기 좌표와 줌 레벨 설정
                LatLng initLatLng = new LatLng(37.2214872, 127.2218612);
                float zoomLevel = 14.0f;

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initLatLng, zoomLevel));
            } else {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLat, mLng), 17.0f));
            }
        }
        // 나침반 추가
        mUi.setCompassEnabled(true);
        // 확대축소 컨트롤 추가
        mUi.setZoomControlsEnabled(true);
    }

    /**
     * 지도 중심 옮기기 - initializeLocation()에 의해 호출됨
     * @param latitude
     * @param longitude
     */
    private void moveMap(double latitude, double longitude){
        Log.d("hanaBBun", "MainActivity의 moveMap() 호출");
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

    /**
     * 현재 죄표 가져오기; 위치 서비스를 초기화하고 위치 업데이트를 요청하는 역할
     */
    private void initializeLocation() {

        // 위치 서비스를 관리하는 클라이언트 초기화
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
                    if(!isInitialMarkerSet) {
                        // 시작 버튼이 안 눌러져 있을 때만 MainFragment의 moveMap()이 호출되도록 한다.
                        moveMap(mLat, mLng);
                        if(nonStartMarker != null) {  // 앱 재시작 시 nullpointerexception 회피
                            nonStartMarker.setVisible(true);
                        }

                    } else {
                        if(nonStartMarker != null) {
                            nonStartMarker.setVisible(false);
                        }

                    }
                }
            }
        };
        // 위치 업데이트를 시작
        startLocationUpdates();

    }

    // 권한 체크
    private void permissionCheck() {
        // 권한 체크 후 리턴이 false로 들어오면
        if (!permission.checkPermissions()){
            // 권한 요청
            permission.requestPermission();
        } else {
            // 모든 권한이 이미 허용된 경우
            initializeLocation();
        }
    }

    /**
     * 권한 요청 작업 수행 후 시스템으로부터 권한 요청 결과 처리 위해 사용
     * ActivityCompat.requestPermissions() 를 호출한 뒤 권한 요청에 응답하면 자동 호출됨
     * -> 승인 여부에 대한 작업 수행 가능;
     *    승인된 권한을 사용해 원하는 작업하거나, 거부된 권한에 대해서 사용자에게 메시지 표시 및 대안 작업 제안
     * - int reqeustCode : 권한 요청을 구별하기 위한 요청 코드
     * - String[] permissions : 권한 요청에 포함된 권한 배열
     * - int[] grantResults : 승인 승인 결과 배열;
     *                        권한이 승인되면 PackageManager.PERMISSIONS_GRANTED = 1, 아니면 0
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // 권한 승인 여부를 확인하고, 승인되지 않은 경우에만 거부 횟수 증가시킴
        permission.permissionResult(requestCode, permissions, grantResults);
        permission.handlePermissionRequest();
        if(permission.locationPermissionGranted) {
            initializeLocation();
        }
    }


    /**
     * 위치 정보를 가진 fusedLocationClient 객체에 의해 위치 업데이트 요청
     */
    private void startLocationUpdates() {
        // 만약 권한이 부여되어 있다면
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            /**
             * locationRequest : 위치 업데이트의 주기와 우선순위를 설정한 객체
             * locationCallback : 위치 업데이트 이벤트를 처리하는 콜백 함수를 설정한 객체
             * null : Looper 를 지정하는데 사용. 이 경우 null로 지정하여 기본
             */
            // 위치 업데이트를 요청 시작 (트리거)
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    /**
     * 액티비티가 파괴될 때 호출되는 메소드 (강제 종료시킬 때)
     * : 메모리 누수 방지(배터리 소모 방지)를 위해 앱 종료가 되자 마자 위치 업데이트를 종료한다.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("hanabbun","onDestroy() 호출");
        stopLocationUpdates();
    }

    /////// 사용자가 안드로이드 앱을 종료할 때 일반적으로 onPause()나 onStop() 메서드가 호출됨!
    /**
     * 사용자가 현재 앱을 나가거나 다른 앱으로 전환할 때 호출
     * 앱이 화면에 보이지 않지만 여전히 실행 중이며 메모리에 상주하고 있음
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.d("hanaBBun", "onPause() 호출");
    }

    /**
     * 사용자가 앱을 더 보고 있지 않고 다른 앱으로 이동하거나 홈화면으로 나갈 때 호출
     * 백그라운드에서 실행될 수 있지만 화면에 보이지 않음.
     */
    @Override
    protected void onStop() {
        super.onStop();
        Log.d("hanaBBun", "onStop() 호출");
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
                        .snippet("청소 기록중이 아님");

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