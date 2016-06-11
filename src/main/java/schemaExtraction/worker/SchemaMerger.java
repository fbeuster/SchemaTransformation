package schemaExtraction.worker;

import schemaExtraction.capsules.Edge;
import schemaExtraction.capsules.Node;
import schemaExtraction.io.Storage;

import java.util.ArrayList;

/**
 * Created by Jakob Langner
 * Implemented by Felix Beuster on 11.06.2016.
 */
public class SchemaMerger {

    private ArrayList<Edge> mergeEdges;
    private ArrayList<Node> mergeNodes;

    private Storage mainStorage;

    public SchemaMerger(Storage mainStorage) {
        this.mainStorage = mainStorage;

        mergeEdges = new ArrayList<>();
        mergeNodes = new ArrayList<>();
    }

    private void mergeEdges(ArrayList<Edge> newEdges) {
        for (Edge a : mainStorage.getEdges()) {
            boolean foundMatch = false;

            for (Edge b : newEdges) {
                if (a.getChildName().equalsIgnoreCase(b.getChildName()) &&
                        a.getParentName().equalsIgnoreCase(b.getParentName()) &&
                        a.getChildLevel() == b.getChildLevel()) {
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
                if(b.getChildName().equalsIgnoreCase(e.getChildName()) &&
                        b.getParentName().equalsIgnoreCase(e.getParentName()) &&
                        b.getChildLevel() == e.getChildLevel()) {
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
                if ((a.getName().equalsIgnoreCase(b.getName()) && a.getLevel() == b.getLevel()) ||
                        (a.getLevel() == 0 && b.getLevel() == 0)) {
                    foundMatch = true;

                    if (a.getLevel() == 0 && b.getLevel() == 0) {
                        b.setName("Merged schema of '" + a.getName() + "' and '" + b.getName() + "'!");
                    }

                    for (String propType : a.getPropType()) {
                        b.setPropType(propType);
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
                if (b.getName().equalsIgnoreCase(n.getName()) && b.getLevel() == n.getLevel()) {
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
