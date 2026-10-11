package commerce.novamart.common.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.databind.JsonNode;

/** Stores an arbitrary JSON tree (variant groups, spec groups) in a TEXT column. */
@Converter
public class JsonNodeConverter implements AttributeConverter<JsonNode, String> {

    @Override
    public String convertToDatabaseColumn(JsonNode attribute) {
        return attribute == null ? "[]" : Json.write(attribute);
    }

    @Override
    public JsonNode convertToEntityAttribute(String dbData) {
        return Json.tree(dbData == null || dbData.isBlank() ? "[]" : dbData);
    }
}
