package schemaTransformation.worker;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import schemaTransformation.capsules.Attribute;
import schemaTransformation.capsules.Relation;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Felix Beuster on 03.07.2016.
 */
public class Transformer {

    private ArrayList<Relation> relations;

    private JsonObject root;

    private String name;

    public Transformer(String name, JsonObject object) {
        this.name = name;
        this.root = object;

        relations = new ArrayList<>();
    }

    private void handleArrayRelations(String name, JsonObject object) {
        if (object.getAsJsonObject("items").get("anyOf") != null) {

            Relation relation = new Relation(name);
            relation.addAttribtue(new Attribute("ID", TypeMapper.TYPE_ID));
            relation.addAttribtue(new Attribute("order", TypeMapper.TYPE_ORDER));

            JsonArray types = object.getAsJsonObject("items").getAsJsonArray("anyOf");
            for(JsonElement element : types) {
                JsonObject type = element.getAsJsonObject();

                int attributeType       = TypeMapper.jsonToInt(type.get("type"));
                String attributeName    = "value_" + TypeMapper.jsonToString(type.get("type"));

                if (TypeMapper.jsonToInt(type.get("type")) == TypeMapper.TYPE_OBJECT) {
                    makeRelation( name + "Object", type.getAsJsonObject("properties"));

                } else if (TypeMapper.jsonToInt(type.get("type")) == TypeMapper.TYPE_ARRAY) {
                    handleArrayRelations( name + "Array", type );

                } else {
                }

                relation.addAttribtue( new Attribute( attributeName, attributeType ) );
            }

            relations.add(relation);

        } else {
            if (TypeMapper.jsonToInt(object.getAsJsonObject("items").get("type")) == TypeMapper.TYPE_OBJECT) {
                ArrayList<Attribute> e = new ArrayList<>();
                e.add(new Attribute("order", TypeMapper.TYPE_ORDER));

                makeRelation(   name + "Array",
                        object.getAsJsonObject("items").getAsJsonObject("properties"),
                        e);

            } else if (TypeMapper.jsonToInt(object.getAsJsonObject("items").get("type")) == TypeMapper.TYPE_ARRAY) {
                makeArrayRelation( name + "Array", object.getAsJsonObject("items") );

            } else {
                makePrimitiveRelation(name + "Array", object.getAsJsonObject("items").get("type"));
            }
        }
    }

    private void makeArrayRelation(String name, JsonObject object) {
        Relation relation = new Relation(name);
        relation.addAttribtue(new Attribute("ID", TypeMapper.TYPE_ID));
        relation.addAttribtue(new Attribute("order", TypeMapper.TYPE_ORDER));
        relation.addAttribtue(new Attribute(name + "ArrayID", TypeMapper.TYPE_ID));

        handleArrayRelations(name, object);

        relations.add(relation);
    }

    private void makePrimitiveRelation(String name, JsonElement element) {
        Relation relation = new Relation(name);
        relation.addAttribtue(new Attribute("ID", TypeMapper.TYPE_ID));
        relation.addAttribtue(new Attribute("order", TypeMapper.TYPE_ORDER));
        relation.addAttribtue(new Attribute("value", TypeMapper.jsonToInt(element)));

        relations.add(relation);
    }

    private void makeRelation(String name, JsonObject object) {
        makeRelation(name, object, new ArrayList<>());
    }

    private void makeRelation(String name, JsonObject object, ArrayList<Attribute> extra) {
        Relation relation = new Relation(name);
        relation.addAttribtue(new Attribute("ID", TypeMapper.TYPE_ID));

        for (Attribute a : extra) {
            relation.addAttribtue(a);
        }

        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String attributeName = entry.getKey();
            JsonObject property = entry.getValue().getAsJsonObject();

            if (property.get("anyOf") != null) {
                JsonArray types = property.getAsJsonArray("anyOf");

                for(JsonElement element : types) {
                    JsonObject type = element.getAsJsonObject();

                    int attType       = TypeMapper.jsonToInt(type.get("type"));
                    String attName    = attributeName + "_" + TypeMapper.jsonToString(type.get("type"));

                    if (TypeMapper.jsonToInt(type.get("type")) == TypeMapper.TYPE_OBJECT) {
                        makeRelation( attName + "Object", type.getAsJsonObject("properties"));

                    } else if (TypeMapper.jsonToInt(type.get("type")) == TypeMapper.TYPE_ARRAY) {
                        handleArrayRelations( attName + "Array", type );

                    } else {
                    }

                    relation.addAttribtue( new Attribute( attName, attType ) );
                }

            } else {
                if (TypeMapper.jsonToInt(property.get("type")) == TypeMapper.TYPE_OBJECT) {
                    attributeName += "ID";
                    makeRelation(entry.getKey(), property.getAsJsonObject("properties"));

                } else if (TypeMapper.jsonToInt(property.get("type")) == TypeMapper.TYPE_ARRAY) {
                    attributeName += "ID";
                    handleArrayRelations(entry.getKey(), property);
                }

                Attribute attribute = new Attribute(attributeName, TypeMapper.jsonToInt(property.get("type")));
                relation.addAttribtue(attribute);
            }
        }

        relations.add(relation);
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
