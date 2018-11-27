package com.bytabit.mobile.common;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.ZonedDateTime;

public class LocalDateTimeConverter implements JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {

    @Override
    public JsonElement serialize(ZonedDateTime localDateTime, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(localDateTime.toString());
    }

    @Override
    public ZonedDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
        return ZonedDateTime.parse(json.getAsString());
    }
}
