import com.google.gson.*;
import org.yaml.snakeyaml.Yaml;
import schemaExtraction.Extraction;
import schemaTransformation.capsules.Config;
import schemaTransformation.capsules.Relation;
import schemaTransformation.worker.Optimizer;
import schemaTransformation.worker.Transformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class Main {
    /** TODO
     *  - keep track of any renamings
     *  - before creating attributes, do name check
     *  - put 'anyOf' in a class constant
     *  - put path separator in a class constant
     *  - do name check before naming a node 'anyOf'
     *  - check property names for path separators
     *  - relation names must me unique
     *  - attribute mapping
     *  - relation tree?
     */

    public static void main(String[] args) {
        Config config = new Config();

        Extraction extraction = new Extraction(
                config.getString("mongodb.database"),
                config.getString("mongodb.collection") );
        extraction.run();

        JsonObject object = null;

        try {
            JsonParser parser = new JsonParser();
            object = parser.parse( extraction.getJsonSchema() ).getAsJsonObject();

        } catch (JsonSyntaxException e) {
            /** TODO
             *  handle syntax error
             */
            System.out.println("some syntax error");

        } catch (Exception e) {
            /** TODO
             *  handle random errors
             */
            System.out.println("something else happened");
            System.out.println(e);
        }

        if (object != null) {
            JsonObject properties = object.get("properties").getAsJsonObject();

            Transformer transformer = new Transformer(object.get("title").getAsString(), properties, config);
            transformer.run();
            transformer.print();

            for (Relation relation : transformer.getRelations()) {
                System.out.println(relation.toSQL(config));
            }

            Optimizer optimizer = new Optimizer(transformer.getRelations());
            optimizer.run();
            optimizer.printResults();
        }
    }
}
