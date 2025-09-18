package com.fathzer.rclonesync.http;

import java.util.HashMap;
import java.util.Map;

import com.fathzer.rclonesync.SynchronizationParameters;
import com.google.gson.Gson;

final class SyncParams {

    private SyncParams() {
    }

    public static String toJson(SynchronizationParameters parameters) {
        if (parameters == null) {
            throw new NullPointerException("parameters must not be null");
        }
        if (parameters.getConfigFile() != null || parameters.getExcludesFile() != null) {
            throw new IllegalArgumentException("configFile and excludesFile are not yet supported");
        }
        
        Map<String, Object> map = new HashMap<>();
        map.put("srcFs", parameters.getSource());
        map.put("dstFs", parameters.getDestination());
        map.put("checksum", parameters.isChecksum());
        map.put("fastList", true);
        map.put("exclude", parameters.getExcludes());
        map.put("_async", true);
        return new Gson().toJson(map);
    }
}
