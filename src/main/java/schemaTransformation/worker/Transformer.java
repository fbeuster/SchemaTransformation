package schemaTransformation.worker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import schemaTransformation.capsules.Attribute;
import schemaTransformation.logs.DataMappingLog;
import schemaTransformation.logs.RelationCollisions;
import utils.Config;
import schemaTransformation.capsules.Relation;
import utils.Types;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Felix Beuster on 03.07.2016.
 */
public class Transformer {

    private LinkedHashMap<String, Relation> relations;

    private Config config;

    private DataMappingLog dataMapper;

    private JsonObject root;

    private RelationCollisions collisions;

    private String arraySuffix;
    private String arrayPKeyName;
    private String name;
    private String objectSuffix;
    private String orderFieldName;
    private String primaryKeyName;
    private String valueFieldName;

    public Transformer(String name, JsonObject object) {
        this.config = new Config();
        this.name   = name;
        this.root   = object;
        collisions  = new RelationCollisions();
        dataMapper  = new DataMappingLog();
        relations   = new LinkedHashMap<>();

        loadConfig();
    }

    private Relation addProperties(Relation relation, JsonObject object) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String attributeName = entry.getKey();
            JsonObject property = entry.getValue().getAsJsonObject();

            if (property.get("anyOf") != null) {
                /** multiple type attribute **/
                int i = 0;
                for (Attribute attribute : handleMultipleTypes(property, attributeName, null, relation)) {
                    String path = property.getAsJsonArray("anyOf").get(i).getAsJsonObject().get("path").getAsString();

                    relation.addAttribtue(attribute);
                    dataMapper.add( path, relation.getName(), attribute );

                    i++;
                }

            } else {
                /** single type attribute **/
                Attribute attribute = handleSingleType(attributeName, entry.getKey(), property, relation);
                relation.addAttribtue( attribute );
                dataMapper.add( property.get("path").getAsString(), relation.getName(), attribute );
            }
        }

        return relation;
    }

    public DataMappingLog getDataMappingLog() {
        return dataMapper;
    }

    public RelationCollisions getCollisions() { return collisions; }

    public LinkedHashMap<String, Relation> getRelations() {
        return relations;
    }

    private String handleArrayRelations(String name, JsonObject object) {
        if (object.getAsJsonObject("items").get("anyOf") != null) {
            /** multiple type array **/

            Relation relation = makeBasicArrayRelation(name);
            relation.setMultiArray(true);

            for (Attribute attribute : handleMultipleTypes(object.getAsJsonObject("items"), valueFieldName, relation.getName(), relation, true)) {
                JsonArray types = object.getAsJsonObject("items").getAsJsonArray("anyOf");
                String path     = types.get(0).getAsJsonObject().get("path").getAsString();

                relation.addAttribtue(attribute);
                dataMapper.add( path, relation.getName(), attribute );
            }

            relations.put(relation.getName(), relation);

            return relation.getName();

        } else {
            /** single type array **/

            JsonElement elementType = object.getAsJsonObject("items").get("type");

            if (Types.jsonToInt( elementType ) == Types.TYPE_OBJECT) {
                return makeObjectArrayRelation(
                        name,
                        object.getAsJsonObject("items").getAsJsonObject("properties"));

            } else if (Types.jsonToInt( elementType ) == Types.TYPE_ARRAY) {
                return makeNestedArrayRelation( name, object.getAsJsonObject("items"), object.getAsJsonObject("items").get("path").getAsString()  );

            } else {
                return makePrimitiveArrayRelation(name, elementType, object.getAsJsonObject("items").get("path").getAsString());
            }
        }
    }

    private ArrayList<Attribute> handleMultipleTypes(JsonObject object, String prepend, String name, Relation relation) {
        return handleMultipleTypes(object, prepend, name, relation, false);
    }

    private ArrayList<Attribute> handleMultipleTypes(JsonObject object, String prepend, String name, Relation relation, boolean fromArray) {
        ArrayList<Attribute> attributes = new ArrayList<>();

        JsonArray types = object.getAsJsonArray("anyOf");
        for(JsonElement element : types) {
            JsonObject type = element.getAsJsonObject();

            int attributeType       = Types.jsonToInt(type.get("type"));
            String attributeName    = prepend + "_" + Types.jsonToString(type.get("type"));

            if (name == null) {
                name = attributeName;
            }

            String foreign = null;

            if (Types.jsonToInt(type.get("type")) == Types.TYPE_OBJECT) {
                foreign = makeRelation( name + objectSuffix, type.getAsJsonObject("properties"));

            } else if (Types.jsonToInt(type.get("type")) == Types.TYPE_ARRAY) {
                if (!fromArray) {
                    name += arraySuffix;
                }
                foreign = handleArrayRelations( name, type );
            }

            Attribute attribute = new Attribute( uniqueAttributeName(relation, attributeName), attributeType );

            if (foreign != null) {
                attribute.setForeignRelationName(foreign);
            }

            attributes.add( attribute );
        }

        return attributes;
    }

    private Attribute handleSingleType(String attributeName, String propertyName, JsonObject property, Relation relation) {
        int attributeType = Types.jsonToInt(property.get("type"));
        String foreign = null;

        if (attributeType == Types.TYPE_OBJECT) {
            attributeName += primaryKeyName;
            foreign = makeRelation(propertyName, property.getAsJsonObject("properties"));

        } else if (attributeType == Types.TYPE_ARRAY) {
            attributeName += arraySuffix + primaryKeyName;
            foreign = handleArrayRelations(propertyName, property);
        }

        Attribute ret = new Attribute(uniqueAttributeName(relation, attributeName), attributeType);

        if (foreign != null) {
            ret.setForeignRelationName(foreign);
        }

        return ret;
    }

    private void loadConfig() {
        arraySuffix     = config.getString("transformation.fields.array_suffix");
        arrayPKeyName   = config.getString("transformation.fields.array_pkey_name");
        objectSuffix    = config.getString("transformation.fields.object_suffix");
        orderFieldName  = config.getString("transformation.fields.order_field_name");
        primaryKeyName  = config.getString("transformation.fields.primary_key_name");
        valueFieldName  = config.getString("transformation.fields.value_field_name");
    }

    private Relation makeBasicArrayRelation(String name) {
        Relation relation   = new Relation( uniqueRelationName(name + arraySuffix) );
        relation.addAttribtue(new Attribute(arrayPKeyName, Types.TYPE_ARRAY_ID));
        relation.addAttribtue(new Attribute(orderFieldName, Types.TYPE_ARRAY_ORDER));
        relation.addPrimaryKey(arrayPKeyName);
        relation.addPrimaryKey(orderFieldName);

        return relation;
    }

    private String makeNestedArrayRelation(String name, JsonObject object, String path) {
        Relation relation = makeBasicArrayRelation(name);

        Attribute arrayAttribute = new Attribute(relation.getName() + arraySuffix + primaryKeyName, Types.TYPE_ARRAY);
        arrayAttribute.setForeignRelationName( handleArrayRelations(relation.getName(), object) );

        relation.addAttribtue(arrayAttribute);

        relations.put(relation.getName(), relation);
        dataMapper.add(path, relation.getName(), arrayAttribute);

        return relation.getName();
    }

    private String makeObjectArrayRelation(String name, JsonObject object) {
        Relation relation = makeBasicArrayRelation(name);

        relations.put(relation.getName(), addProperties(relation, object));

        return relation.getName();
    }

    private String makePrimitiveArrayRelation(String name, JsonElement element, String path) {
        Relation relation = makeBasicArrayRelation(name);
        relation.addAttribtue(new Attribute(valueFieldName, Types.jsonToInt(element)));

        relations.put(relation.getName(), relation);
        dataMapper.add(path, relation.getName(), new Attribute(valueFieldName, Types.jsonToInt(element)));

        return relation.getName();
    }

    private String makeRelation(String name, JsonObject object) {
        String relationName = uniqueRelationName(name);
        Relation relation   = new Relation(relationName);
        relation.addAttribtue(new Attribute(primaryKeyName, Types.TYPE_ID));
        relation.addPrimaryKey(primaryKeyName);

        relations.put(relationName, addProperties(relation, object));

        return relationName;
    }

    private String uniqueAttributeName(Relation relation, String attributeName) {
        int i = 0;
        String append = "";

        while (relation.hasAttribute(attributeName + append)) {
            append = "_" + i;
            i++;
        }

        return attributeName + append;
    }

    private String uniqueRelationName(String name) {
        int i = 0;
        String append = "";

        name = name.replaceAll("\\s+","_");

        while (relations.get(name + append) != null) {
            append = "_" + i;
            i++;
        }

        if (!append.equals("")) {
            collisions.add(name, name + append);
        }

        return name + append;
    }

    public void print() {
        for (String name : relations.keySet()) {
            System.out.println(relations.get(name).toString());
        }
    }

    public String run() {
        return makeRelation(name, root);
    }
}
