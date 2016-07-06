package schemaExtraction.worker;

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

import java.util.*;

/**
 * Created by Jacob Langner
 * Implemented by Felix Beuster on 10.06.2016.
 */
public class Extractor {

    private Extraction main;

    private Calendar startTime;

    private int cycles;

    private JsonParser parser;

    private MongoClient mc;
    private MongoCollection<Document> docs;
    private MongoDatabase mdb;

    private String collection;
    private String datatabase;

    public Extractor(Extraction main, String database, String collection) {
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

            // extraction method call for document schema extraction
            extractFromJsonDocument(parser.parse(doc.toJson()), collection, "", docId, 0, "");
        }
    }

    public void extractTest(String element) {
        cycles = 0;
        startTime = Calendar.getInstance();

        // begin iteration
        System.out.println("test extraction");

        String docId = new ObjectId().toString();
        extractFromJsonDocument(parser.parse(element), collection, "", docId, 0, "");
    }

    public long countDocs() {
        return docs.count();
    }

    private void extractFromJsonDocument(JsonElement node, String nodeName, String parentName, String docId, int level, String path) {
        String propType = getJsonType(node);

        storeNode(nodeName, propType, docId, level, path + "/" + nodeName);

        if (parentName != "" && parentName != collection) {
            storeEdge(nodeName, parentName, docId, level, path + "/" + nodeName, path);
        }

        path = path + "/" + nodeName;

        if (node.isJsonObject()) {
            for (Map.Entry<String, JsonElement> it : node.getAsJsonObject().entrySet()) {
                extractFromJsonDocument(parser.parse(it.getValue().toString()), it.getKey(), nodeName, docId, level + 1, path);
            }

        } else if (node.isJsonArray()) {
            JsonArray array = node.getAsJsonArray();

            Iterator<JsonElement> iterator = array.iterator();

            ArrayList<ArrayList<String>> arrayOrder = new ArrayList();
            int typeCount = 0;
            String currentType = "";

            while (iterator.hasNext()) {
                JsonElement element = iterator.next();

                String elementType = getJsonType(element);
                if (currentType.equalsIgnoreCase(elementType)) {
                    typeCount++;

                } else {
                    if (currentType != "") {
                        ArrayList<String> order = new ArrayList<>();
                        order.add(currentType);
                        order.add(typeCount + "");
                        arrayOrder.add(order);
                    }
                    currentType = elementType;
                    typeCount = 1;
                }

                if (element.isJsonObject()) {
                    extractFromJsonDocument(parser.parse(element.toString()), "anyOf", nodeName, docId, level + 1, path);

                } else if (element.isJsonArray()) {

                    extractFromJsonDocument(parser.parse(element.toString()), "anyOf", nodeName, docId, level + 1, path);

                } else {
                    extractFromJsonDocument(parser.parse(element.toString()), "anyOf", nodeName, docId, level + 1, path);
                }
            }

            if (currentType != "") {
                ArrayList<String> order = new ArrayList<>();
                order.add(currentType);
                order.add(typeCount + "");
                arrayOrder.add(order);
            }

            updateNode(nodeName, arrayOrder, level, path);

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

    private void storeEdge(String name, String parentName, String docId, int level, String childPath, String parentPath) {
        int edgeId = main.getStorage().hasEdge(childPath, parentPath);

        if (edgeId > -1) {
            main.getStorage().getEdge(edgeId).setDocId(docId);

        } else {
            main.getStorage().addEdge(name, parentName, docId, level, childPath, parentPath);
        }
    }

    private void storeNode(String name, String propType, String docId, int level, String path) {
        int nodeId =  main.getStorage().hasNode(path);

        if (nodeId > -1) {
            main.getStorage().getNode(nodeId).setPropType(propType);
            main.getStorage().getNode(nodeId).setDocId(docId);

        } else {
            main.getStorage().addNode(name, propType, docId, level, path);
        }
    }

    private void updateNode(String name, ArrayList<ArrayList<String>> order, int level, String path) {
        int nodeId =  main.getStorage().hasNode(path);

        if (nodeId > -1) {
            main.getStorage().getNode(nodeId).setArrayOrder(order);

        }
    }
}
