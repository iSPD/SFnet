package com.ispd.sfcam.utils;

import java.util.Date;

public class timeCheck {
    public static Date lastTime = new Date();

    public static boolean checkTime(long thisTime){
        Date nowTime = new Date();
        long diffTime = nowTime.getTime() - lastTime.getTime();
        // 기준시간 으로부터 몇 초가 지났는지 계산합니다.

        if (diffTime >= thisTime) {
            lastTime = nowTime;
            return true;
        }
        return false;
    }
}
