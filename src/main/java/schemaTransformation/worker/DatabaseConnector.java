package schemaTransformation.worker;

import schemaTransformation.capsules.Relation;
import utils.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;

/**
 * Created by Felix Beuster on 24.07.2016.
 */
public class DatabaseConnector {

    private ArrayList<String> statements;

    private Boolean debug;
    private Boolean drop;

    private Connection connection;

    private LinkedHashMap<String, Relation> relations;

    private String database;
    private String host;
    private String password;
    private String port;
    private String user;

    public DatabaseConnector(LinkedHashMap<String, Relation> relations, ArrayList<String> statements) {
        this.relations = relations;
        this.statements = statements;

        loadConfig();
    }

    private void dropTables() throws SQLException {
        /** clear relations **/

        /** Due to the foreign key restrains, the tables have to be dropped in reverse order. **/

        ListIterator<Map.Entry<String, Relation>> iterator = new ArrayList<>(relations.entrySet())
                                                                    .listIterator(relations.size());

        while (iterator.hasPrevious()) {
            Map.Entry<String, Relation> entry = iterator.previous();
            Statement statement = connection.createStatement();
            String drop         = "DROP TABLE IF EXISTS `" + entry.getValue().getName() + "`;";

            if (debug) {
                System.out.println(drop);
            }

            statement.execute( drop );
            statement.close();
        }
    }

    private void execute() throws SQLException {
        /** create relations **/
        for (Map.Entry<String, Relation> entry :relations.entrySet()) {
            if (debug) {
                System.out.println(entry.getValue().toSQL(new Config()));
            }

            Statement statement = connection.createStatement();
            statement.execute( entry.getValue().toSQL(new Config()) );
            statement.close();
        }

        /** fill relations **/
        for (String sql : statements) {
            if (debug) {
                System.out.println(sql);
            }

            Statement statement = connection.createStatement();
            statement.execute( sql );
        }
    }

    private void loadConfig() {
        Config config   = new Config();

        database = config.getString("sql.database");
        debug    = config.getBoolean("sql.debug");
        drop     = config.getBoolean("sql.drop_tables");
        host     = config.getString("sql.host");
        password = config.getString("sql.password");
        port     = config.getString("sql.port");
        user     = config.getString("sql.user");
    }

    public void run() {
        String url      = "jdbc:mysql://" + host + ":" + port + "/" + database;

        try {
            connection = DriverManager.getConnection(url, user, password);

            if (drop) {
                dropTables();
            }

            execute();

        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
