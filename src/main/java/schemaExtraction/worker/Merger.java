package schemaExtraction.worker;

import schemaExtraction.capsules.Edge;
import schemaExtraction.capsules.Node;
import schemaExtraction.io.Storage;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Jacob Langner
 * Implemented by Felix Beuster on 11.06.2016.
 */
public class Merger {

    private ArrayList<Edge> mergeEdges;
    private ArrayList<Node> mergeNodes;

    private Storage mainStorage;

    public Merger(Storage mainStorage) {
        this.mainStorage = mainStorage;

        mergeEdges = new ArrayList<>();
        mergeNodes = new ArrayList<>();
    }

    private void mergeEdges(ArrayList<Edge> newEdges) {
        for (Edge a : mainStorage.getEdges()) {
            boolean foundMatch = false;

            for (Edge b : newEdges) {
                if (a.getChildPath().equalsIgnoreCase(b.getChildPath())
                        && a.getParentPath().equalsIgnoreCase(b.getParentPath())) {
                    foundMatch = true;

                    for (String docId : a.getDocId()) {
                        b.setDocId(docId);
                    }

                    mergeEdges.add(b);
                    break;
                }
            }

            if (!foundMatch) {
                mergeEdges.add(a);
            }
        }

        for (Edge b : newEdges) {
            boolean foundMatch = false;

            for (Edge e : mergeEdges) {
                if (b.getChildPath().equalsIgnoreCase(e.getChildPath())
                        && b.getParentPath().equalsIgnoreCase(e.getParentPath())) {
                    foundMatch = true;
                    break;
                }
            }

            if (!foundMatch) {
                mergeEdges.add(b);
            }
        }
    }

    private void mergeNodes(ArrayList<Node> newNodes) {
        for (Node a : mainStorage.getNodes()) {
            boolean foundMatch = false;

            for (Node b : newNodes) {
                if ((a.getPath().equalsIgnoreCase(b.getPath())) ||
                        (a.getLevel() == 0 && b.getLevel() == 0)) {
                    foundMatch = true;

                    if (a.getLevel() == 0 && b.getLevel() == 0) {
                        b.setName("Merged schema of '" + a.getName() + "' and '" + b.getName() + "'!");
                    }

                    for (Map.Entry<String, Integer> propType : a.getPropType().entrySet()) {
                        b.setPropType(propType.getKey());
                    }

                    for (String docId : a.getDocId()) {
                        b.setDocId(docId);
                    }

                    mergeNodes.add(b);
                    break;
                }
            }

            if (!foundMatch) {
                mergeNodes.add(a);
            }
        }

        for (Node b : newNodes) {
            boolean foundMatch = false;

            for (Node n : mergeNodes) {
                if (b.getPath().equalsIgnoreCase(n.getPath())) {
                    foundMatch = true;
                    break;
                }
            }

            if (!foundMatch) {
                mergeNodes.add(b);
            }
        }
    }

    public void mergeWithStorage(Storage newStorage) {
        mergeNodes(newStorage.getNodes());
        mergeEdges(newStorage.getEdges());

        mainStorage.replace(mergeNodes, mergeEdges);
    }
}
