package commerce.novamart.common.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        return Json.write(attribute == null ? List.of() : attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        return Json.stringList(dbData);
    }
}
