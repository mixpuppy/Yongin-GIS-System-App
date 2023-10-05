package org.mixdog.yongin1.fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationRequest;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
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

import org.json.JSONException;
import org.json.JSONObject;
import org.mixdog.yongin1.LocationViewModel;
import org.mixdog.yongin1.MainActivity;
import org.mixdog.yongin1.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class MapFragment extends Fragment
        implements OnMapReadyCallback  {

    // 지도 객체
    private GoogleMap mMap;
    // 지도 ui 객체
    private UiSettings mUi;
    // 위도 경도
    private double mLat, mLng;
    // 구글 api
    protected GoogleApiClient mGoogleApiClient;
    // 로케이션 리퀘스트
    protected LocationRequest mLocationRequest;
    // onConnected() 메소드를 사용할 수 있을때 사용하는 변수
    private FusedLocationProviderClient providerClient;

    // 버튼 선언
    private Button startBtn;
    private Button stopBtn;
    // Http 통신을 위한 변수
    private RequestQueue queue;
    // 인터벌을 위한 변수
    private Timer timerCall;
    TimerTask timerTask;
    // dialog 변수
    private String carNum;
    private View viewDialog;

    //Marker 객체
    private Marker myLocationMarker;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public MapFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static MapFragment newInstance(String param1, String param2) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // 부모 클래스 정의된 초기화 작업
        super.onCreate(savedInstanceState);
        Log.d("mixpuppy", "MapFragment onCreate 성공실행");

        // RequestQueue 초기화
        queue = Volley.newRequestQueue(requireContext());

        // 타이머 초기화
        timerCall = new Timer();
        //프래그먼트 간에 데이터를 전달용
//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }

    }

    // layout xml 참조 메소드 (자동 호출)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // ViewModel 에서 위치정보 객체 참조
        LocationViewModel viewModel = new ViewModelProvider(requireActivity()).get(LocationViewModel.class);
        providerClient = viewModel.getProviderClient();

        Log.d("mixpuppy", "MapFragment onCreateView 성공실행");
        // R.id.mapView 이라는 프래그먼트에 Google Maps API 지도를 로드
        SupportMapFragment mapFragment = (SupportMapFragment) requireActivity().getSupportFragmentManager()
                .findFragmentById(R.id.mapView);
        Log.d("mixpuppy", "mapFragment onCreateView if문 전");
        if (mapFragment != null) {
            Log.d("mixpuppy", "mapFragment onCreateView 실행" + this + "mapFragment : " + mapFragment);
            mapFragment.getMapAsync(this); // 비동기적으로 Google Maps API 지도를 로드
        } else {
            Log.d("mixpuppy", "mapFragment 가 null 로 빠짐");
        }
        // 프래그먼트의 레이아웃을 인플레이트.
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);

        // startBtn과 stopBtn을 레이아웃에서 찾기
        startBtn = rootView.findViewById(R.id.startBtn);
        stopBtn = rootView.findViewById(R.id.stopBtn);

        // startBtn에 클릭 리스너를 설정.
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // timerTask가 취소되지 않은 상태면 취소하고 시작
                if(timerTask != null){
                    timerTask.cancel();
                }
                Log.d("mixpuppy", "시작버튼이 눌렸음");
                // dialog 상자 생성 시작
                AlertDialog.Builder dialog = new AlertDialog.Builder(requireContext());
                dialog.setTitle("차량 번호");
                // View.inflate 로 dialog 레이아웃을 뷰로 구현
                viewDialog = (View) View.inflate(requireContext(), R.layout.dialog, null);
                // setView 함수로 뷰를 dialog 변수에 전달
                dialog.setView(viewDialog);

                // 차랑정보 입력 dialog 버튼
                dialog.setPositiveButton("입력",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                EditText carNumBox = (EditText) viewDialog.findViewById(R.id.car_num);
                                carNum = String.valueOf(carNumBox.getText());
                                Log.d("mixpuppy", "입력된 차량넘버 : " + carNum);
                                // 반복할 코드
                                timerTask = new TimerTask() {
                                    @Override
                                    public void run() {

                                        // 위도경도 정보 얻기
                                        getLastLocation();
                                        Log.d("mixpuppy", "타이머 위경도 잘찍혔나?");
                                        Log.d("mixpuppy", "위경도" + mLat + "/" + mLng);
                                        // Json 데이터 보내기
                                        sendJsonData("http://218.234.109.166/test");
                                    }
                                };
                                // 기록 시작 (10초로 설정)
                                timerCall.schedule(timerTask,0,5);
                                // 버튼 활성화/비활성화
                                stopBtn.setEnabled(true);
                                startBtn.setEnabled(false);
                            }
                        });
                dialog.setNegativeButton("취소",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Log.d("mixpuppy", "다이얼로그 취소버튼이 눌렸음");
                            }
                        });
                dialog.show();
            }
        });

        // stopBtn에 클릭 리스너를 설정합니다.
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("mixpuppy", "정지버튼이 눌렸음");
                if(timerTask != null){
                    timerTask.cancel();
                    Log.d("mixpuppy", "타이머 정지");
                } else  {
                    Log.d("mixpuppy", "정지 눌렀으나 timerTask 가 null 로 인식");
                }
                // 버튼 활성화/비활성화
                startBtn.setEnabled(true);
                stopBtn.setEnabled(false);
            }
        });

        // rootView를 반환합니다.
        return rootView;
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d("asdfqwerasdf", "아무거나" + googleMap);
        mMap = googleMap;
        Log.d("mixpuppy", "MapFragment의 onMapReady: 지도 설정 완료");
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

    // 좌표 얻기 메소드
    private void getLastLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && providerClient != null) {
            Log.d("mixpuppy", "getLastLocation 성공진입");
            // 위치정보 얻기
            providerClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                Log.d("mixpuppy", "로케이션 따오기 메소드 성공 진입");
                                mLat = location.getLatitude();
                                mLng = location.getLongitude();
                                Log.d("mixpuppy", "latitude:" + location.getLatitude());
                                Log.d("mixpuppy", "longitude:" + location.getLongitude());
                                // 지도 중심 이동하기
                                moveMap(mLat, mLng);
                                Log.d("mixpuppy", "해치웠나?!");
                            }
                        }
                    });
        }else {
            // 위치 권한 없을때
            Log.d("mixpuppy", "target 진입 못햇쪙 ㅜㅜ");
        }
    }
    // 이동하는 메소드
    private void moveMap(double latitude, double longitude){
        if(mMap != null) {
            Log.d("mixpuppy", "MapFragment 의 moveMap 메소드 동작");
            LatLng latLng = new LatLng(latitude, longitude);
            // 중심 좌표 생성
            CameraPosition positon = CameraPosition.builder()
                    .target(latLng)
                    .zoom(16f) //  0(세계 지도) ~ 20(건물 수준)
                    .build();
            // 지도 중심 이동
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(positon));

            // 메인 마커 셋팅 메소드(현위치)
            CurrentMarkerSetting(latLng);
            // 경로 흔적 마커
            MarkerOptions markerOptions = new MarkerOptions();
            Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pointer_route);
            int newWidth = 20;
            int newHeight = 20;
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, false);

            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));

            markerOptions.position(latLng);
            markerOptions.title("MyLocation");
            // 마커 표시
            mMap.addMarker(markerOptions);
            Log.d("mixpuppy", "MapFragment 의 moveMap 메소드 끝");
        } else {
            Log.d("mixpuppy", "mMap 이 null 이네!ㅜㅜ");
        }
    }

    public void CurrentMarkerSetting(LatLng latLng) {
        if (myLocationMarker == null) {
            Log.d("MyApp", "중심 마커 찍기");
            // 마커 속성을 설정하는 객체
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("내 위치\n");
            markerOptions.snippet("GPS로 확인한 위치");

            // 마커 이미지 사이즈 조절
            //markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.location_pin));
            Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.clean_truck);
            int newWidth = 100;
            int newHeight = 100;
            // false : 크기 조절에 빠르고 효율. 약간 품질 저하될 수 있어도 대부분의 이미지 크기 조절 작업에 적합.
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, false);

            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));
            myLocationMarker = mMap.addMarker(markerOptions);
            // Z 인덱스 설정
            myLocationMarker.setZIndex(1.1f);
        } else {
            Log.d("MyApp", "마커 이동");
            myLocationMarker.setPosition(latLng);
        }

    }

    // JSON데이터 서버에 전송
    public void sendJsonData(String url){
        // JSON 데이터 전송
        try {
            // JSON 객체 생성
            final JSONObject object = new JSONObject();
            object.put("carNum", carNum);
            object.put("latitude", mLat);
            object.put("longitude", mLng);
            // 전송 준비
            JsonObjectRequest jsonRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    object,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d("mixpuppy", "json onResponse: ");
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d("mixpuppy", "json onErrorResponse: " + error);
                        }
                    }){
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap headers = new HashMap();
                    headers.put("Content-Type","application/json");
                    return headers;
                }
            };
            // 전송
            queue.add(jsonRequest);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }




}