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

        } else if ( type == Types.TYPE_ARRAY || type == Types.TYPE_ID ||
                type == Types.TYPE_ORDER || type == Types.TYPE_OBJECT ) {
            return "`" + name + "` " + Types.constantToSQL(type) + " NOT NULL";

        } else  {
            return "`" + name + "` " + Types.constantToSQL(type) + " NULL";
        }
    }
}
