package org.mixdog.yongin1.utils;

import android.content.Context;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.mixdog.yongin1.interfaces.ResponseListener;

public class NetworkUtils {
    /**
     * 네트워크 통신 관련하여 코드 재사용성을 올리기 위한 유틸 클래스
     * url 정보와 현재 호출되는 곳의 context , 그리고 성공실패 구분을 위한 인터페이스를 파라미터로 받는다
     * 람다식으로 구현하여 응답처리 시 코드를 간결하게 해준다
     */
    // GET 요청을 보낼 URL을 전달받아 처리하는 메서드
    public static void sendGETRequest(String url, Context context, ResponseListener responseListener) {
        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    // 요청이 성공했을 때 응답을 처리
                    responseListener.onResponse(response, null);
                },
                error -> {
                    // 요청이 실패했을 때 에러를 처리
                    responseListener.onResponse(null, error.toString());
                });

        queue.add(stringRequest);
    }

}
