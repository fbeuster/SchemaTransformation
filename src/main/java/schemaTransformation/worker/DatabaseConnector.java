package schemaTransformation.worker;

import schemaTransformation.capsules.Relation;
import utils.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Felix Beuster on 24.07.2016.
 */
public class DatabaseConnector {

    private ArrayList<String> statements;

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

    private void execute() throws SQLException {
        /** create relations **/
        for (Map.Entry<String, Relation> entry :relations.entrySet()) {
            System.out.println(entry.getValue().toSQL(new Config()));
            Statement statement = connection.createStatement();
            statement.execute( entry.getValue().toSQL(new Config()) );
            statement.close();
        }

        /** fill relations **/
        for (String sql : statements) {
            System.out.println(sql);
            Statement statement = connection.createStatement();
            statement.execute( sql );
        }
    }

    private void loadConfig() {
        Config config   = new Config();

        database = config.getString("sql.database");
        host     = config.getString("sql.host");
        password = config.getString("sql.password");
        port     = config.getString("sql.port");
        user     = config.getString("sql.user");
    }

    public void run() {
        String url      = "jdbc:mysql://" + host + ":" + port + "/" + database;

        try {
            connection = DriverManager.getConnection(url, user, password);
            execute();

        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
