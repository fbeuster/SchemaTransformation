package schemaExtraction.capsules;

import java.io.Serializable;
import java.util.TreeSet;

/**
 * Created by Jakob Langner
 * Implemented by Felix Beuster on 10.06.2016.
 */
public class Node implements Serializable {

    private int level;

    private static final long serialVersionUID = -4118580040968696269L;

    private String name = "";

    private TreeSet<String> docId       = new TreeSet<>();
    private TreeSet<String> propType    = new TreeSet<>();

    /**
     * @return number of documents with this node
     */
    private int countDocId() {
        return this.docId.size();
    }

    /**
     * deletes this node in the given document by deleting its docID from this node
     * @param docId document id
     * @return Status of the operation
     */
    public String deleteDocId(String docId) {
        if (this.docId.contains(docId)) {
            this.docId.remove(docId);

            if (this.docId.size() == 0) {
                return "NoDocumentsLeft";

            } else {
                return "Ok";
            }

        } else {
            return "Error";
        }
    }

    public TreeSet<String> getDocId() {
        return docId;
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public TreeSet<String> getPropType() {
        return propType;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public void setDocId(String docId) {
        if (this.docId.contains(docId) == false) {
            this.docId.add(docId);
        }
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPropType(String propType) {
        if (this.propType.contains(propType) == false) {
            this.propType.add(propType);
        }
    }
}
