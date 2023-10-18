package org.mixdog.yongin1.permission;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.mixdog.yongin1.R;

import java.util.ArrayList;
import java.util.List;

public class PermissionSupport {
    private Context context;
    // 현재 활성화된 Activity를 참조하는 변수
    private Activity activity;
    // requireContext()를 사용하면 현재 Fragment 또는 View 컨텐스트를 얻게 된다!

    // 권한 거부에 대한 메시지 표시할 대화상자
    private View viewDialog;

    // 권한 허용 상태
    public boolean locationPermissionGranted = false;
    public boolean notificationPermissionGranted = false;

    public static int locationDeniedCount = 0;
    public static int notificationDeniedCount = 0;

    // 요청할 권한 배열 저장
    private String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
    };
    private List permissionList;

    // 권한 요청 시 발생하는 창에 대한 결과값을 받기 위해 지정해주는 int 형
    // 원하는 임의의 숫자 지정 - 단지 결과 처리할 때 특정 요청을 식별하기 위한 도구로 사용됨!
    // 요청에 대한 결과값 확인을 위해 RequestCode를 final로 정의
    private final int MY_PERMISSONS_REQUEST = 1004;

    // 생성자에서 Activity와 Context를 파라미터로 받음
    // -> 클래스 내의 모든 메서드에서 이 변수들을 사용할 수 있다!
    public PermissionSupport(Activity _activity, Context _context) {
        this.activity = _activity;
        this.context = _context;
    }

    // 배열로 선언한 권한 중 허용되지 않은 권한 있는지 체크; 하나라도 허용되지 않았다면 false 반환
    public boolean checkPermissions() {
        int result;
        permissionList = new ArrayList<>();

        for (String permission : permissions) {
            result = ContextCompat.checkSelfPermission(context, permission);
            if(result != PackageManager.PERMISSION_GRANTED){
                // 허용되지 않은 권한들이 permissionsList에 담기게 된다.
                permissionList.add(permission);
            }
        }
        if(!permissionList.isEmpty()){
            return false;
        }
        return true;
    }

    // 배열로 선언한 권한에 대한 사용자에게 허용 요청
    public void requestPermission() {
        ActivityCompat.requestPermissions(
                activity, (String[]) permissionList.toArray(
                        new String[permissionList.size()]), MY_PERMISSONS_REQUEST);
    }

    // 요청한 권한에 대한 결과값 판단 및 처리; 권한 허용 상태 나타냄
    public void permissionResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        // 각 권한에 대해 허용했는지 상태 나타냄
        if (requestCode == MY_PERMISSONS_REQUEST) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)
                        && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                    locationDeniedCount = 0;
                } else if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)
                        && grantResults[i] != PackageManager.PERMISSION_GRANTED){
                    locationDeniedCount++;
                } else if (permissions[i].equals(Manifest.permission.POST_NOTIFICATIONS)
                        && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    notificationPermissionGranted = true;
                    notificationDeniedCount = 0;
                } else if (permissions[i].equals(Manifest.permission.POST_NOTIFICATIONS)
                        && grantResults[i] != PackageManager.PERMISSION_GRANTED){
                    notificationDeniedCount++;
                }
            }
        }
        Log.d("hanaBBun", "위치/알림 권한 허용 상태 :" + locationPermissionGranted + "/" + notificationPermissionGranted);
    }

    // 권한 거절 횟수에 따른 처리
    public void handlePermissionRequest() {

        Log.d("hanaBBun", "위치/알림 권한 거절 횟수 : " + locationDeniedCount + "/" + notificationDeniedCount);

        // 위치 1 알림 0 : 위치만 1차 요청 -> 위치 2 : Toast2 + 앱 종료
        if (locationDeniedCount == 1 && notificationDeniedCount == 0) {
            // 첫 번째 거절
            Log.d("hanaBBun", "위치 권한만 한 번 거절당했다.");
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.d("hanaBBun", "위치 권한이 필요한 이유에 대해 설명하자.");
                // 사용자에게 권한이 필요한 이유 설명 (AlertDialog나 Toast 메시지 사용하기)
//                Toast.makeText(activity,
//                        R.string.user_location_permission_required,
//                        Toast.LENGTH_SHORT).show();
                AlertDialog.Builder alertDialogBuilder =
                        new AlertDialog.Builder(activity, R.style.permissionAlertDialogStyle);
                alertDialogBuilder.setMessage(R.string.user_location_permission_required);
                alertDialogBuilder.setTitle("안내");

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // alertDialog를 그냥 보여주고 버튼 이벤트를 만들거면 builder에 show() 해도 되지만
                        // isShowing()과 dismiss()는 Builder에서는 지원하지 않는 메소드라
                        // AlertDialog 객체를 별도 생성했다. (Builder.create())
                        if (alertDialog.isShowing()) {
                            alertDialog.dismiss();
                        }
                    }
                }, 3500);
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 위치 권한 요청 - onRequestPermissionResult 호출!
                    ActivityCompat.requestPermissions(
                            activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSONS_REQUEST);
                }
            }, 3500);
        }
        // 위치 2 알림 0 or 위치 2 알림 1 or 위치 2 알림 2 : 위치 Toast2 + 앱 종료
        else if ( locationDeniedCount == 2 ) {
            // 두 번째 거절
//            Toast.makeText(activity,
//                    R.string.user_location_permission_not_granted,
//                    Toast.LENGTH_SHORT).show();

            AlertDialog.Builder alertDialogBuilder =
                    new AlertDialog.Builder(activity, R.style.permissionAlertDialogStyle);
            alertDialogBuilder.setMessage(R.string.user_location_permission_not_granted);
            alertDialogBuilder.setTitle("안내");

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (alertDialog.isShowing()) {
                        alertDialog.dismiss();
                    }
                    // 3초 후 앱 종료
                    Log.d("hanabbun","앱 종료");
                    activity.finish();
                }
            }, 3500);
        }
        // 위치 1 알림 1 : 둘 다 1차 요청
        else if (locationDeniedCount == 1 && notificationDeniedCount == 1) {
//            Toast.makeText(activity,
//                    R.string.user_permissions_required,
//                    Toast.LENGTH_SHORT).show();

            AlertDialog.Builder alertDialogBuilder =
                    new AlertDialog.Builder(activity, R.style.permissionAlertDialogStyle);
            alertDialogBuilder.setMessage(R.string.user_permissions_required);
            alertDialogBuilder.setTitle("안내");

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (alertDialog.isShowing()) {
                        alertDialog.dismiss();
                    }
                }
            }, 3000);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 두 개 권한 모두 요청
                    requestPermission();
                }
            }, 3000);
        }
        // 위치 0 알림 1 : 알림만 1차 요청
        else if (locationDeniedCount == 0 && notificationDeniedCount == 1) {
            // 첫 번째 거절
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.POST_NOTIFICATIONS)) {
                // 사용자에게 권한이 필요한 이유 설명 (AlertDialog나 Toast 메시지 사용하기)
//                Toast.makeText(activity,
//                        R.string.user_alarm_permission_required,
//                        Toast.LENGTH_SHORT).show();

                AlertDialog.Builder alertDialogBuilder =
                        new AlertDialog.Builder(activity, R.style.permissionAlertDialogStyle);
                alertDialogBuilder.setMessage(R.string.user_alarm_permission_required);
                alertDialogBuilder.setTitle("안내");

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (alertDialog.isShowing()) {
                            alertDialog.dismiss();
                        }
                    }
                }, 3000);
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 알림 권한 요청
                    ActivityCompat.requestPermissions(
                            activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, MY_PERMISSONS_REQUEST);
                }
            }, 3000);
        }
        // 위치 1 & 알림 2 또는 위치 0 알림 2 : 알림 Toast2 + 앱은 그냥 실행
        else if ( (locationDeniedCount == 1 && notificationDeniedCount == 2)
                || (locationDeniedCount == 0 && notificationDeniedCount == 2) ) {
//            Toast.makeText(activity,
//                    R.string.user_alarm_permission_not_granted,
//                    Toast.LENGTH_SHORT).show();

            AlertDialog.Builder alertDialogBuilder =
                    new AlertDialog.Builder(activity, R.style.permissionAlertDialogStyle);
            alertDialogBuilder.setMessage(R.string.user_alarm_permission_not_granted);
            alertDialogBuilder.setTitle("안내");

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (alertDialog.isShowing()) {
                        alertDialog.dismiss();
                    }
                }
            }, 3500);
        }
    }
}
