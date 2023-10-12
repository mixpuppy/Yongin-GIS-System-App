package org.mixdog.yongin1.fragment;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationRequest;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

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
import org.mixdog.yongin1.Actions;
import org.mixdog.yongin1.CsvFile;
import org.mixdog.yongin1.LocationViewModel;
import org.mixdog.yongin1.MainActivity;
import org.mixdog.yongin1.MapUpdateService;
import org.mixdog.yongin1.R;
import org.mixdog.yongin1.dto.LocationDto;
import org.mixdog.yongin1.dto.NoiseDto;
import org.mixdog.yongin1.dto.VibrationDto;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class MapFragment extends Fragment
        implements OnMapReadyCallback  {

    //url port
//    private final String xyUrl = "http://172.30.1.55:8081/getXY";
//    private final String noiseUrl = "http://172.30.1.55:8081/getNoise";
//    private final String vibrationUrl = "http://172.30.1.55:8081/getVibration";

    private final String xyUrl = "http://172.30.1.18:80/gis/temp/gps";
    private final String noiseUrl = "http://172.30.1.18:80/gis/temp/noise";
    private final String vibrationUrl = "http://172.30.1.18:80/gis/temp/rpm";

    // 지도 객체
    private GoogleMap mMap;
    // 지도 ui 객체
    private UiSettings mUi;
    // 위도 경도
    private double mLat, mLng;


    // onConnected() 메소드를 사용할 수 있을때 사용하는 변수
    private FusedLocationProviderClient providerClient;

    // 버튼 선언; 포어그라운드 서비스에서 활용하기 위해 public으로 바꿔보았다.
    public Button startBtn;
    public Button stopBtn;
    private Button resetBtn;

    // Http 통신을 위한 변수
    private RequestQueue queue;

    // 인터벌을 위한 변수
    private Timer timerCall;
    TimerTask timerTask;

    // dialog 변수
    private String carNum;
    private View viewDialog;
    private View viewDropdown;

    //Marker 객체
    private Marker startMarker;
    private Marker myLocationMarker;
    private Marker endMarker;
    private List<Marker> startMarkerList = new ArrayList<>();
    private List<Marker> endMarkerList = new ArrayList<>();
    private List<Marker> markerList = new ArrayList<>();

    // CSV FILE PATH
    private String filePath;
    // CSV 파일명

    // CSV 파일 제작용 DTO LIST
    private List<LocationDto> locationDtos;
    private List<NoiseDto> noiseDtos;
    private List<VibrationDto> vibrationDtos;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    /////////////////////////////////////////전역변수 설정 끝////////////////////////////////////////
    public MapFragment() {
        // Required empty public constructor
    }

    // 후속 프래그먼트 호출 시 사용 (현재 사용 x)
    public static MapFragment newInstance(String param1, String param2) {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    // 프래그먼트 초기화
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // 부모 클래스 정의된 초기화 작업
        super.onCreate(savedInstanceState);
        Log.d("mixpuppy", "MapFragment onCreate 성공실행");


        // RequestQueue 초기화
        queue = Volley.newRequestQueue(requireContext());

        // 타이머 초기화
        timerCall = new Timer();

        // csv 저장 path 설정
        /**
         * (프레그먼트에서는 직접 context를 상속받지 않아 별도로 getActivity() 를 호출해야 함
         */
        filePath = getActivity().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath();

        // 각 ArrayList 초기화
        initDtoList();

        //프래그먼트 간에 데이터를 전달용
//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }
    }

    /**
     * 프레그먼트 생명주기 중 하나, 프레그먼트가 화면에 표시될 때 자동 호출됨 (layout xml 참조 메소드)
     * 프래그먼트의 UI를 초기화하고 화면에 표시할 뷰 반환
     * - @SuppressLint("MissingInflatedId") : MissingInflatedId 경고에 대한 무시
     */
    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // ViewModel 에서 위치정보 객체 참조
        // ; 앱 데이터 및 로직 관리 위한 ViewModel, providerClient 초기화
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

        ////// 일단 무시 -------------------------
        // 포어그라운드 서비스 관련 코드; viewModel로부터 action 값 얻어오기
        viewModel.getAction().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String action) {
                if (Actions.start.equals(action)){
                    startBtn.performClick();
                } else if (Actions.end.equals(action)) {
                    stopBtn.performClick();
                }
            }
        });

        // Btns를 레이아웃에서 찾기
        startBtn = rootView.findViewById(R.id.startBtn);
        stopBtn = rootView.findViewById(R.id.stopBtn);
        resetBtn = rootView.findViewById(R.id.resetBtn);

        /**
         * startBtn 클릭 이벤트 리스너
         */
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // timerTask가 취소되지 않은 상태면 취소하고 시작
                if(timerTask != null){
                    timerTask.cancel();
                }
                Log.d("mixpuppy", "주행 시작 버튼을 눌렀습니다");

                // 현재 위치 얻기
                getLastLocation();

                /**
                 * Spinner 위젯을 사용한 드롭다운 목록 만들기 - 차량 번호 선택
                 * ; 차량 번호를 선택하면 해당 선택을 처리하는 리스너 호출
                 *   선택 완료 버튼 클릭 시 선택된 차량 번호를 처리하는 로직 이어감
                 */
                // 차량 드롭다운 목록에서 선택할 차량 번호 목록 준비
                List<String> carNums = Arrays.asList("103하2414", "114하6585");

                // dialog 상자 생성 시작; res/values/styles.xml 파일에서 스타일 지정해줌
                AlertDialog.Builder dialog = new AlertDialog.Builder(requireContext(), R.style.AlertDialogCustom);
                dialog.setTitle("차량 번호 선택");

                // View.inflate 로 dialog 레이아웃을 뷰로 구현
                viewDialog = (View) View.inflate(requireContext(), R.layout.dropdown_layout, null);
                // setView 함수로 뷰를 dialog 변수에 전달
                dialog.setView(viewDialog);

                // 드롭다운 목록에서 선택한 항목 가져오기
                Spinner spinner = viewDialog.findViewById(R.id.spinner);
                // 1. requireContext() : 앱의 정보를 담고 있음
                // 2. layout id. 기본으로 제공되는 simple_spinner_item
                // 3. 내가 작성한 차 목록 배열 집어넣기
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_spinner_item, carNums);
                spinner.setAdapter(adapter);

                // 드롭다운 목록에서 선택 항목 가져오기
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        // 선택된 차량 번호를 carNum 변수 값으로 할당하기
                        carNum = carNums.get(position);
                        // 선택한 차량 번호를 활용한 원하는 작업 수행 가능 ...
                        Log.d("hanaBBun", "선택된 차량 번호 : " + carNum);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // 아무것도 선택되지 않았을 때?
                    }
                });

                // 선택 완료 버튼 추가
                dialog.setPositiveButton("선택 완료",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                Log.d("hanaBBun", "차량 번호 입력 완료");

                                // 주행시작을 알리는 알림창 나타남
                                Intent intent = new Intent(requireContext(), MapUpdateService.class);
                                intent.setAction(Actions.START_FOREGROUND);
                                requireContext().startService(intent);

                                // 메인엑티비티 스테틱 전역변수 버튼활성화 적용
                                MainActivity.isInitialMarkerSet=true;
                                //MainActivity.nonStartMarker.remove();
                                MainActivity.nonStartMarker.setVisible(false);
                                if (myLocationMarker != null) {
                                    myLocationMarker.setVisible(true);
                                }
                                // 출발지점 마커 설정
                                startMarkerSetting();

                                // 반복할 코드
                                timerTask = new TimerTask() {
                                    // TimerTask의 run() 메소드는 백그라운드 스레드에서 실행된다!
                                    // 그래서 getLastLocation() 과 같이 UI 관련 작업을 수행하려면 메인 스레드에서 실행되도록 설정해줘야!
                                    @Override
                                    public void run() {
                                        // 위도경도 정보 얻기
                                        //getLastLocation();
                                        // UI 스레드(메인 스레드)에서 작업 수행을 위한 메소드
                                        // : 안드로이드에서는 UI 요소에 접근 및 조작 작업이 주로 메인 스레드에서 수행되어야 하기 때문에
                                        //   백그라운드 스레드에서 UI와 관련된 작업을 수행할 경우
                                        //   runOnUiThread를 사용하여 메인 스레드에서 해당 작업을 실행한다.
                                        // ... 근데 이건 액티비티의 메소드임으로 여기선 그냥 못 쓰기 때문에 앞에 getActivity() 붙이기!
                                        getActivity().runOnUiThread(new Runnable() {
                                           @Override
                                           public void run() {
                                               getLastLocation();
                                           }
                                        });

                                        LatLng latLng = new LatLng(mLat, mLng);
                                        //routeMarkerSetting(latLng);
                                        Log.d("mixpuppy", "타이머 위경도 잘찍혔나?");
                                        Log.d("mixpuppy", "위경도" + mLat + "/" + mLng);
                                        // Json 데이터 보내기
                                        sendXYJsonData(xyUrl);
                                        sendNoiseJsonData(noiseUrl);
                                        sendVibrationData(vibrationUrl);
                                    }
                                };
                                // 기록 시작 (10초로 설정)
                                timerCall.schedule(timerTask,0,10000);

                                // 버튼 활성화/비활성화
                                startBtn.setVisibility(View.INVISIBLE);
                                stopBtn.setVisibility(View.VISIBLE);
                                startBtn.setEnabled(false);
                                stopBtn.setEnabled(true);
                                resetBtn.setEnabled(false);
                                resetBtn.setVisibility(View.INVISIBLE);
                            }
                });

                dialog.setNegativeButton("선택 취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d("mixpuppy", "다이얼로그 취소버튼이 눌렸음");
                    }
                });

                dialog.show();
            }
        });

        /**
         * stopBtn 클릭 이벤트 리스너
         */
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(requireContext(), MapUpdateService.class);
                intent.setAction(Actions.STOP_FOREGROUND);
                requireContext().startService(intent);

                Log.d("mixpuppy", "정지버튼이 눌렸음");
                // 메인엑티비티 스테틱 전역변수 버튼활성화 적용
                MainActivity.isInitialMarkerSet=false;
                MainActivity.nonStartMarker.setVisible(true);
                // stop 버튼을 눌렀을 때 연한 트럭이 start 버튼 누르기 전의 위치에 처음 찍혀있어서 아래 코드를 추가해봤다.
                MainActivity.nonStartMarker.setPosition(new LatLng(mLat, mLng));
                myLocationMarker.setVisible(false);

                endMarkerSetting();

                // csv 파일 생성
                makeLocationCSV();
                makeNoiseCSV();
                makeVibrationCSV();

                // dto list 초기화
                initDtoList();

                if(timerTask != null){
                    timerTask.cancel();
                    Log.d("mixpuppy", "타이머 정지");
                } else  {
                    Log.d("mixpuppy", "정지 눌렀으나 timerTask 가 null 로 인식");
                }
                // 버튼 활성화/비활성화
                startBtn.setVisibility(View.VISIBLE);
                stopBtn.setVisibility(View.INVISIBLE);
                startBtn.setEnabled(true);
                stopBtn.setEnabled(false);
                resetBtn.setEnabled(true);
                resetBtn.setVisibility(View.VISIBLE);
            }
        });

        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("hanaBBun", "map reset 버튼 클릭");
                for (Marker marker : markerList) {
                    marker.remove();
                }
                markerList.clear();

                for (Marker marker : startMarkerList) {
                    marker.remove();
                }
                startMarkerList.clear();

                for (Marker marker : endMarkerList) {
                    marker.remove();
                }
                endMarkerList.clear();
            }
        });
        // rootView를 반환합니다.
        return rootView;
    }

    //getMapAsync() 호출 시 자동실행 (맵 준비상태가 되면 실행)
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d("hanaBBun", "MapFragment의 onMapReady : " + googleMap);
        mMap = googleMap;
        Log.d("mixpuppy", "MapFragment의 onMapReady: 지도 설정 완료");
        // 컨트롤러 올리기
        mUi = mMap.getUiSettings();

        // 나침반 추가
        mUi.setCompassEnabled(true);
        // 확대축소 컨트롤 추가
        mUi.setZoomControlsEnabled(true);
    }


    ////////////////////////////////////////정의 메소드//////////////////////////////////////////////

    /**
     * 실시간 위치 좌표값 얻기 메소드
     */
    private void getLastLocation() {
        if(providerClient == null) {
            Log.d("hanaBBun", "MapFragment의 getLastLocation에서 providerClient가 null이다ㅠㅠ");
        }
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && providerClient != null) {
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
                                Log.d("mixpuppy", "getLastLocation - latitude:" + location.getLatitude());
                                Log.d("mixpuppy", "getLastLocation - longitude:" + location.getLongitude());
                                // 지도 중심 이동하기
                                moveMap(mLat, mLng);
                                Log.d("mixpuppy", "getLastLocation으로 지도 중심 이동 끝");
                            }
                        }
                    });
        }else {
            // 위치 권한 없을때
            Log.d("mixpuppy", "target 진입 못햇쪙 ㅜㅜ");
        }
    }

    /**
     * 현재 실시간 위치로 지도 중심을 옮기는 메소드
     * @param latitude
     * @param longitude
     */
    private void moveMap(double latitude, double longitude) {
        if(mMap != null) {
            Log.d("mixpuppy", "MapFragment 의 moveMap 메소드 동작");
            LatLng latLng = new LatLng(latitude, longitude);
            // 중심 좌표 생성
            CameraPosition position = CameraPosition.builder()
                    .target(latLng)
                    .zoom(17f) //  0(세계 지도) ~ 20(건물 수준)
                    .build();
            // 지도 중심 이동
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));

            // 메인 마커 셋팅 메소드 (진한 트럭 아이콘으로 현위치 나타내기)
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
            Marker marker = mMap.addMarker(markerOptions);
            markerList.add(marker);
            Log.d("mixpuppy", "MapFragment 의 moveMap 메소드 끝");
        } else {
            Log.d("mixpuppy", "mMap 이 null 이네!ㅜㅜ");
        }
    }

    public void CurrentMarkerSetting(LatLng latLng) {
        if (myLocationMarker == null) {
            Log.d("hanaBBun", "진한 트럭 마커 찍기");

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
            Log.d("HanaBBun", "진한 트럭 마커 이동");
            myLocationMarker.setPosition(latLng);
        }

    }

    private void startMarkerSetting() {
        Log.d("hanaBBun", "startMarkerSetting");
        if(mMap != null) {
            LatLng latLng = new LatLng(mLat, mLng);
            // 마커 옵션
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)
                    .title("MyStartLocation");

            // 마커 이미지 사이즈 조절
            //markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.location_pin));
            Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.start_marker);
            int newWidth = 100;
            int newHeight = 100;
            // false : 크기 조절에 빠르고 효율. 약간 품질 저하될 수 있어도 대부분의 이미지 크기 조절 작업에 적합.
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, false);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));

            // 마커 표시
            //startMarker = mMap.addMarker(markerOptions);
            Marker startMarker = mMap.addMarker(markerOptions);
            Log.d("hanaBBun", "markerSetting - startMarker의 latitude : " + startMarker.getPosition().latitude);
            Log.d("hanaBBun", "markerSetting - startMarker의 longitude : " + startMarker.getPosition().longitude);
            startMarkerList.add(startMarker);

            // 마커에 Z 인덱스 설정
            startMarker.setZIndex(1.2f);
        } else {
            Log.d("mixdog", "googleMap 이 널이여서 마커를 못찍음");
        }
    }

    /*private void routeMarkerSetting(LatLng latLng) {
        if (mMap != null) {
            // 경로 흔적 마커
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("MyMovedLocation");

            // 이미지 세팅
            Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pointer_route);
            int newWidth = 20;
            int newHeight = 20;
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, false);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));

            // 마커 표시
            Marker marker = mMap.addMarker(markerOptions);
            markerList.add(marker);
            Log.d("mixpuppy", "MapFragment 의 moveMap 메소드 끝");
        } else {
            Log.d("mixpuppy", "mMap 이 null 이네!ㅜㅜ");
        }
    }*/

    private void endMarkerSetting() {
        Log.d("hanaBBun", "endMarkerSetting");
        if(mMap != null) {
            LatLng latLng = new LatLng(mLat, mLng);
            // 마커 옵션
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("MyEndLocation");

            // 마커 이미지 사이즈 조절
            //markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.location_pin));
            Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.end_marker);
            int newWidth = 100;
            int newHeight = 100;
            // false : 크기 조절에 빠르고 효율. 약간 품질 저하될 수 있어도 대부분의 이미지 크기 조절 작업에 적합.
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, false);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));

            Marker endMarker = mMap.addMarker(markerOptions);
            endMarkerList.add(endMarker);
            //endMarker = mMap.addMarker(markerOptions);
            Log.d("hanaBBun", "endMarker의 latitude : " + endMarker.getPosition().latitude);
            Log.d("hanaBBun", "endMarker의 longitude : " + endMarker.getPosition().longitude);


            // 마커에 Z 인덱스 설정
            endMarker.setZIndex(1.1f);
        } else {
            Log.d("mixdog", "googleMap 이 널이여서 마커를 못찍음");
        }
    }

    // list 초기화 메소드
    public void initDtoList() {
        locationDtos = new ArrayList<>();
        noiseDtos = new ArrayList<>();
        vibrationDtos = new ArrayList<>();
    }

    // JSON데이터 서버에 전송
    public void sendXYJsonData(String url){
        // 좌표 JSON 데이터 전송
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();
        try {
            // JSON 객체 생성
            final JSONObject object = new JSONObject();
            object.put("carNum", carNum);
            object.put("lat", mLat);
            object.put("lon", mLng);
            object.put("date", date);
            object.put("time", time);
            Log.d("json_log", object.toString());

            // list 에 담을 dto 객체 생성
            LocationDto locationDto = new LocationDto(carNum, date, time, mLat, mLng);
            locationDtos.add(locationDto);

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

    public void sendNoiseJsonData(String url){
        // 소음 JSON 데이터 전송 (now() 메소드를 사용하기 위해 minSDK 버전 26 상향 조정)
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();
        // 임의 랜덤 소음값
        // noise 값을 60에서 100 사이의 랜덤 값으로 생성
        double minNoise = 60.00;
        double maxNoise = 100.00;
        double noise = minNoise + (maxNoise - minNoise) * new Random().nextDouble();
        // 소숫점 2자리까지 포맷팅
        DecimalFormat df = new DecimalFormat("#.##");
        String formattedNoise = df.format(noise);
        try {
            // JSON 객체 생성
            final JSONObject object = new JSONObject();
            object.put("carNum", carNum);
            object.put("noise", formattedNoise);
            object.put("date", date);
            object.put("time", time);
            Log.d("json_log", object.toString());

            // list 에 담을 dto 객체 생성
            NoiseDto noiseDto = new NoiseDto(carNum, date, time, formattedNoise);
            noiseDtos.add(noiseDto);

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
    public void sendVibrationData(String url) {
        // 진동 JSON 데이터 전송 (now() 메소드를 사용하기 위해 minSDK 버전 26 상향 조정)
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();
        // 임의 랜덤 진동값
        int minVibration = 1000;
        int maxVibration = 2500;
        int vibration = new Random().nextInt(maxVibration - minVibration + 1) + minVibration;

        try {
            // JSON 객체 생성
            final JSONObject object = new JSONObject();
            object.put("carNum", carNum);
            object.put("rpm", vibration);
            object.put("date", date);
            object.put("time", time);
            Log.d("json_log", object.toString());

            // list 에 담을 dto 객체 생성
            VibrationDto vibrationDto = new VibrationDto(carNum, date, time, vibration);
            vibrationDtos.add(vibrationDto);

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
                    }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap headers = new HashMap();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };
            // 전송
            queue.add(jsonRequest);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    //csv 파일 생성 메소드
    public void makeLocationCSV() {
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();
        CsvFile csvFile = new CsvFile(filePath);
        String fileName = carNum + "_" + date.toString() + "_" + time.toString() + "locationData.csv";
        csvFile.locationDTOListToCsv(fileName, locationDtos);
        Log.d("csvfile", "저장소 경로 : " + filePath);
        Log.d("csvfile", "location csv 저장 성공");
    }
    public void makeNoiseCSV() {
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();
        CsvFile csvFile = new CsvFile(filePath);
        String fileName = carNum + "_" + date.toString() + "_" + time.toString() +"noiseData.csv";
        csvFile.noiseDTOListToCsv(fileName, noiseDtos);
        Log.d("csvfile", "저장소 경로 : " + filePath);
        Log.d("csvfile", "noise csv 저장 성공");
    }
    public void makeVibrationCSV() {
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();
        CsvFile csvFile = new CsvFile(filePath);
        String fileName = carNum + "_" + date.toString() + "_" + time.toString() +"vibrationData.csv";
        csvFile.vibrationDTOListToCsv(fileName, vibrationDtos);
        Log.d("csvfile", "저장소 경로 : " + filePath);
        Log.d("csvfile", "vibration csv 저장 성공");
    }


}