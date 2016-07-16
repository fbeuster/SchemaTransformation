package schemaTransformation.worker;

import schemaTransformation.capsules.Relation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;

/**
 * Created by Felix Beuster on 12.07.2016.
 */
public class Optimizer {

    private ArrayList<Relation> relations;

    private static double DEFAULT_OMIT_THRESHHOLD = 20.0;

    private static int DEFAULT_ATTRIBUTE_THRESHHOLD = 3;

    private TreeSet<String> fewAttribtues;
    private TreeSet<String> sameNames;

    public Optimizer(ArrayList<Relation> relations) {
        this.relations = relations;

        fewAttribtues = new TreeSet<>();
        sameNames = new TreeSet<>();
    }

    private void checkInline(Relation r) {
        if (r.getAttributes().size() < DEFAULT_ATTRIBUTE_THRESHHOLD) {
            fewAttribtues.add(r.getName());
        }
    }

    private void checkMerge() {
        HashSet<String> compare = new HashSet<>();

        for (Relation r : relations) {
            if (!compare.add(r.getName())) {
                sameNames.add(r.getName());
            }
        }

        compare.clear();
    }

    private void checkMetrics(Relation r) {
    }

    public void printResults() {
        System.out.println("Optimization results:");

        int numberRelations = relations.size();
        System.out.println(numberRelations + " tables were found in the schema");

        int numberFewAttributes = fewAttribtues.size();
        System.out.println(numberFewAttributes + " tables are below the attribute threshold");

        int numberSameNames = sameNames.size();
        System.out.println(numberSameNames + " tables have the same name");
    }

    public void run() {
        checkMerge();
        for (Relation r : relations) {
            checkInline(r);
            checkMetrics(r);
        }
    }
}
