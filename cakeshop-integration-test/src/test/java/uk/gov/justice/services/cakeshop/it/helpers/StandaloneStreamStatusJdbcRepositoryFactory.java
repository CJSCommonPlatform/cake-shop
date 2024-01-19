package uk.gov.justice.services.cakeshop.it.helpers;

import uk.gov.justice.services.event.buffer.core.repository.subscription.StreamStatusJdbcRepository;
import uk.gov.justice.services.jdbc.persistence.PreparedStatementWrapperFactory;

import javax.sql.DataSource;

public class StandaloneStreamStatusJdbcRepositoryFactory {

    public StreamStatusJdbcRepository getStreamStatusJdbcRepository(final DataSource dataSource) {

        return new StreamStatusJdbcRepository(dataSource, new PreparedStatementWrapperFactory());
    }
}
