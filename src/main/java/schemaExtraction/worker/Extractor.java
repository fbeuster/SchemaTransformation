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
import utils.Config;
import utils.Types;

import java.util.*;

/**
 * Created by Jacob Langner
 * Implemented by Felix Beuster on 10.06.2016.
 */
public class Extractor {

    private Extraction main;

    private Calendar startTime;

    private Config config;

    private int cycles;

    private JsonParser parser;

    private MongoClient mc;
    private MongoCollection<Document> docs;
    private MongoDatabase mdb;

    private String collection;
    private String database;
    private String separator;

    public Extractor(Extraction main, Config config) {
        this.main   = main;
        this.config = config;

        database   = config.getString("mongodb.database");
        collection = config.getString("mongodb.collection");
        separator  = config.getString("json.path_separator");

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
        String propType = Types.jsonElementToJsonString(node);

        storeNode(nodeName, propType, docId, level, path + separator + nodeName);

        if (parentName != "" && parentName != collection) {
            storeEdge(nodeName, parentName, docId, level, path + separator + nodeName, path);
        }

        path = path + separator + nodeName;

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

                String elementType = Types.jsonElementToJsonString(element);
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

        if (config.getBoolean("extraction.debug.cycle_numbers")) {
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
