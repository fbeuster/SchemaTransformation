package schemaExtraction;

import schemaExtraction.extract.SchemaExtractor;
import schemaExtraction.io.Storage;
import schemaExtraction.visualize.Visualizer;

import java.util.Calendar;

/**
 * Created by Felix Beuster on 10.06.2016.
 */
public class Extraction {
    private Storage storage;

    public Extraction() {}

    public Storage getStorage() {
        return storage;
    }

    public void run() {
        storage = new Storage();

        System.out.println();
        System.out.println("######## Testarea for retrieving a MongoDB collection and iterating through the documents with the extractSchema algorithm ########");

        SchemaExtractor se = new SchemaExtractor(this, "test", "thesis_data_user");
        long docCount = se.countDocs();

        Calendar startExtraction = Calendar.getInstance();
        se.extractAll();
        Calendar endExtraction = Calendar.getInstance();
        se.close();

        if (Configuration.PRINT_NODES_EDGES) {
            storage.printNodes(docCount);
            storage.printEdges(docCount);
        }

        Visualizer visualizer = new Visualizer(storage, "test");
        System.out.println(visualizer.toString());
        System.out.println();
    }
}
