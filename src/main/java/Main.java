import com.google.gson.*;
import schemaExtraction.Extraction;
import schemaTransformation.worker.DataMover;
import schemaTransformation.worker.Optimizer;
import schemaTransformation.worker.Transformer;


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

        System.out.println("+++ start schema extraction +++");

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

            Transformer transformer = new Transformer(object.get("title").getAsString(), properties);
            transformer.run();
            transformer.print();

            System.out.println("+++ start optimization +++");

            Optimizer optimizer = new Optimizer(transformer.getRelations(), transformer.getCollisions());
            optimizer.run();

            System.out.println("+++ start data transfer +++");

            DataMover dataMover = new DataMover(transformer.getRelations(), transformer.getDataMappingLog());
            dataMover.run();
            dataMover.print();

            System.out.println("+++ all done +++");
        }
    }
}
