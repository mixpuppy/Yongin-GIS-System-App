package org.mixdog.yongin1;

// 액션 상수들 정의
// Intent에 원하는 Action 값을 실어보내면 onStartCommand()에서 Action 값에 따라 필요한 액션을 취할 수 있다.
public class Actions {
    // 액션 문자열을 고유하게 식별하고 충돌 방지 위한 방법
    private static final String prefix = "org.mixdog.yongin1.action.";
    public static final String MAIN = prefix + "main";
    public static final String start = prefix + "start";
    public static final String end = prefix + "end";

    public static final String START_FOREGROUND = prefix + "startforeground";
    public static final String STOP_FOREGROUND = prefix + "stopforeground";

}
