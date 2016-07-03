package schemaTransformation.capsules;

/**
 * Created by Felix Beuster on 03.07.2016.
 */
public class Attribute {

    private int type;

    private String name;

    public Attribute(String name, int type) {
        this.name = name;
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
