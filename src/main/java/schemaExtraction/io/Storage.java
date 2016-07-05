package schemaExtraction.io;

import schemaExtraction.capsules.Edge;
import schemaExtraction.capsules.Node;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by Felix Beuster on 10.06.2016.
 * Based on work of Jacob Langner
 */
public class Storage {

    public static String DEFAULT_PATH = "saves";

    private ArrayList<Edge> edges;
    private ArrayList<Node> nodes;

    public Storage() {
        edges = new ArrayList<>();
        nodes = new ArrayList<>();
    }

    public void addEdge(String childName, String parentName, String docId, int level, String childPath, String parentPath) {
        Edge edge = new Edge();
        edge.setChildName(childName);
        edge.setParentName(parentName);
        edge.setChildPath(childPath);
        edge.setParentPath(parentPath);
        edge.setDocId(docId);
        edge.setChildLevel(level);
        edges.add(edge);
    }

    public void addNode(String name, String propType, String docId, int level, String path) {
        Node node = new Node();
        node.setName(name);
        node.setPropType(propType);
        node.setDocId(docId);
        node.setLevel(level);
        node.setPath(path);
        nodes.add(node);
    }

    public long countEdges() {
        return edges.size();
    }

    public long countNodes() {
        return nodes.size();
    }

    public void flush() {
        edges.clear();
        nodes.clear();
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

    public int hasEdge(String childPath, String parentPath) {
        int counter = 0;

        for (Edge it : edges) {
            if (it.getChildPath().equalsIgnoreCase(childPath)
                    && it.getParentPath().equalsIgnoreCase(parentPath)) {
                return counter;
            }
            counter++;
        }

        return -1;
    }

    public int hasNode(String path) {
        int counter = 0;

        for (Node it : nodes) {
            if (it.getPath().equalsIgnoreCase(path)) {
                return counter;
            }
            counter++;
        }

        return -1;
    }

    public void loadFromFile(String filepath, String collectionName) {
        try {
            String nodes_path = filepath + File.separator + collectionName + File.separator + "nodes.sav";
            String edges_path = filepath + File.separator + collectionName + File.separator + "edges.sav";

            FileInputStream fis     = new FileInputStream(nodes_path);
            ObjectInputStream ois   = new ObjectInputStream(fis);

            Object o;
            while ((o = ois.readObject()) != null) {
                nodes.add((Node) o);
            }
            ois.close();

            fis = new FileInputStream(edges_path);
            ois = new ObjectInputStream(fis);

            while ((o = ois.readObject()) != null) {
                edges.add((Edge) o);
            }
            ois.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printEdges(long docCount) {
        for (Edge e : edges) {
            System.out.println("+++++++ next Edge ++++++++++");
            System.out.println("Childname:   " + e.getChildName());
            System.out.println("Childlevel:  " + e.getChildLevel());
            System.out.println("Childpath:   " + e.getChildPath());
            System.out.println("Parentname:  " + e.getParentName());
            System.out.println("Parentpath:  " + e.getParentPath());
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
            System.out.println("Nodepath:   " + n.getPath());
            System.out.println("Datatypes:   " + n.getPropType());
            System.out.println("Membercount: " + n.countDocId());
            float percentages = n.countDocId() / docCount * 100;
            System.out.println("in percent:  " + percentages + "%");
            if (n.getArrayOrder().size() > 0) {
                System.out.println("array order: ");
                for (int i = 0; i < n.getArrayOrder().size(); i++) {
                    ArrayList<String> e = n.getArrayOrder().get(i);
                    String count = e.get(1);
                    String type = e.get(0);
                    System.out.println("    Pos. " + i + ", Type: " + type + ", Count: " + count);
                }
            }
            System.out.println();
        }
    }

    public void saveToFile(String filepath, String collectionName) {
        try {
            String nodes_path = filepath + File.separator + collectionName + File.separator + "nodes.sav";
            String edges_path = filepath + File.separator + collectionName + File.separator + "edges.sav";

            File nodes_file = new File(nodes_path);
            File edges_file = new File(edges_path);

            nodes_file.getParentFile().mkdirs();
            nodes_file.createNewFile();
            edges_file.createNewFile();

            FileOutputStream fos    = new FileOutputStream(nodes_path);
            ObjectOutputStream oos  = new ObjectOutputStream(fos);

            for (Node n : nodes) {
                oos.writeObject(n);
            }
            oos.writeObject(null);
            oos.flush();
            oos.close();

            fos = new FileOutputStream(edges_path);
            oos = new ObjectOutputStream(fos);

            for (Edge e : edges) {
                oos.writeObject(e);
            }
            oos.writeObject(null);
            oos.flush();
            oos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void replace(ArrayList<Node> newNodes, ArrayList<Edge> newEdges) {
        flush();
        nodes = newNodes;
        edges = newEdges;
    }
}
