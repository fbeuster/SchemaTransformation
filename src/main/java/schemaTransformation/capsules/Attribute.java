package schemaTransformation.capsules;

import schemaTransformation.worker.TypeMapper;

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
        if ( type == TypeMapper.TYPE_ID ) {
            return "`" + name + "` " + TypeMapper.constantToSQL(type) + " NOT NULL AUTO_INCREMENT";

        } else if ( type == TypeMapper.TYPE_ARRAY || type == TypeMapper.TYPE_ID ||
                type == TypeMapper.TYPE_ORDER || type == TypeMapper.TYPE_OBJECT ) {
            return "`" + name + "` " + TypeMapper.constantToSQL(type) + " NOT NULL";

        } else  {
            return "`" + name + "` " + TypeMapper.constantToSQL(type) + " NULL";
        }
    }
}
