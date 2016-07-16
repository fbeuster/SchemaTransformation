package schemaTransformation.worker;

import java.util.HashMap;

/**
 * Created by Felix Beuster on 15.07.2016.
 */
public class DataMapper {

    private HashMap<String, String> attributeMap;
    private HashMap<String, String> relationMap;

    public DataMapper() {
        attributeMap    = new HashMap<>();
        relationMap     = new HashMap<>();
    }

    public void add(String source, String relation, String attribute) {
        attributeMap.put(source, attribute);
        relationMap.put(source, relation);
    }

    public String getAttributeName(String source) {
        return attributeMap.get(source);
    }

    public String getRelationName(String source) {
        return relationMap.get(source);
    }
}
