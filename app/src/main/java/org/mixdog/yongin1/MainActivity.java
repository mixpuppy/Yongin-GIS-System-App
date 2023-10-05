package org.mixdog.yongin1;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
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
        implements OnMapReadyCallback
        //위치 정보 인터페이스
        , GoogleApiClient.ConnectionCallbacks
        , GoogleApiClient.OnConnectionFailedListener
        , LocationListener {

    private NavController navController;

    //  로케이션 리퀘스트와 구글 API 선언
    protected GoogleApiClient mGoogleApiClient;
    // onConnected() 메소드를 사용할 수 있을때 사용하는 변수
    private FusedLocationProviderClient providerClient;
    // Http 통신을 위한 변수(다른곳 공유)
    private RequestQueue queue;
    // 지도 객체
    private GoogleMap mMap;
    // 지도 ui 객체
    private UiSettings mUi;
    // 위도 경도
    private double mLat, mLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // NavHostFragment 가져오기
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

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
            Log.d("mixpuppt", "MainActivity Oncreate else로 빠짐");
        }

        // 엑세스 권한 요청
        ActivityResultLauncher<String[]> requestPermissionLauncher
                = registerForActivityResult( // 권한 요청 결과 처리 시작
                new ActivityResultContracts.RequestMultiplePermissions()
                , isGranted -> { // java.util.Map<String, Boolean>
                    // Map형태로 되어있는 변수에서 stream을 통해 모두 권한이 부여됐는지 체크
                    if (isGranted.values().stream().allMatch(permission -> permission.booleanValue() == true)){
                        mGoogleApiClient.connect();
                    } else {
                        Toast.makeText(this, "권한 거부..", Toast.LENGTH_SHORT).show();
                    }
                });
//////////////////////////////////////////////////////////////////////////////
        // Google API 클라이언트 초기화
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API) // 위치 서비스 API 추가
                .addConnectionCallbacks(this) // Google API 연결 콜백 등록
                .addOnConnectionFailedListener(this) // Google API 연결 실패 콜백 등록
                .build();

        // 위치 권한 확인 및 요청
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // 위치 권한이 이미 허용되어 있는 경우
            mGoogleApiClient.connect(); // Google API 연결 시도
        } else {
            // 위치 권한을 요청하는 코드를 여기에 추가
        }
//////////////////////////////////////////////////////////
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        // R.id.mapView 이라는 프래그먼트에 Google Maps API 지도를 로드 (프레그먼트로 코드 옮김)
//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager() // 현재 활성화된 프래그먼트 매니저를 얻음
//                .findFragmentById(R.id.mapView); // XML 레이아웃에서 정의한 지도 프래그먼트의 ID가 R.id.map인 프래그먼트를 찾음
//        mapFragment.getMapAsync(this); // 비동기적으로 Google Maps API 지도를 로드

        providerClient = LocationServices.getFusedLocationProviderClient(this);
        //ViewModel 로 providerClient 를 전달 (Android Architecture Components 라이브러리에서 제공하는 ViewModelProvider를 사용)
        LocationViewModel viewModel = new ViewModelProvider(this).get(LocationViewModel.class);
        viewModel.setProviderClient(providerClient);
        //// ACCESS_FINE_LOCATION 권한 확인
        /**
         * checkSelfPermission(this, permission) : 현재 앱에서 특정 권한을 가지고 있는지 확인
         * PackageManager.PERMISSION_GRANTED : 권한이 부여되었을 때 상수 값
         * if -> 권한이 부여되지 않았을 때 (ACCESS_FINE_LOCATION의 값과 일치하지 않는 경우)
         * else -> 권한이 부여되었을 때
         */
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // ACCESS_FINE_LOCATION 권한 요청
            /**
             * requestPermissionLauncher : 안드로이드 권한 요청 API
             * launch() : 권한 요청을 시작
             * 사용자에게 권한 요청 대화상자가 표시되며, 사용자가 승인하거나 거부할수 있음
             */
            requestPermissionLauncher.launch(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION});
        }else {
            // 위치 제공자 준비하기
            mGoogleApiClient.connect();
        }

        // INTERNET 권한확인
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            // INTERNET 권한 요청
            /**
             * requestPermissionLauncher : 안드로이드 권한 요청 API
             * launch() : 권한 요청을 시작
             * 사용자에게 권한 요청 대화상자가 표시되며, 사용자가 승인하거나 거부할수 있음
             */
            requestPermissionLauncher.launch(new String[]{android.Manifest.permission.INTERNET});
        }


        /************
         * Volley
         ************/
        // 요청변수 초기화
        if(queue == null){
            queue = Volley.newRequestQueue(this);
        }
        // 요청 웹 주소
        String url = "http://218.234.109.166/test";
    }

    // 인터페이스 구현체
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d("mixpuppy", "onMapReady: 지도 설정 성공적");
        // 컨트롤러 올리기
        mUi = mMap.getUiSettings();

        // 나침반 추가
        mUi.setCompassEnabled(true);
        // 확대축소 컨트롤 추가
        mUi.setZoomControlsEnabled(true);
//        // Add a marker in Sydney and move the camera
//        LatLng choongang = new LatLng(37.5565844, 126.9451737);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(choongang));

    }

    // GoogleApiClient.ConnectionCallbacks 구현체
    // 위치 제공을 사용할 수 있는 상황일 때
    public void onConnected(Bundle bundle) {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && providerClient != null){
            providerClient.getLastLocation()
                    .addOnSuccessListener(
                            this,
                            new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    if (location != null) {
                                        mLat = location.getLatitude();
                                        mLng = location.getLongitude();
                                        Log.d("mixpuppy", "초기위치 | 위도 : " + mLat + ", " + "경도 : " + mLng);
                                        // 지도 중심 이동하기
                                        moveMap(mLat, mLng);
                                        Log.d("mixpuppy", "지도중심 이동 성공적");
                                    }
                                }
                            });
            mGoogleApiClient.disconnect();
        }
    }
    @Override
    public void onConnectionSuspended(int i) {
        // Google API 연결이 일시적으로 정지된 경우 실행되는 로직을 여기에 추가
        Log.d("mixpuppy", "구글 api 연결이 일시적으로 정지됨");
    }

    // GoogleApiClient.OnConnectionFailedListener 구현체
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Google API 연결에 실패한 경우 실행되는 로직을 여기에 추가
        Log.d("mixpuppy", "구글 api 연결이 실패됨");
    }

    // LocationListener 구현체
    @Override
    public void onLocationChanged(@NonNull Location location) {

    }

    private void moveMap(double latitude, double longitude){
//        if(mMap != null) {
        LatLng latLng = new LatLng(latitude, longitude);
        // 중심 좌표 생성
        CameraPosition positon = CameraPosition.builder()
                .target(latLng)
                .zoom(16f)
                .build();
        // 지도 중심 이동
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(positon));

        // 마커 옵션
        MarkerOptions markerOptions = new MarkerOptions();
        // 마커 이미지 사이즈 조절
        //markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.location_pin));
        Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.clean_truck_non);
        int newWidth = 100;
        int newHeight = 100;
        // false : 크기 조절에 빠르고 효율. 약간 품질 저하될 수 있어도 대부분의 이미지 크기 조절 작업에 적합.
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, false);

        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));

        markerOptions.position(latLng);
        markerOptions.title("MyLocation");
        // 마커 표시
        Marker myMarker = mMap.addMarker(markerOptions);

        // 마커에 Z 인덱스 설정
        myMarker.setZIndex(1.0f);
//        }
    }

    // mMap 변수의 fragment 사용위한 메소드
    public GoogleMap getMap() {
        return mMap;
    }
}