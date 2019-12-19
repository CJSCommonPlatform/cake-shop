package uk.gov.justice.services.example.cakeshop.it.helpers;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import uk.gov.justice.services.jdbc.persistence.SystemJdbcDataSourceProvider;
import uk.gov.justice.services.system.persistence.EventErrorLogRepository;

import javax.sql.DataSource;

import org.mockito.InjectMocks;
import org.mockito.Mock;

public class EventErrorLogRepositoryFactory {

    @Mock
    private SystemJdbcDataSourceProvider systemJdbcDataSourceProvider;

    @InjectMocks
    private EventErrorLogRepository eventErrorLogRepository;


    public EventErrorLogRepository create(final DataSource systemDataSource) {

        initMocks(this);

        when(systemJdbcDataSourceProvider.getDataSource()).thenReturn(systemDataSource);

        return eventErrorLogRepository;
    }
}
