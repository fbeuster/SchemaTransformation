package schemaTransformation.worker;

import schemaTransformation.capsules.Attribute;
import schemaTransformation.capsules.DataMapKey;
import schemaTransformation.capsules.Relation;
import schemaTransformation.logs.DataMappingLog;
import utils.Config;
import utils.Types;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by Felix Beuster on 12.07.2016.
 */
public class Optimizer {

    private Boolean askInline;
    private Boolean autoInline;

    private int attributeThreshold;

    private LinkedHashMap<String, String> inlines;
    private LinkedHashMap<String, Relation> relations;
    private DataMappingLog dataMappingLog;

    private String nameSeparator;
    private String primaryKeyName;

    public Optimizer(LinkedHashMap<String, Relation> relations, DataMappingLog dataMappingLog) {
        this.dataMappingLog = dataMappingLog;
        this.relations      = relations;

        inlines = new LinkedHashMap<>();

        loadConfig();
    }

    private void checkInline(Relation r) {
        for (Attribute attribute : r.getAttributes()) {
            if (attribute.getForeignRelationName() != null) {
                Relation foreign = relations.get(attribute.getForeignRelationName());

                if (foreign.getAttributes().size() <= attributeThreshold &&
                        foreign.getType() == Relation.TYPE_OBJECT) {
                    inlines.put(foreign.getName(), r.getName());
                }
            }
        }
    }

    public DataMappingLog getDataMappingLog() {
        return dataMappingLog;
    }

    public LinkedHashMap<String, Relation> getRelations() {
        return relations;
    }

    private void handleInline() {
        for(Map.Entry<String, String> inline : inlines.entrySet()) {
            Relation source = relations.get(inline.getKey());
            Relation target = relations.get(inline.getValue());

            for (Attribute attribute : source.getAttributes()) {
                 if (!source.getPrimaryKeys().contains(attribute.getName())) {
                     attribute.setName(source.getName() + nameSeparator + attribute.getName());
                     target.addAttribtue(attribute);

                     String path = dataMappingLog.getPath(source.getName(), attribute);
                     dataMappingLog.add(path, target.getName(), attribute);
                }
            }

            Attribute foreignKey = new Attribute(source.getName() + primaryKeyName, Types.TYPE_OBJECT);
            foreignKey.setForeignRelationName(source.getName());

            target.removeAttribute(foreignKey);
            dataMappingLog.remove(target.getName(), foreignKey);

            relations.remove(source.getName());
            relations.put(target.getName(), target);
        }
    }

    private void loadConfig() {
        Config config = new Config();
        askInline           = config.getBoolean("optimization.inline.ask_inline");
        attributeThreshold  = config.getInt("optimization.inline.attribute_threshold");
        autoInline          = config.getBoolean("optimization.inline.auto_inline");
        nameSeparator       = config.getString("transformation.fields.name_separator");
        primaryKeyName      = config.getString("transformation.fields.primary_key_name");
    }

    public void printResults() {
        System.out.println("Optimization results:");
        System.out.println();

        for (Map.Entry<String, String> inline : inlines.entrySet()) {
            System.out.println("Relation " + inline.getKey() + " could be inlined into " + inline.getValue());
        }

        System.out.println();
    }

    public void run() {
        for (String name : relations.keySet()) {
            checkInline(relations.get(name));
        }

        if (autoInline) {
            handleInline();

        } else if(askInline) {
            Scanner scan = new Scanner(System.in);
            System.out.print("Do you want to continue with these merges? ");

            String merge = scan.nextLine();

            if (merge.toLowerCase().matches("([y](es)?)|1|(t(rue)?)")) {
                handleInline();
            }
        }
    }
}
