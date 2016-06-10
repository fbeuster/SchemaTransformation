package schemaExtraction.io;

import schemaExtraction.capsules.Edge;
import schemaExtraction.capsules.Node;

import java.util.ArrayList;

/**
 * Created by Felix Beuster on 10.06.2016.
 */
public class Storage {

    private ArrayList<Edge> edges;
    private ArrayList<Node> nodes;

    public Storage() {
        edges = new ArrayList<>();
        nodes = new ArrayList<>();
    }

    public void addEdge(String name, String parentName, String docId, int level) {
        Edge edge = new Edge();
        edge.setChildName(name);
        edge.setParentName(parentName);
        edge.setDocId(docId);
        edge.setChildLevel(level);
        edges.add(edge);
    }

    public void addNode(String name, String propType, String docId, int level) {
        Node node = new Node();
        node.setName(name);
        node.setPropType(propType);
        node.setDocId(docId);
        node.setLevel(level);
        nodes.add(node);
    }

    public long countEdges() {
        return edges.size();
    }

    public long countNodes() {
        return nodes.size();
    }

    public Edge getEdge(int edgeId) {
        return edges.get(edgeId);
    }

    public Node getNode(int nodeId) {
        return nodes.get(nodeId);
    }

    public int hasEdge(String name, String parentName, int level) {
        int counter = -1;

        for (Edge it : edges) {
            counter++;

            if (it.getChildName().equalsIgnoreCase(name)
                    && it.getParentName().equalsIgnoreCase(parentName)
                    && it.getChildLevel() == level) {
                return counter;
            }
        }

        return counter;
    }

    public int hasNode(String name, int level) {
        int counter = -1;

        for (Node it : nodes) {
            counter++;

            if (it.getName().equalsIgnoreCase(name) && it.getLevel() == level) {
                return counter;
            }
        }

        return counter;
    }
}
