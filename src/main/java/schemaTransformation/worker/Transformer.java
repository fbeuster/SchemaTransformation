package schemaTransformation.worker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import schemaTransformation.capsules.Attribute;
import schemaTransformation.logs.DataMappingLog;
import utils.Config;
import schemaTransformation.capsules.Relation;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Felix Beuster on 03.07.2016.
 */
public class Transformer {

    private ArrayList<Relation> relations;

    private Config config;

    private DataMappingLog dataMapper;

    private JsonObject root;

    private String arraySuffix;
    private String name;
    private String objectSuffix;
    private String orderFieldName;
    private String primaryKeyName;
    private String valueFieldName;

    public Transformer(String name, JsonObject object, Config config) {
        this.config = config;
        this.name   = name;
        this.root   = object;
        dataMapper  = new DataMappingLog();
        relations   = new ArrayList<>();

        loadConfig();
    }

    public DataMappingLog getDataMappingLog() {
        return dataMapper;
    }

    public ArrayList<Relation> getRelations() {
        return relations;
    }

    private void handleArrayRelations(String name, JsonObject object) {
        if (object.getAsJsonObject("items").get("anyOf") != null) {
            /** multiple type array **/

            Relation relation = new Relation(name + arraySuffix);
            relation.addAttribtue(new Attribute(primaryKeyName, TypeMapper.TYPE_ID));
            relation.addAttribtue(new Attribute(orderFieldName, TypeMapper.TYPE_ORDER));

            for (Attribute attribute : handleMultipleTypes(object.getAsJsonObject("items"), valueFieldName, name, relation)) {
                JsonArray types = object.getAsJsonObject("items").getAsJsonArray("anyOf");
                String path     = types.get(0).getAsJsonObject().get("path").getAsString();

                relation.addAttribtue(attribute);
                dataMapper.add( path, name + arraySuffix, attribute );
            }

            relations.add(relation);

        } else {
            /** single type array **/

            JsonElement elementType = object.getAsJsonObject("items").get("type");

            if (TypeMapper.jsonToInt( elementType ) == TypeMapper.TYPE_OBJECT) {
                ArrayList<Attribute> extra = new ArrayList<>();
                extra.add(new Attribute(orderFieldName, TypeMapper.TYPE_ORDER));

                makeRelation(
                        name + arraySuffix,
                        object.getAsJsonObject("items").getAsJsonObject("properties"),
                        extra);

            } else if (TypeMapper.jsonToInt( elementType ) == TypeMapper.TYPE_ARRAY) {
                makeArrayRelation( name, object.getAsJsonObject("items") );

            } else {
                makePrimitiveRelation(name, elementType, object.getAsJsonObject("items").get("path").getAsString());
            }
        }
    }

    private ArrayList<Attribute> handleMultipleTypes(JsonObject object, String prepend, String name, Relation relation) {
        ArrayList<Attribute> attributes = new ArrayList<>();

        JsonArray types = object.getAsJsonArray("anyOf");
        for(JsonElement element : types) {
            JsonObject type = element.getAsJsonObject();

            int attributeType       = TypeMapper.jsonToInt(type.get("type"));
            String attributeName    = prepend + "_" + TypeMapper.jsonToString(type.get("type"));

            if (name == null) {
                name = attributeName;
            }

            if (TypeMapper.jsonToInt(type.get("type")) == TypeMapper.TYPE_OBJECT) {
                makeRelation( name + objectSuffix, type.getAsJsonObject("properties"));

            } else if (TypeMapper.jsonToInt(type.get("type")) == TypeMapper.TYPE_ARRAY) {
                handleArrayRelations( name + arraySuffix, type );
            }

            attributes.add( new Attribute( uniqueAttributeName(relation, attributeName), attributeType ) );
        }

        return attributes;
    }

    private Attribute handleSingleType(String attributeName, String propertyName, JsonObject property, Relation relation) {
        int attributeType = TypeMapper.jsonToInt(property.get("type"));

        if (attributeType == TypeMapper.TYPE_OBJECT) {
            attributeName += primaryKeyName;
            makeRelation(propertyName, property.getAsJsonObject("properties"));

        } else if (attributeType == TypeMapper.TYPE_ARRAY) {
            attributeName += primaryKeyName;
            handleArrayRelations(propertyName, property);
        }

        return new Attribute(uniqueAttributeName(relation, attributeName), attributeType);
    }

    private void loadConfig() {
        arraySuffix     = config.getString("transformation.fields.array_suffix");
        objectSuffix    = config.getString("transformation.fields.object_suffix");
        orderFieldName  = config.getString("transformation.fields.order_field_name");
        primaryKeyName  = config.getString("transformation.fields.primary_key_name");
        valueFieldName  = config.getString("transformation.fields.value_field_name");
    }

    private void makeArrayRelation(String name, JsonObject object) {
        Relation relation = new Relation(name + arraySuffix);
        relation.addAttribtue(new Attribute(primaryKeyName, TypeMapper.TYPE_ID));
        relation.addAttribtue(new Attribute(orderFieldName, TypeMapper.TYPE_ORDER));
        relation.addAttribtue(new Attribute(name + arraySuffix + arraySuffix + primaryKeyName, TypeMapper.TYPE_ID));

        handleArrayRelations(name + arraySuffix, object);

        relations.add(relation);
    }

    private void makePrimitiveRelation(String name, JsonElement element, String path) {
        Relation relation = new Relation(name + arraySuffix);
        relation.addAttribtue(new Attribute(primaryKeyName, TypeMapper.TYPE_ID));
        relation.addAttribtue(new Attribute(orderFieldName, TypeMapper.TYPE_ORDER));
        relation.addAttribtue(new Attribute(valueFieldName, TypeMapper.jsonToInt(element)));

        relations.add(relation);
        dataMapper.add(path, name + arraySuffix, new Attribute(valueFieldName, TypeMapper.jsonToInt(element)));
    }

    private void makeRelation(String name, JsonObject object) {
        makeRelation(name, object, new ArrayList<Attribute>());
    }

    private void makeRelation(String name, JsonObject object, ArrayList<Attribute> extra) {
        Relation relation = new Relation(name);
        relation.addAttribtue(new Attribute(primaryKeyName, TypeMapper.TYPE_ID));

        /** adding extras **/
        for (Attribute a : extra) {
            relation.addAttribtue(a);
        }

        /** adding properties **/
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String attributeName = entry.getKey();
            JsonObject property = entry.getValue().getAsJsonObject();

            if (property.get("anyOf") != null) {
                /** multiple type attribute **/
                int i = 0;
                for (Attribute attribute : handleMultipleTypes(property, attributeName, null, relation)) {
                    String path = property.getAsJsonArray("anyOf").get(i).getAsJsonObject().get("path").getAsString();

                    relation.addAttribtue(attribute);
                    dataMapper.add( path, name, attribute );

                    i++;
                }

            } else {
                /** single type attribute **/
                Attribute attribute = handleSingleType(attributeName, entry.getKey(), property, relation);
                relation.addAttribtue( attribute );
                dataMapper.add( property.get("path").getAsString(), name, attribute );
            }
        }

        relations.add(relation);
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

    public void print() {
        for (Relation relation : relations) {
            System.out.println(relation.toString());
        }
    }

    public void run() {
        makeRelation(name, root);
    }
}
