import com.google.gson.*;
import schemaExtraction.Extraction;
import schemaTransformation.worker.Transformer;


public class Main {
    /** TODO
     *  - keep track of any renamings
     *  - before creating attributes, do name check
     *  - put 'anyOf' in a class constant
     *  - put path separator in a class constant
     *  - do name check before naming a node 'anyOf'
     *  - check property names for path separators
     */

    public static void main(String[] args) {
        Extraction extraction = new Extraction("test", "car_orders");
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

            Transformer transformer = new Transformer(object.get("title").toString(), properties);
            transformer.run();
            transformer.print();
        }
    }
}
