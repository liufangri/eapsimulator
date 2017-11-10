/**
 * Copyright (c) 2017, TP-Link Co.,Ltd.
 * Author:  Sun Xiaoyu <sunxiaoyu@tp-link.com.cn>
 * Created: 2017-09-29
 */

package com.tp_link.web.eapsimulator.tools;

import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * A singleton Gson object.
 */
public class GsonHelper {
    private static Gson gson;

    public static Gson getGson() {
        if (gson == null) {
            gson = new GsonBuilder().
                    setDateFormat("yyyy-MM-dd HH:mm:ss:SSS")
                    .registerTypeAdapter(Object.class, new JsonDeserializer<Object>() {
                        @Override
                        public Object deserialize(JsonElement src, Type typeOfSrc, JsonDeserializationContext context) throws JsonParseException {
                            return src.getAsString();
                        }
                    })
                    .disableHtmlEscaping()
                    .create();
        }
        return gson;
    }
}
