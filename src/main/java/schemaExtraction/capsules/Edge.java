package schemaExtraction.capsules;

import java.io.Serializable;
import java.util.TreeSet;

/**
 * Created by Jacob Langner
 * Implemented by Felix Beuster on 10.06.2016.
 */
public class Edge implements Serializable {

    private int childLevel;

    private static final long serialVersionUID = -8196526770077719084L;

    private String childName = "";
    private String childPath = "";
    private String parentName = "";
    private String parentPath = "";

    private TreeSet<String> docId = new TreeSet<>();

    /**
     * @return number of documents with this edge
     */
    public int countDocId() {
        return this.docId.size();
    }

    /**
     * Deletes the edge in a given document by deleting it's docId from this edge.
     * @param docId document id
     * @return Status of operation
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

    public int getChildLevel() {
        return childLevel;
    }

    public String getChildName() {
        return childName;
    }

    public String getChildPath() {
        return childPath;
    }

    public TreeSet<String> getDocId() {
        return docId;
    }

    public String getParentName() {
        return parentName;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setChildLevel(int childLevel) {
        this.childLevel = childLevel;
    }

    public void setChildName(String childName) {
        this.childName = childName;
    }

    public void setChildPath(String childPath) {
        this.childPath = childPath;
    }

    public void setDocId(String docId) {
        this.docId.add(docId);
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }
}
