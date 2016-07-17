package schemaTransformation.worker;

import schemaTransformation.capsules.DataMapKey;

import java.util.LinkedHashMap;

/**
 * Created by Felix Beuster on 15.07.2016.
 */
public class DataMapper {

    private LinkedHashMap<DataMapKey, String> attributeMap;
    private LinkedHashMap<DataMapKey, String> relationMap;

    public DataMapper() {
        attributeMap    = new LinkedHashMap<>();
        relationMap     = new LinkedHashMap<>();
    }

    public void add(String source, int type, String relation, String attribute) {
        DataMapKey key = new DataMapKey(source, type);
        attributeMap.put(key, attribute);
        relationMap.put(key, relation);
    }

    public String getAttributeName(String source, int type) {
        DataMapKey key = new DataMapKey(source, type);
        return attributeMap.get(key);
    }

    public String getRelationName(String source, int type) {
        DataMapKey key = new DataMapKey(source, type);
        return relationMap.get(key);
    }

    public String toString() {
        String ret = "";
        for(DataMapKey key : attributeMap.keySet()) {
            ret += "map " + key.getPath() + "-" + TypeMapper.constantToString(key.getType()) + " to " + relationMap.get(key) + "-" + attributeMap.get(key) + "\n";
        }
        return ret;
    }
}
