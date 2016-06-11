package schemaExtraction;

import schemaExtraction.extract.SchemaExtractor;
import schemaExtraction.io.Storage;
import schemaExtraction.update.SchemaMerger;
import schemaExtraction.visualize.Visualizer;

import java.util.Calendar;

/**
 * Created by Felix Beuster on 10.06.2016.
 */
public class Extraction {

    private long docCount;

    private String collection;
    private String database;

    private Storage storage;

    public Extraction(String database, String collection) {
        this.database = database;
        this.collection = collection;
    }

    public Storage getStorage() {
        return storage;
    }

    public void run() {
        storage = new Storage();

        // mandatory steps
        extract();

        // optional steps
        printObjects();
        visualize();
        saveAndLoad();
        merge("thesis_data_userMerge");
    }

    private void extract() {
        System.out.println();
        System.out.println("######## Testarea for retrieving a MongoDB collection and iterating through the documents with the extractSchema algorithm ########");

        SchemaExtractor se = new SchemaExtractor(this, database, collection);
        docCount = se.countDocs();

        Calendar startExtraction = Calendar.getInstance();
        se.extractAll();
        Calendar endExtraction = Calendar.getInstance();
        se.close();
    }

    private void merge(String collectionName) {
        if (Configuration.MERGE_STORAGES) {
            System.out.println("######## Testarea for merging 2 schemes ########");

            Storage newStorage = new Storage();
            newStorage.loadFromFile(Storage.DEFAULT_PATH, collectionName);

            System.out.println(storage.getNode(5).countDocId());
            System.out.println(newStorage.getNode(5).countDocId());

            SchemaMerger sm = new SchemaMerger(storage);
            sm.mergeWithStorage(newStorage);

            System.out.println(storage.getNode(5).countDocId());
            System.out.println(newStorage.getNode(5).countDocId());

            visualize();
        }
    }

    private void printObjects() {
        if (Configuration.PRINT_NODES_EDGES) {
            storage.printNodes(docCount);
            storage.printEdges(docCount);
        }
    }

    private void saveAndLoad() {
        if (Configuration.SAVE_AND_LOAD) {
            System.out.println("######## Testarea for storing and loading the internal schema to and from file ########");

            Calendar startSaveAndLoad = Calendar.getInstance();
            storage.saveToFile(Storage.DEFAULT_PATH, collection);
            Calendar endSaveStartLoad = Calendar.getInstance();

            storage.flush();
            storage.loadFromFile(Storage.DEFAULT_PATH, collection);
            Calendar endLoad = Calendar.getInstance();

            visualize();
        }
    }

    private void visualize() {
        Visualizer visualizer = new Visualizer(storage, database);
        System.out.println(visualizer.toString());
        System.out.println();
    }
}
