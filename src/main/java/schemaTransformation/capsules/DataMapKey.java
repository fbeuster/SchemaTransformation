package schemaTransformation.capsules;

/**
 * Created by Felix Beuster on 16.07.2016.
 */
public class DataMapKey {

    private int type;

    private String path;

    public DataMapKey(String path, int type) {
        this.path = path;
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public int getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DataMapKey) {
            DataMapKey other = (DataMapKey) obj;

            if (other.getPath().equals(path) && other.getType() == type) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return path.hashCode() * 10 + type;
    }
}
