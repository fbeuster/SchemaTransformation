package schemaExtraction;

import schemaExtraction.extract.SchemaExtractor;
import schemaExtraction.io.Storage;

/**
 * Created by Felix Beuster on 10.06.2016.
 */
public class App {
    private Storage storage;

    public App() {}

    public Storage getStorage() {
        return storage;
    }

    public void run() {
        storage = new Storage();

        SchemaExtractor se = new SchemaExtractor(this, "test", "thesis_data_user");
        long count = se.countDocs();
        se.extractAll();
        se.close();
    }
}
