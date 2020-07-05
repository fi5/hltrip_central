package com.huoli.trip.central.web.util;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 描述: <br>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：王德铭<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/3<br>
 */
@Slf4j
public class ConverterUtil {
    /**
     *
     * @Description 把一个列表中的PO类全转换成为相应的VO类的列表，VO类需要有一个以PO类为参数的构造函数
     * @param sourceList 原PO列表
     * @param poclass 原始的PO类
     * @param voclass 需要转换成的VO类
     * @return 转换成的VO列表
     * @author wangdm
     */
    public static <K, V> List<V> convertPOListToVOList(List<K> sourceList, Class<K> poclass, Class<V> voclass){

        if(sourceList==null){
            return null;

        }

        List<V> targetList = new ArrayList<V>();

        try{
            if(sourceList.size()>0){
                Constructor<V> voconstructor = voclass.getConstructor(new Class[]{poclass});

                for(K source:sourceList){
                    V target = voconstructor.newInstance(new Object[]{source});
                    targetList.add(target);
                }
            }
        }catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            log.error("转换List失败",e);
        }
        return targetList;
    }

    public static <K, V> List<V> convertVOListToPOList(List<K> sourceList, Class<K> voclass, Class<V> poclass) {

        List<V> returnlist = new ArrayList<V>();

        String poname = poclass.getName();
        String shortname = poname.substring(poname.lastIndexOf(".") + 1);

        Method[] methods = voclass.getMethods();
        Method targetmethod = null;
        for (Method method : methods) {
            if (shortname.equals(method.getName().replaceFirst("to", ""))) {
                targetmethod = method;
                break;
            }
        }

        if (targetmethod != null) {
            for (K vo : sourceList) {
                V po = null;
                try {
                    po = (V) targetmethod.invoke(vo, null);
                } catch (Exception e) {
                    log.error("转换List失败",e);
                }
                returnlist.add(po);
            }
        }

        return returnlist;
    }

}
