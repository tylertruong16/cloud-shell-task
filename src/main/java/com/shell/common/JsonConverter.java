package com.shell.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.Optional;
import java.util.logging.Level;

@UtilityClass
@Log
public class JsonConverter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.disable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
    }

    public String convertListToJson(Object param) {
        try {
            return objectMapper.writeValueAsString(param);
        } catch (Exception e) {
            log.log(Level.WARNING, "cloud-shell-task >> JsonConverter >> convertListToJson >> Exception:", e);
            return "[]";
        }
    }

    public String convertObjectToJson(Object param) {
        try {
            return objectMapper.writeValueAsString(param);
        } catch (Exception e) {
            log.log(Level.WARNING, "cloud-shell-task >> JsonConverter >> convertObjectToJson >> Exception:", e);
            return "{}";
        }
    }

    public static <T> Optional<T> convertToObject(String json, Class<T> responseType) {
        try {
            if (StringUtils.isBlank(json)) {
                return Optional.empty();
            }
            var result = objectMapper.readValue(json, responseType);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            log.log(Level.WARNING, MessageFormat.format("cloud-shell-task >> JsonConverter >> convertToObject >> request: {0} >> Exception:", json), e);
            return Optional.empty();
        }
    }

    public static boolean canConvertToJson(String json) {
        try {
            var jsonValue = StringUtils.defaultIfBlank(json, "").trim();
            return jsonValue.startsWith("{") || jsonValue.startsWith("[");
        } catch (Exception e) {
            log.log(Level.WARNING, MessageFormat.format("cloud-shell-task >> JsonConverter >> convertStringToJsonString >> request: {0} >> Exception:", json), e);
            return false;
        }
    }

    public static <T> Optional<T> convertToObject(String json, TypeReference<T> typeKey) {
        try {
            var result = objectMapper.readValue(json, typeKey);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            log.log(Level.WARNING, MessageFormat.format("cloud-shell-task >> JsonConverter >> convertToObject >> request: {0} >> Exception:", json), e);
            return Optional.empty();
        }
    }

}
