package commerce.novamart.common.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

@Converter
public class StringMapConverter implements AttributeConverter<Map<String, String>, String> {

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        return Json.write(attribute == null ? Map.of() : attribute);
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        return Json.stringMap(dbData);
    }
}
