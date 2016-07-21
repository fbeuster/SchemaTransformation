package schemaTransformation.worker;

import com.google.gson.*;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import schemaTransformation.capsules.Attribute;
import schemaTransformation.capsules.Relation;
import schemaTransformation.logs.DataMappingLog;
import utils.Config;

import java.util.*;

/**
 * Created by Felix Beuster on 20.07.2016.
 */
public class DataMover {

    /**
     * rough idea of data transfer
     *
     *   1  each document
     *   2      read in document
     *   3      make insert statement for current element
     *   4      ech property
     *   5          look up property in data mapper
     *   6          if primitive, just add to statement
     *   7          if nested, handle nested element (4)
     *   8          get insert id of (7) and set for attribute
     *   9      end
     *  10  end
     *
     */

    private ArrayList<String> insertStatements;

    private Config config;

    private DataMappingLog dataMapping;

    private JsonParser parser;

    private LinkedHashMap<String, Relation> relations;

    private MongoCollection<Document> docs;

    private String separator;

    public DataMover(LinkedHashMap<String, Relation> relations, DataMappingLog dataMapping) {
        this.dataMapping    = dataMapping;
        this.relations      = relations;

        config      = new Config();
        insertStatements = new ArrayList<>();
        parser      = new JsonParser();
        separator   = config.getString("json.path_separator");

        MongoClient mc      = new MongoClient();
        MongoDatabase mdb   = mc.getDatabase( config.getString("mongodb.database") );
        docs                = mdb.getCollection( config.getString("mongodb.collection") );
    }

