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

    private ArrayList<String> statements;

    private Boolean insertWithSelect;
    private Boolean uniqueIndex;
    private Boolean uniqueIndexHash;

    private Config config;

    private DataMappingLog dataMapping;

    private JsonParser parser;

    private LinkedHashMap<String, Relation> relations;

    private MongoCollection<Document> docs;

    private String arrayPKeyName;
    private String arraySuffix;
    private String hashSuffix;
    private String lastInsertPrefix;
    private String lastArrayIdPrefix;
    private String nameSeparator;
    private String objectSuffix;
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

    private void buildInsertDuplicateStatement(String relationName, LinkedHashMap<String, Object> attributes) {
        String duplicate;
        String fields   = "";
        String pName    = relations.get(relationName).getPrimaryKeys().get(0);
        String values   = "";

        for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
            fields += "`" + attribute.getKey() + "`" + ", ";
            values += attribute.getValue() + ", ";
        }

        String relation = "INSERT INTO " + relationName;
        fields          = " (" + fields.substring(0, fields.length() - 2) + ")";
        values          = " VALUES (" + values.substring(0, values.length() - 2) + ")";
        duplicate       = " ON DUPLICATE KEY UPDATE `" + pName + "`=LAST_INSERT_ID(`" + pName + "`);";

        statements.add(relation + fields + values + duplicate);
    }

    private void buildInsertStatementWithSelect(String relationName, LinkedHashMap<String, Object> attributes) {
        String fields = "";
        String values = "";
        String where = "";
        String e = "";

        for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
            e = attribute.getKey();
            fields += "`" + attribute.getKey() + "`" + ", ";
            values += attribute.getValue() + " AS `" + attribute.getKey() + "`, ";
            where += "`" + attribute.getKey() + "` = " + attribute.getValue() + " AND ";
        }

        String relation = "INSERT INTO `" + relationName + "` (" + fields.substring(0, fields.length() - 2) + ")";
        relation += " SELECT * FROM (SELECT " + values.substring(0, values.length() - 2) + ") AS t1";
        relation += " WHERE NOT EXISTS (SELECT `" + e + "` FROM `" + relationName + "` WHERE " + where.substring(0, where.length() - 5) + ") LIMIT 1";

        statements.add(relation);
    }

    public ArrayList<String> getStatements() {
        return statements;
    }

    private void loadConfig() {
        arrayPKeyName       = config.getString("transformation.fields.array_pkey_name");
        arraySuffix         = config.getString("transformation.fields.array_suffix");
        hashSuffix          = config.getString("transformation.fields.hash_suffix");
        insertWithSelect    = config.getBoolean("sql.insert_with_select");
        lastArrayIdPrefix   = config.getString("transfer.last_array_id_prefix");
        lastInsertPrefix    = config.getString("transfer.last_insert_prefix");
        nameSeparator       = config.getString("transformation.fields.name_separator");
        objectSuffix        = config.getString("transformation.fields.object_suffix");
        orderFieldName      = config.getString("transformation.fields.order_field_name");
        uniqueIndex         = config.getBoolean("sql.unique_index.active");
        uniqueIndexHash     = config.getBoolean("sql.unique_index.hash");
        separator           = config.getString("json.path_separator");
        valueFieldName      = config.getString("transformation.fields.value_field_name");
    }

    private String getArrayID(String relationName) {
        return "(SELECT IFNULL(MAX(`" + arrayPKeyName + "`), 0) FROM " + relationName + " LIMIT 1)";
    }

    private LinkedHashMap<String, Object> inlineAttributes(JsonObject object, String path) {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();

        /** iterate and parse properties **/
        for (Map.Entry<String, JsonElement> property : object.entrySet()) {

            int type = Types.jsonElementToInt(property.getValue());
            Attribute attribute = dataMapping.getAttribute(path + separator + property.getKey(), type);

            if (attribute == null) {
                for (Map.Entry<String, Object> entry: inlineAttributes(property.getValue().getAsJsonObject(), path + separator + property.getKey()).entrySet()) {
                    attributes.put(entry.getKey(), entry.getValue());
                }

            } else {
                attributes.put(attribute.getName(), parseObjectProperty(attribute, property, path));

                if (attribute.getType() == Types.TYPE_ARRAY) {
                    attributes.put(attribute.getForeignRelationName() + nameSeparator + arraySuffix + nameSeparator + "order", 0);
                }

                if (uniqueIndex && uniqueIndexHash && attribute.getType() == Types.TYPE_STRING) {
                    attributes.put(attribute.getName() + nameSeparator + hashSuffix, "SHA2(" + property.getValue() + ", 512)");
                }
            }
        }

        return attributes;
    }

    private LinkedHashMap<String, Object> parseMultiArrayElement(JsonElement element, String path) {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();

        if (element.isJsonObject()) {
            Attribute attribute = dataMapping.getAttribute(path + separator + "anyOf", Types.TYPE_OBJECT);
            parseObject(
                    element.getAsJsonObject(),
                    path + separator + "anyOf");

            attributes.put(
                    objectSuffix,
                    "@" + lastInsertPrefix + attribute.getForeignRelationName());

        } else if (element.isJsonArray()) {
            Attribute attribute = dataMapping.getAttribute(path + separator + "anyOf", Types.TYPE_ARRAY);
            parseSubArray(
                    attribute.getForeignRelationName(),
                    element.getAsJsonArray(),
                    path + separator + "anyOf");

            attributes.put(
                    attribute.getForeignRelationName() + nameSeparator + arrayPKeyName,
                    getArrayID(attribute.getForeignRelationName()));
            attributes.put(
                    attribute.getForeignRelationName() + nameSeparator + arraySuffix + nameSeparator + "order",
                    0);

        } else {
            int type = Types.jsonElementToInt(element);
            attributes.put(
                    Types.constantToString(type),
                    element);

            if (uniqueIndex && uniqueIndexHash && type == Types.TYPE_STRING) {
                attributes.put(Types.constantToString(type) + nameSeparator + hashSuffix, "SHA2(" + element + ", 512)");
            }
        }

        return attributes;
    }

    private LinkedHashMap<String, Object> parseSingleArrayElement(JsonElement element, String path, String relationName) {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();

        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> property : element.getAsJsonObject().entrySet()) {
                Attribute attribute = dataMapping.getAttribute(path + separator + "anyOf" + separator + property.getKey(), Types.jsonElementToInt(property.getValue()));

                attributes.put(attribute.getName(), parseSingleArrayObjectProperty(property, path));

                if (property.getValue().isJsonArray()) {
                    attributes.put(attribute.getForeignRelationName() + nameSeparator + arraySuffix + nameSeparator + "order", 0);
                }
            }

        } else if (element.isJsonArray()) {
            Relation relation = relations.get(relationName);

            for (Attribute attribute : relation.getAttributes()) {
                if (attribute.getType() == Types.TYPE_ARRAY) {
                    parseSubArray(
                            attribute.getForeignRelationName(),
                            element.getAsJsonArray(),
                            path + separator + "anyOf");
                }

                attributes.put(attribute.getName(), getArrayID(attribute.getForeignRelationName()));
                attributes.put(attribute.getForeignRelationName() + nameSeparator + arraySuffix + nameSeparator + "order:", 0);
            }

        } else {
            attributes.put(valueFieldName, element);

            if (uniqueIndex && uniqueIndexHash && Types.jsonToInt(element) == Types.TYPE_STRING) {
                attributes.put(valueFieldName + nameSeparator + hashSuffix, "SHA2(" + element + ", 512)");
            }
        }

        return attributes;
    }

    private Object parseSingleArrayObjectProperty(Map.Entry<String, JsonElement> property, String path) {
        if (property.getValue().isJsonObject()) {
            Attribute attribute = dataMapping.getAttribute( path + separator + "anyOf" + separator + property.getKey(), Types.TYPE_OBJECT);
            parseObject(
                    property.getValue().getAsJsonObject(),
                    path + separator + "anyOf" + separator + property.getKey());

            return "@" + lastInsertPrefix + attribute.getForeignRelationName();

        } else if (property.getValue().isJsonArray()) {
            Attribute attribute = dataMapping.getAttribute(path + separator + "anyOf" + separator + property.getKey(), Types.jsonElementToInt(property.getValue()));
            parseSubArray(
                    attribute.getForeignRelationName(),
                    property.getValue().getAsJsonArray(),
                    path + separator + "anyOf");

            return getArrayID(attribute.getForeignRelationName());

        } else {
            return property.getValue();
        }
    }

    private void parseObject(JsonObject object, String path) {
        String relationName = "";
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();

        /** iterate and parse properties **/
        for (Map.Entry<String, JsonElement> property : object.entrySet()) {

            int type = Types.jsonElementToInt(property.getValue());
            Attribute attribute = dataMapping.getAttribute(path + separator + property.getKey(), type);

            if (attribute == null) {
                for (Map.Entry<String, Object> entry : inlineAttributes(property.getValue().getAsJsonObject(), path + separator + property.getKey()).entrySet()) {
                    attributes.put(entry.getKey(), entry.getValue());
                }

            } else {
                relationName = dataMapping.getRelationName(path + separator + property.getKey(), type);
                attributes.put(attribute.getName(), parseObjectProperty(attribute, property, path));

                if (attribute.getType() == Types.TYPE_ARRAY) {
                    attributes.put(attribute.getForeignRelationName() + nameSeparator + arraySuffix + nameSeparator + "order", 0);
                }

                if (uniqueIndex && uniqueIndexHash && attribute.getType() == Types.TYPE_STRING) {
                    attributes.put(attribute.getName() + nameSeparator + hashSuffix, "SHA2(" + property.getValue() + ", 512)");
                }
            }
        }

        /** build insert statement, save ID **/
        if (insertWithSelect) {
            buildInsertStatementWithSelect(relationName, attributes);
            saveLastInsertIDWithSelect(relationName, attributes);

        } else if (uniqueIndex) {
            buildInsertDuplicateStatement(relationName, attributes);
            saveLastInsertID(relationName);

        } else {
            buildInsertStatement(relationName, attributes);
            saveLastInsertID(relationName);
        }
    }

    private Object parseObjectProperty(Attribute attribute, Map.Entry<String, JsonElement> property, String path) {
        if (attribute.getType() == Types.TYPE_OBJECT) {
            parseObject(property.getValue().getAsJsonObject(), path + separator + property.getKey());

            return "@" + lastInsertPrefix + attribute.getForeignRelationName();

        } else if (attribute.getType() == Types.TYPE_ARRAY) {
            parseSubArray(
                    attribute.getForeignRelationName(),
                    property.getValue().getAsJsonArray(),
                    path + separator + property.getKey());

            return getArrayID(attribute.getForeignRelationName());

        } else {
            return property.getValue();
        }
    }

    private void parseSubArray(String relationName, JsonArray array, String path) {
        if (array.size() > 0) {
            saveLastArrayId(relationName);

            /** iterate and parse array items **/
            int order = 0;
            for (JsonElement element : array) {
                LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
                LinkedHashMap<String, Object> valueAttributes;

                attributes.put(arrayPKeyName, "@" + lastArrayIdPrefix + relationName + " + 1");
                attributes.put(orderFieldName, order);
                order++;

                if (relations.get(relationName).getType() == Relation.TYPE_MULTI) {
                    valueAttributes = parseMultiArrayElement(element, path);

                } else {
                    valueAttributes = parseSingleArrayElement(element, path, relationName);
                }

                for (Map.Entry<String, Object> entry : valueAttributes.entrySet()) {
                    attributes.put(entry.getKey(), entry.getValue());
                }

                /** build insert statement **/
                if (insertWithSelect) {
                    buildInsertStatementWithSelect(relationName, attributes);

                } else if (uniqueIndex) {
                    buildInsertDuplicateStatement(relationName, attributes);

                } else {
                    buildInsertStatement(relationName, attributes);
                }
            }
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
            for (Document doc : docs.find()) {
                parseObject(parser.parse(doc.toJson()).getAsJsonObject(), separator + config.getString("mongodb.collection"));
            }
        }
    }

    private void saveLastArrayId(String relationName) {
        statements.add("SET @" + lastArrayIdPrefix + relationName + " = " + getArrayID(relationName) + ";");
    }

    private void saveLastInsertID(String relationName) {
        statements.add("SET @" + lastInsertPrefix + relationName + " = LAST_INSERT_ID();");
    }

    private void saveLastInsertIDWithSelect(String relationName, LinkedHashMap<String, Object> attributes) {
        String where = "";

        for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
            where += "`" + attribute.getKey() + "` = " + attribute.getValue() + " AND ";
        }

        String relation = "SELECT @" + lastInsertPrefix + relationName + " := `ID` FROM `" + relationName + "` WHERE " + where.substring(0, where.length() - 5) + ";";

        statements.add(relation);
    }
}
