package org.mixdog.yongin1.fragment;


import static org.mixdog.yongin1.MainActivity.isInitialMarkerSet;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
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

import org.json.JSONArray;
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
import org.mixdog.yongin1.utils.NetworkUtils;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
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

    private final String xyUrl = "http://172.30.1.22:80/gis/temp/gps";
    private final String noiseUrl = "http://172.30.1.22:80/gis/temp/noise";
    private final String vibrationUrl = "http://172.30.1.22:80/gis/temp/rpm";
    private final String requestCarNumUrl = "http://172.30.1.22:80/gis/car";
    private final String startSendUrl = "http://172.30.1.61:80/gis/start";
    private final String stopSendUrl = "http://172.30.1.61:80/gis/stop";

    // DB 차량 넘버 조회
    private List<String> serverCarNums;

    // 지도 객체
    private GoogleMap mMap;
    // 지도 ui 객체
    private UiSettings mUi;
    // 위도 경도
    private double mLat, mLng;

    private FusedLocationProviderClient providerClient;

    // 버튼 선언; 포어그라운드 서비스에서 활용하기 위해 public 선언
    public static Button startBtn;
    public static Button stopBtn;
    private Button resetBtn;

    // 서비스에 버튼 클릭 상태를 전달하기 위한 intent 객체
    Intent viewBtnIntent;

    // Http 통신을 위한 변수; 네트워크 요청 및 응답 처리
    private RequestQueue queue;

    // 인터벌을 위한 변수
    private Timer timerCall;
    TimerTask timerTask;

    // dialog 변수
    private String carNum;
    private View viewDialog;

    //Marker 객체
    private Marker myLocationMarker;
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

    /////////////////////////////////////////전역변수 설정 끝////////////////////////////////////////
    public MapFragment() {
        // Required empty public constructor
    }

    // 후속 프래그먼트 호출 시 사용 (현재 사용 x)
    public static MapFragment newInstance(String param1, String param2) {
        MapFragment fragment = new MapFragment();

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
        // 차량넘버 초기화
        serverCarNums = new ArrayList<>();

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

        /*
        ViewModel 에서 위치정보 객체 참조
         : 앱 데이터 및 로직 관리 위한 ViewModel, providerClient 초기화
         - ViewModelProvider : 액티비티 또는 프래그먼트와 같은 구성요소에서 뷰 모델 생성 및 관리하는 데 사용
                              뷰모델을 생성하고 이미 생성된 뷰모델을 반환하여 데이터 유지 및 관리하도록 도와줌
         */

        // owner 자리에 requireActivity() 대신 this를 적어줬는데 제대로 된 것 맞을까.
        // this는 현재 액티비티 또는 프래그먼트 / requireAcivity()는 MainActivity인 듯.
        LocationViewModel viewModel = new ViewModelProvider(requireActivity()).get(LocationViewModel.class);
        providerClient = viewModel.getProviderClient();

        // R.id.mapView 이라는 프래그먼트에 Google Maps API 지도를 로드
        SupportMapFragment mapFragment = (SupportMapFragment) requireActivity().getSupportFragmentManager()
                .findFragmentById(R.id.mapView);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this); // 비동기적으로 Google Maps API 지도를 로드
        } else {
            Log.d("mixpuppy", "mapFragment 가 null 로 빠짐");
        }
        // 프래그먼트의 레이아웃을 인플레이트
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);

        // Btns를 레이아웃에서 찾기
        startBtn = rootView.findViewById(R.id.startBtn);
        stopBtn = rootView.findViewById(R.id.stopBtn);
        resetBtn = rootView.findViewById(R.id.resetBtn);

        // 원격 서버 DB 에서 차량번호 정보 조회 요청
        requestCarNum(requestCarNumUrl);

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
                //getLastLocation();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getLastLocation();
                    }
                });

                /**
                 * Spinner 위젯을 사용한 드롭다운 목록 만들기 - 차량 번호 선택
                 * ; 차량 번호를 선택하면 해당 선택을 처리하는 리스너 호출
                 *   선택 완료 버튼 클릭 시 선택된 차량 번호를 처리하는 로직 이어감
                 */
                // 차량 드롭다운 목록에서 선택할 차량 번호 목록 준비 - 연습용
//                List<String> carNums = Arrays.asList("103하2414", "114하6585");

                // dialog 상자 생성 시작
                AlertDialog.Builder carDialogBuilder = new AlertDialog.Builder(requireContext(), R.style.AlertDialogCustom);
                carDialogBuilder.setTitle("차량 번호 선택");

                // View.inflate 로 dialog 레이아웃을 뷰로 구현
                viewDialog = (View) View.inflate(requireContext(), R.layout.dropdown_layout, null);

                // 드롭다운 목록에서 선택한 항목 가져오기
                Spinner spinner = viewDialog.findViewById(R.id.spinner);

                // setView 함수로 뷰를 dialog 변수에 전달
                carDialogBuilder.setView(viewDialog);

                // 1. requireContext() : 앱의 정보를 담고 있음
                // 2. layout id. 기본으로 제공되는 simple_spinner_item
                // 3. 내가 작성한 차 목록 배열 집어넣기
