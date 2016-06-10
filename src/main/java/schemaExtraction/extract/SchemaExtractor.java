package schemaExtraction.extract;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.google.gson.JsonParser;
import org.bson.Document;
import org.bson.types.ObjectId;
import schemaExtraction.Extraction;
import schemaExtraction.Configuration;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Jakob Langner
 * Implemented by Felix Beuster on 10.06.2016.
 */
public class SchemaExtractor {

    private Extraction main;

    private Calendar startTime;

    private int cycles;

    private JsonParser parser;

    private MongoClient mc;
    private MongoCollection<Document> docs;
    private MongoDatabase mdb;

    private String collection;
    private String datatabase;

    public SchemaExtractor(Extraction main, String database, String collection) {
        this.main = main;

        this.collection = collection;
        this.datatabase = database;

        mc      = new MongoClient();
        mdb     = mc.getDatabase(database);
        docs    = mdb.getCollection(collection);

        parser = new JsonParser();
    }

    public void close() {
        mc.close();
    }

    public void extractAll() {
        cycles = 0;
        startTime = Calendar.getInstance();

        // begin iteration
        System.out.println(docs.count() + " documents were found in the collection. The program will begin to iterate through all of them now.");

        // iteration
        for (Document doc : docs.find()) {
            String docId;

            if (doc.get("_id") == null) {
                docId = new ObjectId().toString();
            } else {
                docId = doc.get("_id").toString();
            }

            // extract method call for document schema extraction
            extractFromJsonDocument(parser.parse(doc.toJson()), collection, "", docId, 0);
        }
    }

    public long countDocs() {
        return docs.count();
    }

    private void extractFromJsonDocument(JsonElement node, String nodeName, String parentName, String docId, int level) {
        String propType = getJsonType(node);

        storeNode(nodeName, propType, docId, level);

        if (parentName != "" && parentName != collection) {
            storeEdge(nodeName, parentName, docId, level);
        }

        if (node.isJsonObject()) {
            for (Map.Entry<String, JsonElement> it : node.getAsJsonObject().entrySet()) {
                extractFromJsonDocument(parser.parse(it.getValue().toString()), it.getKey(), nodeName, docId, level + 1);
            }

        } else if (node.isJsonArray()) {
            JsonArray array = node.getAsJsonArray();

            Iterator<JsonElement> iterator = array.iterator();

            while (iterator.hasNext()) {
                JsonElement element = iterator.next();

                if (element.isJsonObject()) {
                    extractFromJsonDocument(parser.parse(element.toString()), "ArrayObject", nodeName, docId, level + 1);

                } else if (element.isJsonArray()) {
                    Iterator<JsonElement> nestedIterator = element.getAsJsonArray().iterator();

                    while (nestedIterator.hasNext()) {
                        extractFromJsonDocument(parser.parse(nestedIterator.next().toString()), "anyOf", nodeName, docId, level + 1);
                    }

                } else {
                    extractFromJsonDocument(parser.parse(element.toString()), "oneOf", nodeName, docId, level + 1);
                }
            }
        }

        if (Configuration.PRINT_CYCLE_NUMBER) {
            cycles++;

            if (cycles % 1000000 == 0) {
                Calendar now = Calendar.getInstance();
                long seconds = (now.getTimeInMillis() - startTime.getTimeInMillis()) / 1000;
                System.out.println(cycles + " cycles were completed after " + seconds + " seconds.");

                System.out.println(String.format("%d cycles were completed after %d seconds. %d nodes and %d edges are extracted so far.",
                        cycles, seconds, main.getStorage().countNodes(), main.getStorage().countEdges()));
            }
        }
    }

    private String getJsonType(JsonElement e) {
        if (Configuration.USE_SIMPLE_PROP_TYPES) {
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

    private void storeEdge(String name, String parentName, String docId, int level) {
        int edgeId = main.getStorage().hasEdge(name, parentName, level);

        if (edgeId > -1) {
            main.getStorage().getEdge(edgeId).setDocId(docId);

        } else {
            main.getStorage().addEdge(name, parentName, docId, level);
        }
    }

    private void storeNode(String name, String propType, String docId, int level) {
        int nodeId =  main.getStorage().hasNode(name, level);

        if (nodeId > -1) {
            main.getStorage().getNode(nodeId).setPropType(propType);
            main.getStorage().getNode(nodeId).setDocId(docId);

        } else {
            main.getStorage().addNode(name, propType, docId, level);
        }
    }
}
