package schemaTransformation.capsules;

import utils.Types;

/**
 * Created by Felix Beuster on 03.07.2016.
 */
public class Attribute {

    private int type;

    private String name;
    private String foreignRelationName = null;

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

    public String getForeignRelationName() {
        return foreignRelationName;
    }

    public void setForeignRelationName(String name) {
        foreignRelationName = name;
    }

    public String toSQL() {
        if ( type == Types.TYPE_ID ) {
            return "`" + name + "` " + Types.constantToSQL(type) + " NOT NULL AUTO_INCREMENT";

        } else if ( type == Types.TYPE_ARRAY_ID || type == Types.TYPE_ARRAY_ORDER ) {
            return "`" + name + "` " + Types.constantToSQL(type) + " NOT NULL";

        } else if ( type == Types.TYPE_HASH ) {
            return "`" + name + "` " + Types.constantToSQL(type) + "(128) NULL";

        } else {
            return "`" + name + "` " + Types.constantToSQL(type) + " NULL";
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Attribute) {
            Attribute other = (Attribute) obj;

            if (name.equals(other.getName()) && type == other.getType() && (
                    (foreignRelationName == null && other.getForeignRelationName() == null) ||
                    (foreignRelationName.equals(other.getForeignRelationName()))
            )) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 10 + type;
    }
}
