package io.alauda.jenkins.plugins.pipeline.utils;

import java.util.Map;

public class Converter {
    public static String getDataAsString(Map<String, Object> data, String key) {
        if (data == null || data.size() == 0 || !data.containsKey(key)) {
            return null;
        }

        Object k1 = data.get(key);
        if (k1 != null) {
            return k1.toString();
        }
        return null;
    }

    public static Boolean getDataAsBool(Map<String, Object> data, String key) {
        if (data == null || data.size() == 0 || !data.containsKey(key)) {
            return false;
        }

        Object k1 = data.get(key);
        if (k1 != null) {
            return (Boolean) k1;
        }
        return false;
    }

    public static int getDataAsInt(Map<String, Object> data, String key, int defaultValue) {
        if (data == null || data.size() == 0 || !data.containsKey(key)) {
            return defaultValue;
        }

        Object k1 = data.get(key);
        if (k1 != null) {
            return Integer.parseInt(k1.toString());
        }
        return defaultValue;
    }
}
