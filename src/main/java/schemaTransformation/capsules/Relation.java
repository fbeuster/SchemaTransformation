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

    private String foreignKeys(Config config) {
        String keyString = "";

        for (Attribute a : attributes) {
            if (a.getType() == Types.TYPE_ARRAY) {
                String key_name                 = "fk" + config.getString("transformation.fields.name_separator") + a.getName();
                String referenced_attributes    = "`" + config.getString("transformation.fields.array_pkey_name") + "`, `" + config.getString("transformation.fields.order_field_name") + "`";
                String referenced_relation      = a.getForeignRelationName();
                String referencing_attributes   = "`" + a.getName() + "`, `" + a.getForeignRelationName() + config.getString("transformation.fields.name_separator") + config.getString("transformation.fields.order_field_name") + "`";

                keyString += " FOREIGN KEY `" + key_name + "`(" + referencing_attributes + ")";
                keyString += " REFERENCES `" + referenced_relation + "`(" + referenced_attributes + ")";
                keyString += " ON UPDATE CASCADE ON DELETE SET NULL, ";

            } else if (a.getType() == Types.TYPE_OBJECT) {
                String key_name                 = "fk" + config.getString("transformation.fields.name_separator") + a.getName();
                String referenced_attribute     = config.getString("transformation.fields.primary_key_name");
                String referenced_relation      = a.getForeignRelationName();
                String referencing_attribute    = a.getName();

                keyString += " FOREIGN KEY `" + key_name + "`(`" + referencing_attribute + "`)";
                keyString += " REFERENCES `" + referenced_relation + "`(`" + referenced_attribute + "`)";
                keyString += " ON UPDATE CASCADE ON DELETE SET NULL, ";
            }
        }

        if (keyString.length() > 0) {
            return ", " + keyString.substring(0, keyString.length() - 2);
        }

        return "";
    }

    public ArrayList<Attribute> getAttributes() {
        return attributes;
    }

    public String getName() {
        return name;
    }

    public ArrayList<String> getPrimaryKeys() {
        return primaryKeys;
    }

    public int getType() {
        return type;
    }

    public boolean hasAttribute(String name) {
        for (Attribute attribute : attributes) {
            if (attribute.getName().equalsIgnoreCase( name )) {
                return true;
            }
        }

        return false;
    }

    public boolean hasAttribute(Attribute attribute) {
        for (Attribute a : attributes) {
            if (a.equals(attribute)) {
                return true;
            }
        }

        return false;
    }

    private String primaryKeys(Config config) {
        String keyString = "";

        for (String primaryKey : primaryKeys) {
            keyString += "`" + primaryKey + "`, ";
        }

        if (keyString.length() > 0) {
            return "PRIMARY KEY (" + keyString.substring(0, keyString.length() - 2) + ")";
        }

        return "";
    }

    public void setType(int type) {
        this.type = type;
    }

    private String textIndices(Config config) {
        if (config.getBoolean("sql.text_index.active")) {
            String indices = "";

            for(Attribute attribute : attributes) {
                int size = config.getInt("sql.text_index.length");
                size = size <= 767 ? size : 767;
                if (attribute.getType() == Types.TYPE_STRING) {
                    indices += "INDEX (`" + attribute.getName() + "`(" + size + ")), ";
                }
            }

            if (indices.length() > 0) {
                return ", " + indices.substring(0, indices.length() - 2);
            }
        }

        return "";
    }

    public String toSQL(Config config) {
        String sql = "";

        sql += "CREATE TABLE `" + config.getString("sql.database") + "`.`" + name + "`(";

        for(Attribute attribute : attributes) {
            sql += attribute.toSQL() + ", ";
        }

        sql += primaryKeys(config);
        sql += foreignKeys(config);
        sql += textIndices(config);
        sql += uniqueIndex(config);

        sql += ")";

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

    private String uniqueIndex(Config config) {
        if (config.getBoolean("sql.unique_index.active")) {
            int max_key_size = 3072;
            int total_text_size = max_key_size;
            int text_fields = 0;

            for (Attribute attribute : attributes) {
                if (attribute.getType() == Types.TYPE_BOOL) {
                    total_text_size -= 1;
                } else if (attribute.getType() != Types.TYPE_STRING) {
                    total_text_size -= 11;
                } else {
                    text_fields++;
                }
            }

            int size = 0;
            if (text_fields > 0) {
                size = (int) Math.min(767, Math.floor(total_text_size / text_fields));
            }
            String keyString = "";

            for (Attribute attribute : attributes) {
                if (attribute.getType() != Types.TYPE_ARRAY_ID &&
                        attribute.getType() != Types.TYPE_ID &&
                        attribute.getType() != Types.TYPE_OBJECT &&
                        attribute.getType() != Types.TYPE_ARRAY_ORDER &&
                        attribute.getType() != Types.TYPE_ARRAY) {


                    if (attribute.getType() == Types.TYPE_STRING) {
                        if (!config.getBoolean("sql.unique_index.hash")) {
                            keyString += "`" + attribute.getName() + "`(" + size + ")";
                            keyString += ", ";
                        }

                    } else {
                        keyString += "`" + attribute.getName() + "`";
                        keyString += ", ";
                    }
                }
            }

            if (keyString.length() > 0) {
                return ", UNIQUE INDEX `redundancyAvoidance` (" + keyString.substring(0, keyString.length() - 2) + ")";
            }
        }

        return "";
    }

    public void removeAttribute(Attribute attribute) {
        attributes.remove(attribute);
    }
}
