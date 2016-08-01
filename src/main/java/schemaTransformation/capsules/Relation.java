package schemaTransformation.capsules;

import utils.Types;
import utils.Config;

import java.util.ArrayList;

/**
 * Created by Felix Beuster on 03.07.2016.
 */
public class Relation {

    public static int TYPE_ARRAY    = 0;
    public static int TYPE_MULTI    = 1;
    public static int TYPE_OBJECT   = 2;

    private ArrayList<Attribute> attributes;
    private ArrayList<String> primaryKeys;

    private int type;

    private String name;

    public Relation(String name) {
        this.name = name;

        attributes = new ArrayList<>();
        primaryKeys = new ArrayList<>();
    }

    public void addAttribtue(Attribute attribute) {
        attributes.add(attribute);
    }

    public void addPrimaryKey(String attributeName) {
        primaryKeys.add(attributeName);
    }

    public ArrayList<Attribute> getAttributes() {
        return attributes;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public boolean hasAttribute(String name) {
        for (Attribute attribute : attributes) {
            if (attribute.getName().equals( name )) {
                return true;
            }
        }

        return false;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String toSQL(Config config) {
        String sql = "";

        sql += "CREATE TABLE `" + config.getString("sql.database") + "`.`" + name + "`(";

        for(Attribute attribute : attributes) {
            sql += attribute.toSQL() + ", ";
        }

        String keyString = "";

        for (String primaryKey : primaryKeys) {
            keyString += "`" + primaryKey + "`, ";
        }

        sql += "PRIMARY KEY (" + keyString.substring(0, keyString.length() - 2) + "))";

        return sql;
    }

    public String toString() {
        String ret = "";
        ret += "Relation " + name + "\n";

        for (Attribute attribute : attributes) {
            ret += "- " + attribute.getName() + " : " + Types.constantToString(attribute.getType());

            if (attribute.getType() == Types.TYPE_ARRAY || attribute.getType() == Types.TYPE_OBJECT) {
                ret += " (" + attribute.getForeignRelationName() + ")";
            }

            ret += "\n";
        }

        return ret;
    }
}
