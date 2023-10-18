package org.mixdog.yongin1;

import static org.mixdog.yongin1.fragment.MapFragment.startBtn;
import static org.mixdog.yongin1.fragment.MapFragment.stopBtn;

import android.app.Notification;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.mixdog.yongin1.fragment.MapFragment;


public class MapUpdateService extends Service {

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;

    // 포어그라운드 서비스 알림을 고유하게 식별하는데 사용되는 정수값; 다른 서비스 또는 알림과 구별 위해 사용된다고...
    private static final int NOTIFICATION_ID = 1;
    // 안드로이드 8.0 이상에서 도입된 알림 채널 정의에 사용되는 문자열 값.
    // 다양한 종류의 알림 사용할 때 각각에 대한 CHANNEL_ID를 정의해 알림 관리 된다고....
    //private static final String CHANNEL_ID = "my_channel_id";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // bound service가 아니므로 null
        return null;
    }

    // onCreate() : 서비스가 최초 생성될 때 한 번 초기화 작업 수행
    public void onCreate() {
        super.onCreate();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest();

        Log.d("hanaBBun", "MapUpdateService | onCreate()");

        // 알림 채널 생성
        Notification notification = MapNotification.createNotification(this);
        // 알림 채널 권한 요청
        //requestNotificationPermission();

        // 알림 표시 및 서비스 시작!; 이것 실행 없으면 백그라운드 상태에서 1분 뒤 서비스 소멸됨!
        // startForeground 메서드가 사용되어야, onStartCommand 가 호출되며 포그라운드 서비스 시작!
        startForeground(NOTIFICATION_ID, notification);
    }

    // 앱의 다른 구성 요소에서 서비스 실행 시 함수 호출
    // 포어그라운드 서비스가 시작되면 호출됨. 주로 백그라운드 작업 및 서비스 제어에 사용.
    // intent 객체를 통해 알림창의 버튼 클릭 이벤트 처리 가능
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("hanaBBun", "MapUpdateService | onStartCommand() 호출");
        Log.d("hanaBBun", "MapUpdateService | Action Received = " + intent.getAction());


        if(intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case Actions.START_FOREGROUND:
                    Log.d("hanaBBun", "Start Foreground 인텐트 받음");
                    startForegroundService();
                    break;
                case Actions.STOP_FOREGROUND:
                    Log.d("hanaBBun", "Stop Foreground 인텐트 받음");
                    // 주행 종료 버튼 누르면 알림창 사라지게 함
                    stopForegroundService();
                    break;
                case Actions.start:
                    // 알림창의 '주행 시작' 버튼을 클릭하면 뷰의 주행 시작 버튼이 눌러진 상태
                    Log.d("hanaBBun", "start 인텐트 받음");
                    startBtn.performClick();
                    break;
                case Actions.end:
                    // 알림창의 '주행 종료' 버튼을 클릭하면 뷰의 주행 종료 버튼이 눌러진 상태
                    Log.d("hanaBBun", "end 인텐트 받음");
                    stopBtn.performClick();

                    // 앱의 메인 화면으로 이동
                    Intent appOpenIntent = new Intent(this, MainActivity.class);
                    // appOpenIntent.setAction(Actions.end);

                    // 알림 클릭 이벤트 처리;
//                    PendingIntent appOpenPendingIntent =
//                            PendingIntent.getActivity(
//                                    this, 0, appOpenIntent, PendingIntent.FLAG_IMMUTABLE);
                    appOpenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(appOpenIntent);

                    break;
            }
        }

        return START_STICKY;

        // START_NOT_STICKY : 서비스가 죽어도 시스템에서 다시 재생성하지 않는다.
        // START_STICKY : 서비스가 죽어도 시스템에서 다시 재생성하며, onStartCommand() 호출 (null intent)
        // START_REDELIVER_IITENT : 재생성하며, onStartCommand() 호출 (same intent)
    }

    // 사용자가 '주행 시작' 버튼을 클릭했을 때마다 포어그라운드 서비스 시작 및 알림 표시!
    // (주행 종료를 클릭하면 포어그라운드 서비스가 종료되도록 했으므로, 다시 주행 시작 눌렀을 때 서비스가 시작되도록 해야 한다.)
    // 현재 휴대폰에서 Foreground Service가 돌아가고 있고,
    // 시스템 자원을 사용하고 있다는 것을 유저가 알 수 있도록 상태바의 알림(Notification)을 이용한다!
    private void startForegroundService() {
        Log.d("hanaBBun", "MapUpdateService | startForegroundService() 호출 성공");

    Notification notification = MapNotification.createNotification(this);
    // 알림 표시; 이것 실행 없으면 백그라운드 상태에서 1분 뒤 서비스 소멸됨!
    // startForeground 메서드가 사용되어야, onStartCommand 가 호출되며 포그라운드 서비스 시작!
    startForeground(NOTIFICATION_ID, notification);
    }

    private void stopForegroundService() {
        stopForeground(true);
        stopSelf();
    }

    /*
    // 서비스 소멸 시 호출
    public void onDestroy() {
        // 서비스가 종료될 때 정리 작업 수행
        // 위치 업데이트 중지 등 작업
        super.onDestroy();
        Log.d("hanaBBun", "onDestory()");
    }
     */

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }
        stopSelf();
    }

    // 위치 업데이트를 여기서 하라고 한다...
    // onCreate()나 onStartCommand() 메소드에서 startLocationUpdate()를 호출하는 방식으로 구현 가능

    // 위치 업데이트 로직 구현
    private void startLocationUpcates() {

    }
}