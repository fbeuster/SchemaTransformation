import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;
import schemaExtraction.Extraction;
import schemaTransformation.capsules.Attribute;
import schemaTransformation.capsules.Relation;
import schemaTransformation.worker.Transformer;

import java.io.IOException;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Extraction extraction = new Extraction("test", "thesis_data_user");
        extraction.run();

        JsonObject object = null;

        try {
            JsonParser parser = new JsonParser();
            object = parser.parse( extraction.getJsonSchema() ).getAsJsonObject();

        } catch (JsonSyntaxException e) {
            System.out.println("some syntax error");

        } catch (Exception e) {
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
