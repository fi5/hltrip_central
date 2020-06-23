package com.huoli.trip.central.web.util;

import com.baidu.disconf.client.usertools.IDisconfDataGetter;
import com.baidu.disconf.client.usertools.impl.DisconfDataGetterDefaultImpl;

import java.util.Map;

public class ConfigGetter {

        private static IDisconfDataGetter iDisconfDataGetter = new DisconfDataGetterDefaultImpl();

        public ConfigGetter() {
        }

        public static String getConfig(String key) {
            String[] params = key.split("@@");
            Object obj = getByFileItem(params[0], params[1]);
            return obj == null ? null : obj.toString();
        }

        public static Map<String, Object> getByFile(String fileName) {
            return iDisconfDataGetter.getByFile(fileName);
        }

        public static Object getByFileItem(String fileName, String fileItem) {
            return iDisconfDataGetter.getByFileItem(fileName, fileItem);
        }

        public static String getByFileItemString(String fileName, String fileItem) {
            Object obj = iDisconfDataGetter.getByFileItem(fileName, fileItem);
            return obj == null ? null : obj.toString();
        }

        public static Integer getByFileItemInteger(String fileName, String fileItem) {
            Object obj = iDisconfDataGetter.getByFileItem(fileName, fileItem);
            return obj == null ? null : Integer.parseInt(obj.toString());
        }

        public static Boolean getByFileItemBoolean(String fileName, String fileItem) {
            Object obj = iDisconfDataGetter.getByFileItem(fileName, fileItem);
            return obj == null ? null : Boolean.parseBoolean(obj.toString());
        }

        private static Object getByItem(String itemName) {
            return iDisconfDataGetter.getByItem(itemName);
        }
    }

