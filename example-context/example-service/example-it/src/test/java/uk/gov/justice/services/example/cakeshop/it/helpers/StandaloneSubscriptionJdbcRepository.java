package uk.gov.justice.services.example.cakeshop.it.helpers;



import uk.gov.justice.services.event.source.subscriptions.repository.jdbc.SubscriptionsRepository;

import javax.sql.DataSource;

public class StandaloneSubscriptionJdbcRepository extends SubscriptionsRepository {
    private final DataSource datasource;

    public StandaloneSubscriptionJdbcRepository(final DataSource datasource) {
        this.datasource = datasource;
    }

    protected DataSource getDataSource() {
        return datasource;
    }
}
