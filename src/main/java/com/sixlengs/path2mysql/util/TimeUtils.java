package com.sixlengs.path2mysql.util;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * BelongsProject: 中汽知投大数据
 *
 * @author wb, xing
 * CreateTime: 2021/4/8 11:53
 * Description:
 */
@Slf4j
public class TimeUtils {
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String long2Date(long millionSeconds) {
        return sdf.format(new Date(millionSeconds));
    }
    public static String format(Date date){
       return sdf.format(date);
    }
    public static String longDiffFormat(long begin, long end) {
        if (begin>end){
            long temp = begin;
            begin = end;
            end = temp;
        }
        long million = end - begin;
        Calendar diff = Calendar.getInstance();
        diff.setTime(new Date(million));
        Calendar zero = Calendar.getInstance();
        zero.setTime(new Date(0));

        int day = diff.get(Calendar.DAY_OF_YEAR) - zero.get(Calendar.DAY_OF_YEAR);
        int hour = diff.get(Calendar.HOUR) - zero.get(Calendar.HOUR);
        int minute = diff.get(Calendar.MINUTE) - zero.get(Calendar.MINUTE);
        int second = diff.get(Calendar.SECOND) - zero.get(Calendar.SECOND);
        int millionSecond = diff.get(Calendar.MILLISECOND) - zero.get(Calendar.MILLISECOND);

        return StrUtil.format("{}天 {}小时 {}分钟 {}秒 {}毫秒",day,hour,minute,second,millionSecond);
        }
    public static String longDiffFormat(long begin) {
        long end = System.currentTimeMillis();
        if (begin>end){
            long temp = begin;
            begin = end;
            end = temp;
        }
        long million = end - begin;
        Calendar diff = Calendar.getInstance();
        diff.setTime(new Date(million));
        Calendar zero = Calendar.getInstance();
        zero.setTime(new Date(0));

        int day = diff.get(Calendar.DAY_OF_YEAR) - zero.get(Calendar.DAY_OF_YEAR);
        int hour = diff.get(Calendar.HOUR) - zero.get(Calendar.HOUR);
        int minute = diff.get(Calendar.MINUTE) - zero.get(Calendar.MINUTE);
        int second = diff.get(Calendar.SECOND) - zero.get(Calendar.SECOND);
        int millionSecond = diff.get(Calendar.MILLISECOND) - zero.get(Calendar.MILLISECOND);

        return StrUtil.format("{}天 {}小时 {}分钟 {}秒 {}毫秒",day,hour,minute,second,millionSecond);
    }

}
