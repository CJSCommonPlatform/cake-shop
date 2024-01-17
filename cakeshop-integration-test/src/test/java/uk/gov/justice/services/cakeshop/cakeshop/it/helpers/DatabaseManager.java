package uk.gov.justice.services.cakeshop.cakeshop.it.helpers;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;

public class DatabaseManager {

    private static final String POSTGRES_DRIVER = "org.postgresql.Driver";
    private static final TestProperties TEST_PROPERTIES = new TestProperties("test.properties");

    public DataSource initEventStoreDb()  {
        return initDatabase("db.eventstore.url", "db.eventstore.userName",
                "db.eventstore.password");
    }

    public DataSource initViewStoreDb()  {
        return initDatabase("db.cakeshop.url", "db.cakeshop.userName",
                "db.cakeshop.password");
    }

    public DataSource initSystemDb()  {
        return initDatabase("db.system.url", "db.system.userName",
                "db.system.password");
    }

    public DataSource initFileServiceDb()  {
        return initDatabase("db.fileservice.url", "db.fileservice.userName",
                "db.fileservice.password");
    }

    private static DataSource initDatabase(final String dbUrlPropertyName,
                                           final String dbUserNamePropertyName,
                                           final String dbPasswordPropertyName)  {
        final BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(POSTGRES_DRIVER);

        dataSource.setUrl(TEST_PROPERTIES.value(dbUrlPropertyName));
        dataSource.setUsername(TEST_PROPERTIES.value(dbUserNamePropertyName));
        dataSource.setPassword(TEST_PROPERTIES.value(dbPasswordPropertyName));

        return dataSource;
    }
}
