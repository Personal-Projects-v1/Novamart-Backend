package commerce.novamart.common.persistence;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

/** Shared Jackson helper used by the JPA attribute converters and seeders. */
public final class Json {

    public static final JsonMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private Json() {
    }

    public static String write(Object value) {
        return MAPPER.writeValueAsString(value);
    }

    public static JsonNode tree(String json) {
        return MAPPER.readTree(json);
    }

    public static List<String> stringList(String json) {
        if (json == null || json.isBlank()) return new java.util.ArrayList<>();
        return MAPPER.readValue(json, new TypeReference<List<String>>() {});
    }

    public static Map<String, String> stringMap(String json) {
        if (json == null || json.isBlank()) return new java.util.LinkedHashMap<>();
        return MAPPER.readValue(json, new TypeReference<Map<String, String>>() {});
    }
}