//                ArrayAdapter<String> adapter = new ArrayAdapter<>(
//                        requireContext(), android.R.layout.simple_spinner_item, carNums);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_spinner_item, serverCarNums);
                spinner.setAdapter(adapter);

                // 드롭다운 목록에서 선택 항목 가져오기
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        // 선택된 차량 번호를 carNum 변수 값으로 할당하기
//                        carNum = carNums.get(position);
                        carNum = serverCarNums.get(position);
                        // 선택한 차량 번호를 활용한 원하는 작업 수행 가능 ...
                        Log.d("hanaBBun", "선택된 차량 번호 : " + carNum);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // 아무것도 선택되지 않았을 때?
                        // ; 차량 목록을 조회만 할 수 있다면, 목록의 첫번째 차량이 기본으로 선택된 상태기 때문에
                        // 사용자 의지로 아무 것도 선택하지 않고 주행 시작할 수는 없다.
                    }
                });

                // 선택 완료 버튼 추가
                carDialogBuilder.setPositiveButton("선택 완료",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                // 현재 위치 얻기
                                // 왜 여기에서 좌표값을 얻으면 출발 마커가 제대로 안 찍히는지 의문이다.
                                // 여기 대신 startBtn.onClick 하자마자 되도록 했다.
                                //getLastLocation();

                                Log.d("hanaBBun", "차량 번호 입력 완료");

                                // 주행시작을 알리는 알림창 나타남
                                Intent intent = new Intent(requireContext(), MapUpdateService.class);
                                intent.setAction(Actions.START_FOREGROUND);
                                requireContext().startService(intent);

                                // viewModel로 액션 상태 데이터를 서비스로 전달할랬는데 실패
                                // 서비스와 뷰 사이에 데이터 공유하기 위해서 intent를 사용하는 거다.
                                //viewModel.setAction(Actions.start);
                                //Log.d("hanaBBun", "viewModel의 Action : " + viewModel.getAction());

                                viewBtnIntent = new Intent(getActivity(), MapUpdateService.class);
                                viewBtnIntent.putExtra("action", Actions.start);
                                getContext().startService(viewBtnIntent);


                                // 메인엑티비티 스테틱 전역변수 버튼활성화 적용
                                isInitialMarkerSet=true;
                                Log.d("hanaBBun", "isInitialMarkerSet : " + isInitialMarkerSet);
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
                                                // 지도 중심 이동 및 마커 찍기
                                                moveMap(mLat, mLng);
                                            }
                                        });

                                        Log.d("mixpuppy", "타이머 위경도 잘찍혔나?");
                                        Log.d("mixpuppy", "timer 위경도 : " + mLat + "/" + mLng);
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

//                                // 원격 서버에 시작했음을 알리는 트리거
//                                NetworkUtils.sendGETRequest(startSendUrl, requireContext(),
//                                        (response, error) -> {
//                                            if (response != null) {
//                                                // response 변수에 응답 데이터가 전달
//                                                Log.d("network", "util Get 요청 성공!!");
//                                            } else {
//                                                // error 변수에 에러 정보가 전달됩니다.
//                                                Log.d("network", "util Get 요청 실패ㅜㅜ : " + error);
//                                            }
//                                        }
//                                );
                            }
                        });

                carDialogBuilder.setNegativeButton("선택 취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d("mixpuppy", "다이얼로그 취소버튼이 눌렸음");

                        // 주행 시작 버튼을 누름과 동시에 찍히는 마커들 삭제
                        for (Marker marker : markerList) {
                            marker.remove();
                        }
                        markerList.clear();

                        //시작버튼 없어짐 문제로 삭제
//                        if (!startMarkerList.isEmpty()) {
//                            Marker recentStartMarker = startMarkerList.get(startMarkerList.size() - 1);
//                            recentStartMarker.remove();
//                            startMarkerList.remove(recentStartMarker);
//                        }
                    }
                });

                AlertDialog carAlertDialog = carDialogBuilder.create();
                carAlertDialog.show();

                if (serverCarNums.isEmpty()) {
                    // 서버로부터 가져온 차 목록이 비어있거나, 서버와 연결이 되어 있지 않은 경우
                    // 현재 통신 상의 문제가 생겨 원활한 앱 사용이 어렵습니다.
                    // 다시 앱이 실행해주세요., 하고 앱 종료시키기
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (carAlertDialog.isShowing()) {
                                carAlertDialog.dismiss();
                            }
                            AlertDialog.Builder alertDialogBuilder =
                                    new AlertDialog.Builder(requireContext(), R.style.permissionAlertDialogStyle);
                            alertDialogBuilder.setMessage(R.string.carList_read_fail);
                            alertDialogBuilder.setTitle("안내");

                            AlertDialog alertDialog = alertDialogBuilder.create();
                            alertDialog.show();

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (alertDialog.isShowing()) {
                                        alertDialog.dismiss();
                                    }
                                    Log.d("hanabbun","앱 종료");
                                    getActivity().finish();
                                }
                            }, 3000);
                        }
                    }, 1000);
                }
            }
        });

        /**
         * stopBtn 클릭 이벤트 리스너
         */
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 포어그라운드 서비스; 알림창이 사라짐
                Intent intent = new Intent(requireContext(), MapUpdateService.class);
                intent.setAction(Actions.STOP_FOREGROUND);
                requireContext().startService(intent);

                Log.d("mixpuppy", "정지버튼이 눌렸음");
                // 메인엑티비티 스테틱 전역변수 버튼활성화 적용
                isInitialMarkerSet=false;
                Log.d("hanaBBun", "isInitialMarkerSet : " + isInitialMarkerSet);
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

