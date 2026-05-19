package helpers.sql;

import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import io.ebean.datasource.DataSourceConfig;

public enum SqlConfigFactory {
    MASTER;

    private final Database database;

    SqlConfigFactory() {
        DatabaseConfig config = new DatabaseConfig();
        config.setName("master");
        config.setRegister(true);
        config.setDefaultServer(true);

        config.addPackage("models.sql");
        config.addPackage("helpers.blueprint.models");

        config.setDdlGenerate(false);
        config.setDdlRun(false);

        // Loads app/src/main/resources/application.properties (datasource.db.*)
        config.loadFromProperties();

        // Railway / Docker env overrides (optional)
        String host = System.getenv("MYSQLHOST");
        if (host != null) {
            DataSourceConfig ds = config.getDataSourceConfig();
            if (ds == null) {
                ds = new DataSourceConfig();
                config.setDataSourceConfig(ds);
            }
            String port = envOr("MYSQLPORT", "3306");
            String db = envOr("MYSQLDATABASE", "railway");
            String user = envOr("MYSQLUSER", "root");
            String pass = envOr("MYSQLPASSWORD", "");
            ds.setUrl("jdbc:mysql://" + host + ":" + port + "/" + db
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            ds.setUsername(user);
            ds.setPassword(pass);
            ds.setDriver("com.mysql.cj.jdbc.Driver");
        }

        this.database = DatabaseFactory.create(config);
        DataSourceConfig ds = config.getDataSourceConfig();
        System.out.println("Connected to MySQL: " + (ds != null ? ds.getUrl() : "unknown"));
        System.out.println("Registered entity packages: models.sql, helpers.blueprint.models");
    }

    private static String envOr(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }

    public Database getServer() {
        return database;
    }

    public static void init() {
        SqlConfigFactory.MASTER.getServer();
    }
}
