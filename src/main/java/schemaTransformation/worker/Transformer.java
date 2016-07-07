package schemaTransformation.worker;

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

    private void makePrimitiveRelation(String name, JsonElement element) {
        Relation relation = new Relation(name);
        relation.addAttribtue(new Attribute("ID", TypeMapper.TYPE_ID));
        relation.addAttribtue(new Attribute("value", TypeMapper.jsonToInt(element)));

        relations.add(relation);
    }

    private void makeRelation(String name, JsonObject object) {
        Relation relation = new Relation(name);
        relation.addAttribtue(new Attribute("ID", TypeMapper.TYPE_ID));

        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String attributeName = entry.getKey();
            JsonObject property = entry.getValue().getAsJsonObject();

            if (property.get("anyOf") != null) {
                /** TODO
                 *  mixed types for a property
                 */

            } else {
                if (TypeMapper.jsonToInt(property.get("type")) == TypeMapper.TYPE_OBJECT) {
                    attributeName += "ID";
                    makeRelation(entry.getKey(), property.getAsJsonObject("properties"));

                } else if (TypeMapper.jsonToInt(property.get("type")) == TypeMapper.TYPE_ARRAY) {
                    attributeName += "ID";


                    if (property.getAsJsonObject("items").get("anyOf") != null) {
                        /** TODO
                         *  mixed arrays
                         */

                    } else {
                        /** TODO
                         *  - new relation (inner) ID and order field
                         *  - relation calling the array (outer) has inner.ID has field
                         *  - valid for all array types
                         */
                        if (TypeMapper.jsonToInt(property.getAsJsonObject("items").get("type")) == TypeMapper.TYPE_OBJECT) {
                            makeRelation(entry.getKey(), property.getAsJsonObject("items").getAsJsonObject("properties"));

                        } else if (TypeMapper.jsonToInt(property.getAsJsonObject("items").get("type")) == TypeMapper.TYPE_ARRAY) {
                            System.out.println("TODO nested arrays");

                        } else {
                            makePrimitiveRelation(entry.getKey(), property.getAsJsonObject("items").get("type"));
                        }
                    }
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
