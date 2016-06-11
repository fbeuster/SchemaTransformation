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

    public ArrayList<Edge> getEdges() {
        return edges;
    }

    public ArrayList<Node> getNodes() {
        return nodes;
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

    public void printEdges(long docCount) {
        for (Edge e : edges) {
            System.out.println("+++++++ next Edge ++++++++++");
            System.out.println("Nodename:    " + e.getChildName());
            System.out.println("Nodelevel:   " + e.getChildLevel());
            System.out.println("Parentname:  " + e.getParentName());
            System.out.println("Membercount: " + e.countDocId());
            float percentages = e.countDocId() / docCount * 100;
            System.out.println("in percent:  " + percentages + "%");
            System.out.println();
        }
    }

    public void printNodes(long docCount) {
        for(Node n : nodes) {
            System.out.println("+++++++ next Node ++++++++++");
            System.out.println("Nodename:    " + n.getName());
            System.out.println("Nodelevel:   " + n.getLevel());
            System.out.println("Datatypes:   " + n.getPropType());
            System.out.println("Membercount: " + n.countDocId());
            float percentages = n.countDocId() / docCount * 100;
            System.out.println("in percent:  " + percentages + "%");
            System.out.println();
        }
    }
}
