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
        long[][] runtimes = new long[config.getInt("main.runs")][6];

        for (int i = 0; i < config.getInt("main.runs"); i++) {
            System.out.println("+++ start run " + (i + 1) + " +++");

            System.out.println("+++ start configuration check +++");

            if (!config.isValid()) {
                break;
            }

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
                Optimizer optimizer = new Optimizer(transformer.getRelations());
                optimizer.check();
                optimizer.printResults();

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

                runtimes[i][0] = startTransform.getTimeInMillis() - startExtraction.getTimeInMillis();
                runtimes[i][1] = startOpti.getTimeInMillis() - startTransform.getTimeInMillis();
                runtimes[i][2] = startMove.getTimeInMillis() - startOpti.getTimeInMillis();
                runtimes[i][3] = startDB.getTimeInMillis() - startMove.getTimeInMillis();
                runtimes[i][4] = endAll.getTimeInMillis() - startDB.getTimeInMillis();
                runtimes[i][5] = endAll.getTimeInMillis() - startExtraction.getTimeInMillis();

                System.out.println();
                System.out.println("Runtimes: ");
                System.out.println("-------------------");
                System.out.println("extraction  " + runtimes[i][0] + " ms");
                System.out.println("transform   " + runtimes[i][1] + " ms");
                System.out.println("optimize    " + runtimes[i][2] + " ms");
                System.out.println("data map    " + runtimes[i][3] + " ms");
                System.out.println("db transfer " + runtimes[i][4] + " ms");
                System.out.println("-------------------");
                System.out.println("total       " + runtimes[i][5] + " ms");
            }
        }

        if (config.getBoolean("main.debug.total_runtimes")) {
            System.out.println();
            System.out.println("Average runtimes: ");
            System.out.println("-------------------");
            System.out.println("extraction  " + columnAvg(runtimes, 0) + " ms");
            System.out.println("transform   " + columnAvg(runtimes, 1) + " ms");
            System.out.println("optimize    " + columnAvg(runtimes, 2) + " ms");
            System.out.println("data map    " + columnAvg(runtimes, 3) + " ms");
            System.out.println("db transfer " + columnAvg(runtimes, 4) + " ms");
            System.out.println("-------------------");
            System.out.println("total       " + columnAvg(runtimes, 5) + " ms");

            System.out.println();
            System.out.println("Maximum runtimes: ");
            System.out.println("-------------------");
            System.out.println("extraction  " + columnMax(runtimes, 0) + " ms");
            System.out.println("transform   " + columnMax(runtimes, 1) + " ms");
            System.out.println("optimize    " + columnMax(runtimes, 2) + " ms");
            System.out.println("data map    " + columnMax(runtimes, 3) + " ms");
            System.out.println("db transfer " + columnMax(runtimes, 4) + " ms");
            System.out.println("-------------------");
            System.out.println("total       " + columnMax(runtimes, 5) + " ms");

            System.out.println();
            System.out.println("Minimum runtimes: ");
            System.out.println("-------------------");
            System.out.println("extraction  " + columnMin(runtimes, 0) + " ms");
            System.out.println("transform   " + columnMin(runtimes, 1) + " ms");
            System.out.println("optimize    " + columnMin(runtimes, 2) + " ms");
            System.out.println("data map    " + columnMin(runtimes, 3) + " ms");
            System.out.println("db transfer " + columnMin(runtimes, 4) + " ms");
            System.out.println("-------------------");
            System.out.println("total       " + columnMin(runtimes, 5) + " ms");
        }
    }

    private static float columnAvg(long[][] array, int column) {
        long sum = 0;

        for(int row = 0; row < array.length; row++) {
            sum += array[row][column];
        }

        return sum / array.length;
    }

    private static long columnMax(long[][] array, int column) {
        long max = 0;

        for(int row = 0; row < array.length; row++) {
            if (array[row][column] > max) {
                max = array[row][column];
            }
        }

        return max;
    }

    private static long columnMin(long[][] array, int column) {
        long min = Integer.MAX_VALUE;

        for(int row = 0; row < array.length; row++) {
            if (array[row][column] < min) {
                min = array[row][column];
            }
        }

        return min;
    }
}
