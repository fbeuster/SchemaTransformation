import com.google.gson.*;
import schemaExtraction.Extraction;
import schemaTransformation.capsules.Relation;
import schemaTransformation.worker.Optimizer;
import schemaTransformation.worker.Transformer;
import utils.Config;

import java.util.HashMap;
import java.util.LinkedHashMap;


public class Main {
    /** TODO
     *  - keep track of any renamings
     *  - put 'anyOf' in a class constant
     *  - put path separator in a class constant
     *  - do name check before naming a node 'anyOf'
     *  - check property names for path separators
     *  - relation tree?
     */

    public static void main(String[] args) {
        Extraction extraction = new Extraction();
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

            Transformer transformer = new Transformer(object.get("title").getAsString(), properties);
            transformer.run();
            transformer.print();

            LinkedHashMap<String, Relation> relations = transformer.getRelations();
            for (String name : relations.keySet()) {
                System.out.println(relations.get(name).toSQL( new Config() ));
            }

            Optimizer optimizer = new Optimizer(transformer.getRelations(), transformer.getCollisions());
            optimizer.run();
            optimizer.printResults();

            System.out.println(transformer.getDataMappingLog());
        }
    }
}
