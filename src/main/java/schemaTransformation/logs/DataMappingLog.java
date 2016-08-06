package schemaTransformation.logs;

import schemaTransformation.capsules.Attribute;
import schemaTransformation.capsules.DataMapKey;
import utils.Types;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Felix Beuster on 15.07.2016.
 */
public class DataMappingLog {

    private LinkedHashMap<DataMapKey, Attribute> attributeMap;
    private LinkedHashMap<DataMapKey, String> relationMap;

    public DataMappingLog() {
        attributeMap    = new LinkedHashMap<>();
        relationMap     = new LinkedHashMap<>();
    }

    public void add(String source, String relation, Attribute attribute) {
        DataMapKey key = new DataMapKey(source, attribute.getType());
        attributeMap.put(key, attribute);
        relationMap.put(key, relation);
    }

    public Attribute getAttribute(String source, int type) {
        DataMapKey key = new DataMapKey(source, type);
        return attributeMap.get(key);
    }

    public String getPath(String relationName, Attribute attribute) {
        for (Map.Entry<DataMapKey, String> entry : relationMap.entrySet()) {
            if (entry.getValue().equals(relationName)) {
                if (attributeMap.get(entry.getKey()).equals(attribute)) {
                    return entry.getKey().getPath();
                }
            }
        }

        return null;
    }

    public String getRelationName(String source, int type) {
        DataMapKey key = new DataMapKey(source, type);
        return relationMap.get(key);
    }

    public String toString() {
        String ret = "";
        for(DataMapKey key : attributeMap.keySet()) {
            ret += "map " + key.getPath() + "-" + Types.constantToString(key.getType()) + " to " + relationMap.get(key) + "-" + attributeMap.get(key).getName() + "\n";
        }
        return ret;
    }

    public void remove(String relationName, Attribute attribute) {
        String path = getPath(relationName, attribute);

        attributeMap.remove(new DataMapKey(path, attribute.getType()));
        relationMap.remove(new DataMapKey(path, attribute.getType()));
    }
}
