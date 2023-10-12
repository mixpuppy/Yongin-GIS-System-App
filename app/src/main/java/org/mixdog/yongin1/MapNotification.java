package org.mixdog.yongin1;

import static org.mixdog.yongin1.MainActivity.isInitialMarkerSet;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import org.mixdog.yongin1.fragment.MapFragment;

public class MapNotification {
    public static final String CHANNEL_ID = "foreground_service_channel"; // 임의의 채널 ID

    public static Notification createNotification(Context context) {
        // 알림 클릭시 MainActivity로 이동됨 -- 📌실행 중인 페이지로 이동해야 하는거 아닐까?
        // -> 알림 클릭시 MapFragment로 이동되도록 코드 수정
        //Intent notificationIntent = new Intent(context, MainActivity.class);
        Intent notificationIntent = new Intent(context, MapFragment.class);
        //Intent notificationIntent = new Intent(context, MapUpdateService.class);
        notificationIntent.setAction(Actions.MAIN);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // 알림 클릭 이벤트 처리
        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // start 액션을 수신했을 때, MapUpdateService로 인텐트를 전달하는 것 같은데...
        // 각 버튼들에 관한 Intent
        Intent startIntent = new Intent(context, MapUpdateService.class);
        startIntent.setAction(Actions.start);
        PendingIntent startPendingIntent =
                PendingIntent.getService(context, 0, startIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent endIntent = new Intent(context, MapUpdateService.class);
        endIntent.setAction(Actions.end);
        PendingIntent endPendingIntent =
                PendingIntent.getService(context, 0, startIntent, PendingIntent.FLAG_IMMUTABLE);

        // 알림 -- 📌아이콘 이미지를 수정해야 하는 것 같다.
        // addAction()을 이용해서 알림에 버튼 넣기 가능; Android N 이후로는 아이콘 정의해도 어차피 타이틀만 보인다고?
        // 각 버튼을 누르면 정의해둔 PendingIntent가 작동하게 됨
        /*
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("용인시 청소차량 APP")
                .setContentText("용인시 청소차량 추적 앱이 실행 중입니다.")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setOngoing(true) // true 일경우 알림 리스트에서 클릭하거나 좌우로 드래그해도 사라지지 않음
                .addAction(new NotificationCompat.Action(android.R.drawable.ic_media_previous, "주행 시작", startPendingIntent))
                .addAction(new NotificationCompat.Action(android.R.drawable.ic_media_play, "주행 종료", endPendingIntent))
                .setContentIntent(pendingIntent)
                .build();
         */

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("용인시 청소차량 APP")
                .setContentText("용인시 청소차량 추적 앱이 실행 중입니다.")
                .setSmallIcon(R.drawable.app_icon)
                .setWhen(System.currentTimeMillis())
                .setOngoing(true);

        // fragment의 start 버튼 클릭 여부를 뜻함
        if (isInitialMarkerSet) { // 주행 시작된 경우; 알림 창에 주행 종료만 보인다.
            builder.addAction(android.R.drawable.button_onoff_indicator_on, "주행 시작", startPendingIntent);
        } else {
            builder.addAction(android.R.drawable.button_onoff_indicator_off, "주행 종료", endPendingIntent);
        }
        Notification notification = builder.setContentIntent(pendingIntent).build();

        // // Android 8.0(Oreo) 이상에서는 알림 채널 설정 필요 - 각 앱의 알림 설정에서 확인 가능
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "용인시 청소차 포어그라운드 서비스 알림", // 채널표시명
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
        return notification;
    }
}
