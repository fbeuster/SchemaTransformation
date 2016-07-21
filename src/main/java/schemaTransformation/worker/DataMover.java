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
import utils.Types;

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

        config              = new Config();
        insertStatements    = new ArrayList<>();
        parser              = new JsonParser();
        separator           = config.getString("json.path_separator");

        MongoClient mc      = new MongoClient();
        MongoDatabase mdb   = mc.getDatabase( config.getString("mongodb.database") );
        docs                = mdb.getCollection( config.getString("mongodb.collection") );
    }

    private void buildInsertStatement(String relationName, LinkedHashMap<String, Object> attributes) {
        String fields = "";
        String values = "";

        for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
            fields += "`" + attribute.getKey() + "`" + ", ";
            values += attribute.getValue() + ", ";
        }

        String relation = "INSERT INTO " + relationName;
        fields = "(" + fields.substring(0, fields.length() - 2) + ") ";
        values = "VALUES (" + values.substring(0, values.length() - 2) + ")";

        insertStatements.add(relation + fields + values);
    }

    private void parseMultiArray(JsonArray array, String path) {
        if (array.size() > 0) {
            String relationName = dataMapping.getAttribute(path, Types.TYPE_ARRAY).getForeignRelationName();

            /** iterate and parse array items **/
            int order = 0;
            for (JsonElement element : array) {
                LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
                attributes.put("order", order);
                order++;

                if (element.isJsonObject()) {
                    parseObject(
                            element.getAsJsonObject(),
                            path + separator + "anyOf");
                    attributes.put("value_object", "__ID");

                } else if (element.isJsonArray()) {
                    Attribute attribute = dataMapping.getAttribute(path + separator + "anyOf", Types.TYPE_ARRAY);
                    parseSubArray(
                            attribute.getForeignRelationName(),
                            element.getAsJsonArray(),
                            path + separator + "anyOf");
                    attributes.put("value_array", "__ID");

                } else {
                    int type = Types.jsonElementToInt(element);
                    attributes.put("value_" + Types.constantToString(type), element);
                }

                /** build insert statement **/
                buildInsertStatement(relationName, attributes);
            }
        }
    }

    private void parseSingleArray(JsonArray array, String path) {
        if (array.size() > 0) {
            String relationName = dataMapping.getAttribute(path, Types.TYPE_ARRAY).getForeignRelationName();

            /** iterate and parse array items **/
            int order = 0;
            for (JsonElement element : array) {
                LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
                attributes.put("order", order);
                order++;

                if (element.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> property : element.getAsJsonObject().entrySet()) {
                        if (property.getValue().isJsonObject()) {
                            parseObject(property.getValue().getAsJsonObject(), path + separator + "anyOf" + separator + property.getKey());
                            attributes.put(property.getKey(), "__ID");

                        } else if (property.getValue().isJsonArray()) {
                            Attribute attribute = dataMapping.getAttribute(path + separator + "anyOf" + separator + property.getKey(), Types.jsonElementToInt(property.getValue()));
                            parseSubArray(
                                    attribute.getForeignRelationName(),
                                    property.getValue().getAsJsonArray(),
                                    path + separator + "anyOf");
                            attributes.put(property.getKey(), "__ID");

                        } else if (property.getValue().isJsonPrimitive()) {
                            attributes.put(property.getKey(), property.getValue());
                        }
                    }

                } else if (element.isJsonArray()) {
                    relationName = dataMapping.getAttribute(path, Types.TYPE_ARRAY).getForeignRelationName();
                    Relation relation = relations.get(relationName);

                    for (Attribute attribute : relation.getAttributes()) {
                        if (!attribute.getName().equals("ID") && !attribute.getName().equals("order")) {
                            parseSubArray(
                                    attribute.getForeignRelationName(),
                                    element.getAsJsonArray(),
                                    path + separator + "anyOf");
                        }
                        attributes.put(attribute.getName(), "__ID");
                    }

                } else {
                    attributes.put("value", element);
                }

                /** build insert statement **/
                buildInsertStatement(relationName, attributes);
            }

        }
    }

    private void parseObject(JsonObject object, String path) {
        String relationName = "";
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();

        /** iterate and parse properties **/
        for (Map.Entry<String, JsonElement> property : object.entrySet()) {

            int type = Types.jsonElementToInt(property.getValue());
            Attribute attribute = dataMapping.getAttribute(path + separator + property.getKey(), type);
            relationName        = dataMapping.getRelationName(path + separator + property.getKey(), type);

            if (attribute.getType() == Types.TYPE_OBJECT) {
                parseObject(property.getValue().getAsJsonObject(), path + separator + property.getKey());
                attributes.put(attribute.getName(), "__ID");

            } else if (attribute.getType() == Types.TYPE_ARRAY) {
                parseSubArray(
                        attribute.getForeignRelationName(),
                        property.getValue().getAsJsonArray(),
                        path + separator + property.getKey());
                attributes.put(attribute.getName(), "__ID");

            } else {
                attributes.put(attribute.getName(), property.getValue());
            }
        }

        /** build insert statement **/
        buildInsertStatement(relationName, attributes);
    }

    private void parseSubArray(String relationName, JsonArray array, String path) {
        Relation relation   = relations.get(relationName);
        if (relation.isMultiArray()) {
            parseMultiArray(array, path);
        } else {
            parseSingleArray(array, path);
        }
    }

    public void print() {
        for (String insert : insertStatements) {
            System.out.println(insert);
        }
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
}
