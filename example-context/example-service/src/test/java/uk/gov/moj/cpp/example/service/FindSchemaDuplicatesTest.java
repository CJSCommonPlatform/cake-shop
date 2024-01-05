package uk.gov.moj.cpp.example.service;

import uk.gov.justice.services.test.utils.core.schema.SchemaDuplicateTestHelper;

import org.junit.jupiter.api.Test;

public class FindSchemaDuplicatesTest {

    @Test
    public void shouldFindSchemaDuplicatesTest() {
        SchemaDuplicateTestHelper.failTestIfDifferentSchemasWithSameName();
    }
}
