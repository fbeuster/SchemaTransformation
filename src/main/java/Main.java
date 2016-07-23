import com.google.gson.*;
import schemaExtraction.Extraction;
import schemaTransformation.capsules.Relation;
import schemaTransformation.worker.DataMover;
import schemaTransformation.worker.Optimizer;
import schemaTransformation.worker.Transformer;
import utils.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;


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

            System.out.println("+++ start optimization +++");

            Optimizer optimizer = new Optimizer(transformer.getRelations(), transformer.getCollisions());
            optimizer.run();

            System.out.println("+++ start data transfer +++");

            DataMover dataMover = new DataMover(transformer.getRelations(), transformer.getDataMappingLog());
            dataMover.run();

            System.out.println("+++ all done +++");

            Config config   = new Config();

            String host     = config.getString("sql.host");
            String port     = config.getString("sql.port");
            String dataabse = config.getString("sql.database");
            String user     = config.getString("sql.user");
            String password = config.getString("sql.password");

            String url      = "jdbc:mysql://" + host + ":" + port + "/" + dataabse;

            try (Connection connection = DriverManager.getConnection(url, user, password)) {
                System.out.println("Database connected!");

                for (Map.Entry<String, Relation> entry : transformer.getRelations().entrySet()) {
                    System.out.println(entry.getValue().toSQL(new Config()));
                    Statement statement = connection.createStatement();
                    statement.execute( entry.getValue().toSQL(new Config()) );
                    statement.close();
                }

                for (String sql : dataMover.getStatements()) {
                    System.out.println(sql);
                    Statement statement = connection.createStatement();
                    statement.execute( sql );
                }

            } catch (SQLException e) {
                throw new IllegalStateException("Cannot connect the database!", e);
            }
        }
    }
}
