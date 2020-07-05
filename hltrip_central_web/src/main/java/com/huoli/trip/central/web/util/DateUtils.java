package com.huoli.trip.central.web.util;

import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 描述: <br> 中台时间日期工具类
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：王德铭<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/3<br>
 */
public class DateUtils {
    /**
     * 获取范围内日期的集合
     * @param =dBegin
     * @param =dEnd
     * @author =wangdm
     * @return
     * @throws ParseException
     * @throws java.text.ParseException
     */
    public static List<Date> getDateList(String dBegin, String dEnd) throws ParseException, java.text.ParseException {
        //日期工具类准备
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        //设置开始时间
        Calendar calBegin = Calendar.getInstance();
        calBegin.setTime(format.parse(dBegin));
        //设置结束时间
        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(format.parse(dEnd));
        //装返回的日期集合容器
        List<Date> Datelist = new ArrayList<>();
        //将第一个月添加里面去
        Datelist.add(parseTimeStringToDate(format.format(calBegin.getTime())));
        // 每次循环给calBegin日期加一天，直到calBegin.getTime()时间等于dEnd
        while (format.parse(dEnd).after(calBegin.getTime()))  {
            // 根据日历的规则，为给定的日历字段添加或减去指定的时间量
            calBegin.add(Calendar.DAY_OF_MONTH, 1);
            Datelist.add(parseTimeStringToDate(format.format(calBegin.getTime())));
        }
//        System.out.println(Datelist);
        return Datelist;
    }

    /**
     * 时间String转Date("yyyy-MM-dd")
     * @param =time
     * @author =wangdm
     * @return
     */
    public static Date parseTimeStringToDate(String time) throws ParseException{
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;
        try {
            date =  simpleDateFormat.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException("时间类型转换异常",e);
        }
        return date;
    }
    /**
     * 时间String转Date("yyyy-MM-dd HH:mm:ss") 初始化时间入参格式拼接08:00:00
     * @param =time("1970-1-1")
     * @author =wangdm
     * @return
     */
    public static Date parseTimeStringToDate2(String time) throws ParseException{
        if(StringUtils.isBlank(time)){
            return null;
        }
        //时间以1970 年 1 月 1 日的 08:00为基准,所以传参拼接字符串
        time = time+" 08:00:00";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date =  simpleDateFormat.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException("时间类型转换异常",e);
        }
        return date;
    }
}
