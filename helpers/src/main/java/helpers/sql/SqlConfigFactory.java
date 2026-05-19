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

        try {
            config.loadFromProperties();
        } catch (Exception e) {
            System.out.println("⚠️  Could not load ebean.properties, using explicit configuration");
        }

        DataSourceConfig ds = new DataSourceConfig();

        String host = System.getenv("MYSQLHOST");
        String port = System.getenv("MYSQLPORT");
        String db = System.getenv("MYSQLDATABASE");
        String user = System.getenv("MYSQLUSER");
        String pass = System.getenv("MYSQLPASSWORD");

        // Fallback for local dev
        if (host == null) host = "localhost";
        if (port == null) port = "3306";
        if (db == null) db = "college_placement";
        if (user == null) user = "root";
        if (pass == null) pass = "PaZIjGjnKVEbyKjMqthELuxgVqVLBNgk";

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&allowPublicKeyRetrieval=true";

        ds.setUrl(jdbcUrl);
        ds.setUsername(user);
        ds.setPassword(pass);
        ds.setDriver("com.mysql.cj.jdbc.Driver");

        config.setDataSourceConfig(ds);

        this.database = DatabaseFactory.create(config);
        System.out.println("✅ Connected to MySQL: " + jdbcUrl);
        System.out.println("✅ Registered entity packages: models.sql, helpers.blueprint.models");
    }

    public Database getServer() {
        return database;
    }

    public static void init() {
        SqlConfigFactory.MASTER.getServer();
    }
}