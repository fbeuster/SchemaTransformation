package schemaTransformation.capsules;

import schemaTransformation.worker.TypeMapper;

import java.util.ArrayList;

/**
 * Created by Felix Beuster on 03.07.2016.
 */
public class Relation {

    public static String DEFAULT_ARRAY_SUFFIX       = "Array";
    public static String DEFAULT_OBJECT_SUFFIX      = "Object";
    public static String DEFAULT_ORDER_FIELD_NAME   = "order";
    public static String DEFAULT_PRIMARY_KEY_NAME   = "ID";
    public static String DEFAULT_VALUE_FIELD_NAME   = "value";

    private ArrayList<Attribute> attributes;
    private String name;

    public Relation(String name) {
        this.name = name;

        attributes = new ArrayList<>();
    }

    public void addAttribtue(Attribute attribute) {
        attributes.add(attribute);
    }

    public ArrayList<Attribute> getAttributes() {
        return attributes;
    }

    public String getName() {
        return name;
    }

    public String toSQL(Config config) {
        String sql = "";

        sql += "CREATE TABLE `" + config.getString("sql.database") + "`.`" + name + "`(";

        for(Attribute attribute : attributes) {
            sql += attribute.toSQL() + ", ";
        }

        sql += "PRIMARY KEY (`" + config.getString("transformation.fields.primary_key_name") + "`))";

        return sql;
    }

    public String toString() {
        String ret = "";
        ret += "Relation " + name + "\n";

        for (Attribute attribute : attributes) {
            ret += "- " + attribute.getName() + " : " + TypeMapper.constantToString(attribute.getType()) + "\n";
        }

        return ret;
    }
}
