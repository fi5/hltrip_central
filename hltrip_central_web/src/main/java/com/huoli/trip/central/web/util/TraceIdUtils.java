package com.huoli.trip.central.web.util;

import com.huoli.eagle.eye.core.HuoliAtrace;
import com.huoli.eagle.eye.core.HuoliTrace;
import com.huoli.eagle.eye.core.statistical.trace.TraceInfo;
import com.huoli.trip.common.entity.ChinaCity;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Component
@Slf4j
public class TraceIdUtils {
    private static HuoliAtrace huoliAtrace;

    @Autowired
    public TraceIdUtils(HuoliAtrace _huoliAtrace) {
        huoliAtrace = _huoliAtrace;
    }

    public static String getTraceId() {
        String traceId = "test";
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            traceId = request.getHeader("X-B3-TraceId");
            log.info("原始请求传递X-B3-TraceId is :{}",traceId);
        } catch (Exception e) {
            log.error("获取traceID 失败");
        }
        if (StringUtils.isEmpty(traceId)) {
            try {
                HuoliTrace huoliTrace = huoliAtrace.getHuoliTrace();
                if (huoliTrace != null) {
                    TraceInfo traceInfo = huoliTrace.getTraceInfo();
                    if (traceInfo != null) {
                        traceId = traceInfo.getTraceId();
                        log.info("重新生成 traceId:{}",traceId);
                    }
                }
            } catch (Exception e) {
            	log.error("",e);
            }

        }
        return traceId;
    }

    public static void main(String[] args) throws BadHanyuPinyinOutputFormatCombination {
        List<ChinaCity> list = new ArrayList<>();
        ChinaCity city = new ChinaCity();
        city.setName("商洛");
        ChinaCity city1 = new ChinaCity();
        city1.setName("商丘");
        ChinaCity city3 = new ChinaCity();
        city3.setName("上饶");
        ChinaCity city2 = new ChinaCity();
        city2.setName("上海");
        list.add(city);
        list.add(city1);
        list.add(city3);
        list.add(city2);
        System.out.println(list);

        List<String> list1 = new ArrayList<>();
        list1.add("商洛");
        list1.add("商丘");
        list1.add("上饶");
        list1.add("上海");


        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        list.sort((ChinaCity info1, ChinaCity info2) -> {
            String province1 = null;
            String province2 = null;
            try {
                province1 = PinyinHelper.toHanYuPinyinString(info1.getName(), format, " ", true);
                System.out.println(province1);
                province2 = PinyinHelper.toHanYuPinyinString(info2.getName(), format, " ", true);
                System.out.println(province2);
            } catch (BadHanyuPinyinOutputFormatCombination badHanyuPinyinOutputFormatCombination) {
                log.info("汉字排序错误: {}", badHanyuPinyinOutputFormatCombination.getMessage());
            }
            return Optional.ofNullable(province1).orElse("").compareTo(province2);
        });
        System.out.println(list);

        String toHanYuPinyinString = PinyinHelper.toHanYuPinyinString("商洛", format, " ", true);

        String b = PinyinHelper.toHanYuPinyinString("上海", format, " ", true);
        System.out.println(toHanYuPinyinString);
        System.out.println(b);
    }

}