//                // 원격 서버에 시작했음을 알리는 트리거
//                NetworkUtils.sendGETRequest(stopSendUrl, requireContext(),
//                        (response, error) -> {
//                            if (response != null) {
//                                // response 변수에 응답 데이터가 전달
//                                Log.d("network", "util Get 요청 성공!!");
//                            } else {
//                                // error 변수에 에러 정보가 전달됩니다.
//                                Log.d("network", "util Get 요청 실패ㅜㅜ : " + error);
//                            }
//                        }
//                );

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

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 프래그먼트 내에서 뒤로가기 키 이벤트 처리
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 뒤로가기 키를 눌렀을 때 확인 다이얼로그 표시
                AlertDialog.Builder dialog = new AlertDialog.Builder(requireContext());

                dialog.setMessage("앱을 종료하시겠습니까?")
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (isInitialMarkerSet == true) {
                                    // 강제로 종료 버튼을 누르게 함
                                    stopBtn.performClick();
                                }
                                // 앱 종료
                                Log.d("hanabbun","앱 종료");
                                requireActivity().finish();
                            }
                        })
                        .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 다이얼로그 닫기
                                dialog.cancel();
                            }
                        });

                dialog.show();
            }
        });
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
     * (1014 코드 리뷰해본 바)
     * -> 주행 시작 전에는 MainActivity의 InitializeLocation()이 좌표값을 얻어와서
     *    그게 호출하는 MoveMap()이 지도 중심을 옮기며,
     *    StartLocationUpdate()에 의해 fusedLocationClient가 가진 위치 정보를 업데이트해서 fragment와 정보 공유
     * -> 주행 중에는 getLastLocation()에 의해 fusedLocationClient로부터 좌표값을 얻어오고,
     *    이게 호출하는 MoveMap()에 의해 지도 중심을 옮기며 이동 경로 마커를 찍음.
     */
    private void getLastLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && providerClient != null) {
            Log.d("mixpuppy", "MapFragment getLastLocation 성공진입");
            // 위치정보 얻기
            providerClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                Log.d("mixpuppy", "MapFragment 로케이션 따오기 메소드 성공 진입");
                                mLat = location.getLatitude();
                                mLng = location.getLongitude();
                                Log.d("mixpuppy", "getLastLocation - latitude:" + location.getLatitude());
                                Log.d("mixpuppy", "getLastLocation - longitude:" + location.getLongitude());
//                                // 지도 중심 이동 및 마커 찍기
//                                moveMap(mLat, mLng);
                                Log.d("mixpuppy", "getLastLocation으로 지도 중심 이동 끝");
                            } else {
                                Log.d("hanaBBun", "MapFragment getLastLocation() location == null");
                            }
                        }
                    });
        }else {
            // 위치 권한 없을때
            Log.d("mixpuppy", "target 진입 못햇쪙 ㅜㅜ");
        }
    }

    /**
     * 현재 실시간 위치로 지도 중심을 옮기는 메소드 + 주행 중 이동 경로 마커 찍기
     * @param latitude
     * @param longitude
     */
    private void moveMap(double latitude, double longitude) {
        Log.d("hanaBBun", "MapFragment의 moveMap() 호출");
        if(mMap != null) {
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
            endMarker.setZIndex(1.3f);
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
                    object,  // 전송하는 데이터
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

    public void requestCarNum(String url){
        Log.d("------------------------------", "get 리퀘스트 요청 시작");
        // 전송 준비
        JsonObjectRequest jsonRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,         // 데이터를 보내지 않으므로 null
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("mixdogg", "json onResponse: " + response);
                        try {
                            // 서버응답으로 부터 carNum 키에 해당하는 JSON 배열 추출
                            JSONArray carNumArray = response.getJSONArray("carNum");

                            for (int i = 0; i < carNumArray.length(); i++) {
                                String carNumber = carNumArray.getString(i);
                                serverCarNums.add(carNumber);
                            }

                        } catch (JSONException e){
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("mixdogg", "json onErrorResponse: " + error);
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
    }

}