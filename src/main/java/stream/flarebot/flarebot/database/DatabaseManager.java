package stream.flarebot.flarebot.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stream.flarebot.flarebot.Config;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private static HikariConfig config;
    private static HikariDataSource ds;

    public static void init() {
        config = new HikariConfig();

        config.setDataSource(new PGSimpleDataSource());
        config.setUsername(Config.INS.getDatabaseUsername());
        config.setPassword(Config.INS.getDatabasePassword());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "100");

        ds = new HikariDataSource(config);
    }

    public static void run(Query query) {
        try {
            Connection connection = ds.getConnection();
            query.run(connection);
            connection.close();
        } catch (SQLException e) {
            logger.error("SQL query failed", e);
        }
    }

    public static HikariConfig getConfig() {
        return config;
    }
}