    private void parseSingleArray(JsonArray array, String path) {
        if (array.size() > 0) {

            String relationName = "";

            /** collect fields **/

            JsonElement first = array.get(0);
            if (first.isJsonObject()) {
                relationName = dataMapping.getAttribute(path, TypeMapper.TYPE_ARRAY).getForeignRelationName();

            } else if (first.isJsonArray()) {
                relationName = dataMapping.getAttribute(path, TypeMapper.TYPE_ARRAY).getForeignRelationName();

            } else if (first.isJsonPrimitive()) {
                relationName = dataMapping.getRelationName(path + separator + "anyOf", TypeMapper.jsonStringToInt(getJsonType(first)));

            }

            /** iterate and parse array items **/

            int order = 0;
            for (JsonElement element : array) {
                LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
                attributes.put("ID", "__ID");
                attributes.put("order", order);
                order++;

                if (element.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> property : first.getAsJsonObject().entrySet()) {
                        if (property.getValue().isJsonObject()) {
                            parseObject(property.getValue().getAsJsonObject(), path + separator + "anyOf" + separator + property.getKey());
                            attributes.put(property.getKey(), "__ID");

                        } else if (property.getValue().isJsonArray()) {
                            Attribute attribute = dataMapping.getAttribute(path + separator + "anyOf" + separator + property.getKey(), TypeMapper.jsonStringToInt(getJsonType(property.getValue())));
                            String foreignName = attribute.getForeignRelationName();
                            Relation relation = relations.get(foreignName);

                            if (relation.isMultiArray()) {
                                parseMultiArray(
                                        property.getValue().getAsJsonArray(),
                                        path + separator + "anyOf");
                            } else {
                                parseSingleArray(
                                        property.getValue().getAsJsonArray(),
                                        path + separator + "anyOf");
                            }
                            attributes.put(property.getKey(), "__ID");

                        } else if (property.getValue().isJsonPrimitive()) {
                            attributes.put(property.getKey(), property.getValue());
                        }
                    }

                } else if (element.isJsonArray()) {
                    relationName = dataMapping.getAttribute(path, TypeMapper.TYPE_ARRAY).getForeignRelationName();
                    Relation relation = relations.get(relationName);

                    for (Attribute attribute : relation.getAttributes()) {
                        if (!attribute.getName().equals("ID") && !attribute.getName().equals("order")) {
                            String foreignName = attribute.getForeignRelationName();
                            Relation foreignRelation = relations.get(foreignName);

                            if (foreignRelation.isMultiArray()) {
                                parseMultiArray(
                                        element.getAsJsonArray(),
                                        path + separator + "anyOf");
                            } else {
                                parseSingleArray(
                                        element.getAsJsonArray(),
                                        path + separator + "anyOf");
                            }
                        }
                        attributes.put(attribute.getName(), "__ID");
                    }

                } else if (element.isJsonPrimitive()) {
                    attributes.put("value", element);

                } else {
                }

                /** build insert statement **/

                String insert = "INSERT INTO " + relationName + "(";
                String fields = "";
                String values = "";

                for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
                    fields += "`" + attribute.getKey() + "`" + ", ";
                    values += attribute.getValue() + ", ";
                }

                insert = insert + fields.substring(0, fields.length() - 2) + ") VALUES (" + values.substring(0, values.length() - 2) + ")";

                insertStatements.add(insert);
            }

        }
    }

    private void parseMultiArray(JsonArray array, String path) {
        if (array.size() > 0) {
            String relationName = dataMapping.getAttribute(path, TypeMapper.TYPE_ARRAY).getForeignRelationName();

            /** iterate and parse array items **/

            int order = 0;
            for (JsonElement element : array) {

                LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
                attributes.put("ID", "__ID");
                attributes.put("order", order);
                order++;

                if (TypeMapper.jsonStringToInt(getJsonType(element)) == TypeMapper.TYPE_OBJECT) {
                    parseObject(
                            element.getAsJsonObject(),
                            path + separator + "anyOf");
                    attributes.put("value_object", "__ID");

                } else if (TypeMapper.jsonStringToInt(getJsonType(element)) == TypeMapper.TYPE_ARRAY) {
                    Attribute attribute = dataMapping.getAttribute(path + separator + "anyOf", TypeMapper.TYPE_ARRAY);
                    String foreignName = attribute.getForeignRelationName();
                    Relation foreignRelation   = relations.get(foreignName);

                    if (foreignRelation.isMultiArray()) {
                        parseMultiArray(
                                element.getAsJsonArray(),
                                path + separator + "anyOf");
                    } else {
                        parseSingleArray(
                                element.getAsJsonArray(),
                                path + separator + "anyOf");
                    }
                    attributes.put("value_array", "__ID");

                } else if (TypeMapper.jsonStringToInt(getJsonType(element)) == TypeMapper.TYPE_BOOL) {
                    attributes.put("value_boolean", element);

                } else if (TypeMapper.jsonStringToInt(getJsonType(element)) == TypeMapper.TYPE_STRING) {
                    attributes.put("value_string", element);

                } else if (TypeMapper.jsonStringToInt(getJsonType(element)) == TypeMapper.TYPE_NUMBER) {
                    attributes.put("value_number", element);

                } else {
                    attributes.put("value_null", element);
                }

                /** build insert statement **/

                String insert = "INSERT INTO " + relationName + "(";
                String fields = "";
                String values = "";

                for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
                    fields += "`" + attribute.getKey() + "`" + ", ";
                    values += attribute.getValue() + ", ";
                }

                insert = insert + fields.substring(0, fields.length() - 2) + ") VALUES (" + values.substring(0, values.length() - 2) + ")";

                insertStatements.add(insert);
            }
        }
    }

    private void parseObject(JsonObject object, String path) {
        String relationName = "";
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("ID", "__ID");

        /** iterate and parse properties **/

        for (Map.Entry<String, JsonElement> property : object.entrySet()) {

            String type = getJsonType(property.getValue());
            Attribute attribute = dataMapping.getAttribute(path + separator + property.getKey(), TypeMapper.jsonStringToInt(type));
            relationName = dataMapping.getRelationName(path + separator + property.getKey(), TypeMapper.jsonStringToInt(type));
            Object value;


            if (attribute.getType() == TypeMapper.TYPE_OBJECT) {
                parseObject(property.getValue().getAsJsonObject(), path + separator + property.getKey());
                value = "__ID";

            } else if (attribute.getType() == TypeMapper.TYPE_ARRAY) {
                String foreignName = attribute.getForeignRelationName();
                Relation relation   = relations.get(foreignName);

                if (relation.isMultiArray()) {
                    parseMultiArray(
                            property.getValue().getAsJsonArray(),
                            path + separator + property.getKey());
                } else {
                    parseSingleArray(
                            property.getValue().getAsJsonArray(),
                            path + separator + property.getKey());
                }
                value = "__ID";
            } else {
                value = property.getValue();
            }

            attributes.put(attribute.getName(), value);

        }

        /** build insert statement **/

        String insert = "INSERT INTO " + relationName + "(";
        String fields = "";
        String values = "";

        for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
            fields += "`" + attribute.getKey() + "`" + ", ";
            values += attribute.getValue() + ", ";
        }

        insert = insert + fields.substring(0, fields.length() - 2) + ") VALUES (" + values.substring(0, values.length() - 2) + ")";

        insertStatements.add(insert);
    }

    public void run() {
        System.out.println(dataMapping);

        if (false) {
            String element = "{\"test\":[{\"data\" : {\"cool\" : 4}}]}";
            parseObject(parser.parse(element).getAsJsonObject(), separator + config.getString("mongodb.collection"));
        } else {

            int i = 0;
            for (Document doc : docs.find()) {
                if (i != 3) {
                    i++;
                    continue;
                }

                System.out.println(parser.parse(doc.toJson()));
                parseObject(parser.parse(doc.toJson()).getAsJsonObject(), separator + config.getString("mongodb.collection"));
                break;
            }
        }
    }

    public void print() {
        for (String insert : insertStatements) {
            System.out.println(insert);
        }
    }

    private String getJsonType(JsonElement e) {
        if (config.getBoolean("extraction.features.simple_prop_types")) {
            // method body for simple property types
            if (e.isJsonObject()) {         return "JsonObject";
            } else if (e.isJsonArray()) {   return "JsonArray";
            } else if (e.isJsonNull()) {    return "JsonNull";

            } else {
                JsonPrimitive p = e.getAsJsonPrimitive();

                if (p.isString()) {         return "class com.google.gson.JsonPrimitive.String";
                } else if (p.isNumber()) {  return "class com.google.gson.JsonPrimitive.Number";
                } else if (p.isBoolean()) { return "class com.google.gson.JsonPrimitive.Boolean";
                } else if (p.isJsonNull()) {return "class com.google.gson.JsonPrimitive.JsonNull";
                } else {                    return null;
                }
            }

        } else {
            // alternative method body for exact property types
            if(e.isJsonPrimitive() == false) {
                return e.getClass().toString();

            } else {
                JsonPrimitive p = e.getAsJsonPrimitive();

                if (p.isString()) {         return "class com.google.gson.JsonPrimitive.String";
                } else if (p.isNumber()) {  return "class com.google.gson.JsonPrimitive.Number";
                } else if (p.isBoolean()) { return "class com.google.gson.JsonPrimitive.Boolean";
                } else if (p.isJsonNull()) {return "class com.google.gson.JsonPrimitive.JsonNull";
                } else {                    return null;
                }
            }
        }
    }
}
