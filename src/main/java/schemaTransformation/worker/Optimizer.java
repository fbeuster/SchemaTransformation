package schemaTransformation.worker;

import schemaTransformation.capsules.Attribute;
import schemaTransformation.capsules.Relation;
import utils.Config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Felix Beuster on 12.07.2016.
 */
public class Optimizer {
    private int attributeThreshold;

    private LinkedHashMap<String, String> inlines;
    private LinkedHashMap<String, Relation> relations;

    public Optimizer(LinkedHashMap<String, Relation> relations) {
        this.relations  = relations;

        inlines = new LinkedHashMap<>();

        loadConfig();
    }

    public void check() {
        for (String name : relations.keySet()) {
            checkInline(relations.get(name));
        }
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

    private void handleInline() {
    }

    private void loadConfig() {
        Config config = new Config();
        attributeThreshold = config.getInt("optimization.inline.attribute_threshold");
    }

    public void optimize() {
        handleInline();
    }

    public void printResults() {
        System.out.println("Optimization results:");
        System.out.println();

        for (Map.Entry<String, String> inline : inlines.entrySet()) {
            System.out.println("Relation " + inline.getKey() + " could be inlined into " + inline.getValue());
        }
    }
}
