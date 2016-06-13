package schemaExtraction.worker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import org.bson.Document;
import schemaExtraction.capsules.Edge;
import schemaExtraction.capsules.Node;
import schemaExtraction.io.Storage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by Jacob Langner
 * Implemented by Felix Beuster on 11.06.2016.
 */
public class Visualizer {

    private Document schema;

    private Storage storage;

    private String database;

    public Visualizer(Storage storage, String database) {
        this.database   = database;
        this.storage    = storage;

        objectsToDcoument();
    }

    private Document elementToDocument(String nodeName, int parentOcc, int nodeLevel) {
        Document schema = new Document();

        // find current node in nodes
        for (Node n : storage.getNodes()) {
            if (n.getName().equalsIgnoreCase(nodeName) && n.getLevel() == nodeLevel) {

                // store data type of current node
                TreeSet<String> propType = n.getPropType();

                if (propType.size() == 1) {
                    schema.append("type", propType.first());

                } else {
                    schema.append("type", propType.toArray());
                }

                // store description of current node
                int nodeOcc     = n.countDocId();
                float occRel    = nodeOcc * 100 / parentOcc;
                String desc     = "Occurence: " + nodeOcc + "/" + parentOcc + ", " + occRel + "%";
                schema.append("description", desc);

                // node contains JsonObject
                if (propType.contains("JsonObject") || propType.contains("class com.google.gson.JsonObject")) {
                    List<Edge> outgoingEdges = new ArrayList<>();

                    for (Edge e : storage.getEdges()) {
                        if (e.getParentName().equalsIgnoreCase(nodeName) && e.getChildLevel() == nodeLevel + 1) {
                            outgoingEdges.add(e);
                        }
                    }

                    if (outgoingEdges.size() > 0) {
                        Document properties             = new Document();
                        JsonArray requiredProperties    = new JsonArray();

                        for (Edge e : outgoingEdges) {
                            properties.append(e.getName(), elementToDocument(e.getName(), nodeOcc, e.getChildLevel()));

                            if (e.countDocId() == nodeOcc) {
                                requiredProperties.add(e.getName());
                            }
                        }

                        schema.append("properties", properties);

                        if (requiredProperties.size() > 0) {
                            schema.append("required properties", requiredProperties);
                        }
                    }
                }

                // node contains array
                if (propType.contains("JsonArray") || propType.contains("class com.google.gson.JsonArray")) {
                    List<Edge> outgoingEdges = new ArrayList<>();

                    for (Edge e : storage.getEdges()) {
                        if (e.getParentName().equalsIgnoreCase(nodeName) && e.getChildLevel() == nodeLevel + 1) {
                            outgoingEdges.add(e);
                        }
                    }

                    if (outgoingEdges.size() == 1) {
                        schema.append("items", elementToDocument(outgoingEdges.get(0).getName(), nodeOcc, outgoingEdges.get(0).getChildLevel()));

                    } else if (outgoingEdges.size() > 1) {
                        Document items = new Document();

                        for (Edge e : outgoingEdges) {
                            items.append(e.getName(), elementToDocument(e.getName(), nodeOcc, e.getChildLevel()));
                        }

                        schema.append("items", items);
                    }
                }

                // break find current node
                break;
            }
        }

        return schema;
    }

    public void objectsToDcoument() {
        // retrieve schema information from root node
        String collectionName   = "";
        int docCount            = 0;

        for (Node n : storage.getNodes() ){
            if (n.getLevel() == 0) {
                collectionName  = n.getName();
                docCount        = n.countDocId();
                break;
            }
        }

        // build schema header
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String now          = df.format(Calendar.getInstance().getTime());
        String description  = String.format("Json Schema for collection %s of database %s, created on %s.",
                collectionName, database, now);

        Document schema = new Document();
        schema.append("title", collectionName);
        schema.append("description", description);
        schema.append("$schema", "http://json-schema.org/draft-04/schema#");

        // build schema by node iteration
        Document properties             = new Document();
        JsonArray requiredProperties    = new JsonArray();

        for (Edge e : storage.getEdges()) {
            if (e.getChildLevel() == 2) {
                properties.append(e.getParentName(), elementToDocument(e.getParentName(), docCount, 1));
            }
        }

        for (Node n : storage.getNodes()) {
            if (n.getLevel() == 1) {
                if (!properties.containsKey(n.getName())) {
                    properties.append(n.getName(), elementToDocument(n.getName(), docCount, 1));
                }

                if (n.countDocId() == docCount) {
                    requiredProperties.add(n.getName());
                }
            }
        }

        schema.append("properties", properties);
        schema.append("required properties", requiredProperties);

        this.schema = schema;
    }

    public Document toDocument() {
        return schema;
    }

    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(schema);
    }
}
