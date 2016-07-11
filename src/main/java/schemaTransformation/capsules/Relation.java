package schemaTransformation.capsules;

import schemaTransformation.worker.TypeMapper;

import java.util.ArrayList;

/**
 * Created by Felix Beuster on 03.07.2016.
 */
public class Relation {

    private ArrayList<Attribute> attributes;
    private String name;

    public Relation(String name) {
        this.name = name;

        attributes = new ArrayList<>();
    }

    public void addAttribtue(Attribute attribute) {
        attributes.add(attribute);
    }

    public String toSQL() {
        /**
         * CREATE TABLE `test`.`testing` (
         * `ID` INT NOT NULL AUTO_INCREMENT ,
         * `order` INT NOT NULL ,
         * `foreign` INT NOT NULL ,
         * `chars` MEDIUMTEXT NULL ,
         * `number` DOUBLE NULL ,
         * `boolean` BOOLEAN NULL ,
         * PRIMARY KEY (`ID`)) ENGINE = InnoDB;
         */
        String sql = "";

        sql += "CREATE TABLE `some_db`.`" + name + "`(";

        for(Attribute attribute : attributes) {
            sql += attribute.toSQL() + ", ";
        }

        sql += "PRIMARY KEY (`ID`))";

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
