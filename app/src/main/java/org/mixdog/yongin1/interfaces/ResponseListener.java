package org.mixdog.yongin1.interfaces;

public interface ResponseListener {
    /**
     * http 통신 시 에러 처리를 쉽게 하고 코드 재사용성을 올리기 위한 인터페이스
     * 본래 성공시/실패시 2가지 인터페이스를 정의하였었으나,
     * 람다 식 구현 시 충돌 문제로 인해 하나의 메소드에 response 와 error 을 함께 담음
     */
    void onResponse(String response, String error);
}
