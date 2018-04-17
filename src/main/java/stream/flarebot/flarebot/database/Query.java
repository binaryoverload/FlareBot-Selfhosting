package stream.flarebot.flarebot.database;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface Query {

    void run(Connection connection) throws SQLException;
}
