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

    private ArrayList<String> statements;

    private Config config;

    private DataMappingLog dataMapping;

    private JsonParser parser;

    private LinkedHashMap<String, Relation> relations;

    private MongoCollection<Document> docs;

    private String arrayPKeyName;
    private String orderFieldName;
    private String separator;
    private String valueFieldName;

    public DataMover(LinkedHashMap<String, Relation> relations, DataMappingLog dataMapping) {
        this.dataMapping    = dataMapping;
        this.relations      = relations;

        config      = new Config();
        statements  = new ArrayList<>();
        parser      = new JsonParser();

        MongoClient mc      = new MongoClient();
        MongoDatabase mdb   = mc.getDatabase( config.getString("mongodb.database") );
        docs                = mdb.getCollection( config.getString("mongodb.collection") );

        loadConfig();
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
        values = "VALUES (" + values.substring(0, values.length() - 2) + ");";

        statements.add(relation + fields + values);
    }

    private void loadConfig() {
        arrayPKeyName   = config.getString("transformation.fields.array_pkey_name");
        orderFieldName  = config.getString("transformation.fields.order_field_name");
        separator       = config.getString("json.path_separator");
        valueFieldName  = config.getString("transformation.fields.value_field_name");
    }

    private String getArrayID(String relationName) {
        return "(SELECT IFNULL(MAX(`" + arrayPKeyName + "`), 0) FROM " + relationName + " LIMIT 1)";
    }

    private void saveLastArrayId(String relationName) {
        statements.add("SET @last_array_id_" + relationName + " = " + getArrayID(relationName) + ";");
    }

    private void parseMultiArray(JsonArray array, String path) {
        if (array.size() > 0) {
            String relationName = dataMapping.getAttribute(path, Types.TYPE_ARRAY).getForeignRelationName();
            saveLastArrayId(relationName);

            /** iterate and parse array items **/
            int order = 0;
            for (JsonElement element : array) {
                LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
                attributes.put(arrayPKeyName, "@last_array_id_" + relationName + " + 1");
                attributes.put(orderFieldName, order);
                order++;

                if (element.isJsonObject()) {
                    Attribute attribute = dataMapping.getAttribute(path + separator + "anyOf", Types.TYPE_OBJECT);
                    parseObject(
                            element.getAsJsonObject(),
                            path + separator + "anyOf");

                    saveLastInsertID(attribute.getForeignRelationName());
                    attributes.put("value_object", "@last_insert_" + attribute.getForeignRelationName());

                } else if (element.isJsonArray()) {
                    Attribute attribute = dataMapping.getAttribute(path + separator + "anyOf", Types.TYPE_ARRAY);
                    parseSubArray(
                            attribute.getForeignRelationName(),
                            element.getAsJsonArray(),
                            path + separator + "anyOf");

                    attributes.put("value_array", getArrayID(attribute.getForeignRelationName()));

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
            saveLastArrayId(relationName);

            /** iterate and parse array items **/
            int order = 0;
            for (JsonElement element : array) {
                LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
                attributes.put(arrayPKeyName, "@last_array_id_" + relationName + " + 1");
                attributes.put(orderFieldName, order);
                order++;

                if (element.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> property : element.getAsJsonObject().entrySet()) {
                        if (property.getValue().isJsonObject()) {
                            Attribute attribute = dataMapping.getAttribute( path + separator + "anyOf" + separator + property.getKey(), Types.TYPE_OBJECT);
                            parseObject(
                                    property.getValue().getAsJsonObject(),
                                    path + separator + "anyOf" + separator + property.getKey());

                            saveLastInsertID(attribute.getForeignRelationName());
                            attributes.put(property.getKey(), "@last_insert_" + attribute.getForeignRelationName());

                        } else if (property.getValue().isJsonArray()) {
                            Attribute attribute = dataMapping.getAttribute(path + separator + "anyOf" + separator + property.getKey(), Types.jsonElementToInt(property.getValue()));
                            parseSubArray(
                                    attribute.getForeignRelationName(),
                                    property.getValue().getAsJsonArray(),
                                    path + separator + "anyOf");

                            attributes.put(property.getKey(), getArrayID(attribute.getForeignRelationName()));

                        } else if (property.getValue().isJsonPrimitive()) {
                            attributes.put(property.getKey(), property.getValue());
                        }
                    }

                } else if (element.isJsonArray()) {
                    relationName = dataMapping.getAttribute(path, Types.TYPE_ARRAY).getForeignRelationName();
                    Relation relation = relations.get(relationName);

                    for (Attribute attribute : relation.getAttributes()) {
                        if (attribute.getType() == Types.TYPE_ARRAY) {
                            parseSubArray(
                                    attribute.getForeignRelationName(),
                                    element.getAsJsonArray(),
                                    path + separator + "anyOf");
                        }

                        attributes.put(attribute.getName(), getArrayID(attribute.getForeignRelationName()));
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

                saveLastInsertID(attribute.getForeignRelationName());
                attributes.put(attribute.getName(), "@last_insert_" + attribute.getForeignRelationName());

            } else if (attribute.getType() == Types.TYPE_ARRAY) {
                parseSubArray(
                        attribute.getForeignRelationName(),
                        property.getValue().getAsJsonArray(),
                        path + separator + property.getKey());

                attributes.put(attribute.getName(), getArrayID(attribute.getForeignRelationName()));

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
        /**
         * failing when
         *  - null in not null
         */
        System.out.println("DECLARE EXIT HANDLER FOR SQLEXCEPTION ROLLBACK;");
        System.out.println("START TRANSACTION;");

        for (String insert : statements) {
            System.out.println(insert);
        }

        System.out.println("COMMIT;");
    }

    public void run() {
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

    private void saveLastInsertID(String relationName) {
        statements.add("SET @last_insert_" + relationName + " = LAST_INSERT_ID();");
    }
}
