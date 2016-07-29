import com.google.gson.*;
import schemaExtraction.Extraction;
import schemaTransformation.worker.DataMover;
import schemaTransformation.worker.DatabaseConnector;
import schemaTransformation.worker.Optimizer;
import schemaTransformation.worker.Transformer;
import utils.Config;

import java.util.Calendar;


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
        Config config = new Config();

        for (int i = 0; i < config.getInt("main.runs"); i++) {
            System.out.println("+++ start run " + (i + 1) + " +++");
            run();
        }
    }

    private static void run() {
        System.out.println("+++ start schema extraction +++");

        Calendar startExtraction = Calendar.getInstance();
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

            System.out.println("+++ start schema transform +++");


            Calendar startTransform = Calendar.getInstance();
            Transformer transformer = new Transformer(object.get("title").getAsString(), properties);
            transformer.run();

            System.out.println("+++ start optimization +++");

            Calendar startOpti = Calendar.getInstance();
            Optimizer optimizer = new Optimizer(transformer.getRelations(), transformer.getCollisions());
            optimizer.run();

            System.out.println("+++ start data transfer +++");

            Calendar startMove = Calendar.getInstance();
            DataMover dataMover = new DataMover(transformer.getRelations(), transformer.getDataMappingLog());
            dataMover.run();

            System.out.println("+++ start SQL handling +++");

            Calendar startDB = Calendar.getInstance();
            DatabaseConnector databaseConnector = new DatabaseConnector(transformer.getRelations(), dataMover.getStatements());
            databaseConnector.run();

            System.out.println("+++ all done +++");

            Calendar endAll = Calendar.getInstance();

            System.out.println();
            System.out.println("Runtimes: ");
            System.out.println("-------------------");
            System.out.println("extraction  " + (startTransform.getTimeInMillis() - startExtraction.getTimeInMillis()) + " ms");
            System.out.println("transform   " + (startOpti.getTimeInMillis() - startTransform.getTimeInMillis()) + " ms");
            System.out.println("optimize    " + (startMove.getTimeInMillis() - startOpti.getTimeInMillis()) + " ms");
            System.out.println("data map    " + (startDB.getTimeInMillis() - startMove.getTimeInMillis()) + " ms");
            System.out.println("db transfer " + (endAll.getTimeInMillis() - startDB.getTimeInMillis()) + " ms");
            System.out.println("-------------------");
            System.out.println("total       " + (endAll.getTimeInMillis() - startExtraction.getTimeInMillis()) + " ms");
        }
    }
}
