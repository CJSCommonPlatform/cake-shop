package uk.gov.justice.services.cakeshop.cakeshop.healthcheck;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.healthcheck.healthchecks.JobStoreHealthcheck.JOB_STORE_HEALTHCHECK_NAME;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CakeShopIgnoredHealthcheckNamesProviderTest {

    @InjectMocks
    private CakeShopIgnoredHealthcheckNamesProvider ignoredHealthcheckNamesProvider;

    @Test
    public void shouldGetListOfAllHealthchecksToIgnore() throws Exception {

        final List<String> ignoredHealthChecks = ignoredHealthcheckNamesProvider.getNamesOfIgnoredHealthChecks();

        assertThat(ignoredHealthChecks.size(), is(1));
        assertThat(ignoredHealthChecks.get(0), is(JOB_STORE_HEALTHCHECK_NAME));
    }
}