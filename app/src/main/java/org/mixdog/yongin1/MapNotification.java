package org.mixdog.yongin1;

import static org.mixdog.yongin1.MainActivity.isInitialMarkerSet;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import org.mixdog.yongin1.fragment.MapFragment;

public class MapNotification {
    public static final String CHANNEL_ID = "foreground_service_channel"; // 임의의 채널 ID

    @SuppressLint("NotificationTrampoline")
    public static Notification createNotification(Context context) {
        // 알림 클릭시 MapFragment로 이동
        Intent mapFragmentIntent = new Intent(context, MainActivity.class);
        mapFragmentIntent.setAction(Actions.MAIN);
        //notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // 알림 클릭 이벤트 처리; MapFragment를 타겟으로 이동하도록 함
        PendingIntent mapFragmentPendingIntent =
                PendingIntent.getActivity(
                        context, 0, mapFragmentIntent, PendingIntent.FLAG_IMMUTABLE);

        // 각 버튼들에 관한 Intent
        // start 또는 end 액션을 수신했을 때, MapUpdateService로 인텐트가 이동하면서 특정 액션을 취하도록 한다.
        // PendingIntent에 의해서, 각 알림창의 버튼을 클릭하면
        // MapUpdateService의 onStartCommand() 메서드에서 Actions.start/end에 대한 처리 수행하게 됨.
        Intent startIntent = new Intent(context, MapUpdateService.class);
        startIntent.setAction(Actions.start);
        PendingIntent startPendingIntent =
                PendingIntent.getService(context, 0, startIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent endIntent = new Intent(context, MapUpdateService.class);
        endIntent.setAction(Actions.end);
        PendingIntent endPendingIntent =
                PendingIntent.getService(context, 0, endIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("용인시 청소차량 APP")
                .setContentText("용인시 청소차량 추적 앱이 실행 중입니다.")
                .setSmallIcon(R.drawable.app_icon)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setWhen(System.currentTimeMillis())
                .setOngoing(true);

        // fragment의 start 버튼 클릭 여부를 뜻함
        // addAction()을 이용해서 알림에 버튼 넣기 가능;
        // 각 버튼을 누르면 정의해둔 PendingIntent가 작동하게 됨
        // ... 알림창에 딱히 주행 시작 버튼이 필요하지 않아서 최종 사용되진 않았다.
        if (isInitialMarkerSet) { // 주행 시작된 경우; 알림 창에 주행 종료만 보인다.
            builder.addAction(android.R.drawable.btn_default, "주행 종료", endPendingIntent);
        } else {
            builder.addAction(android.R.drawable.btn_default, "주행 시작", startPendingIntent);
        }
        Notification notification = builder.setContentIntent(mapFragmentPendingIntent).build();

        // // Android 8.0(Oreo) 이상에서는 알림 채널 설정 필요 - 각 앱의 알림 설정에서 확인 가능
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "용인시 청소차 포어그라운드 서비스 알림", // 채널표시명
                    NotificationManager.IMPORTANCE_HIGH // 알림창이 생성될 때 푸시(헤드업) 알림으로 뜨도록!
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
