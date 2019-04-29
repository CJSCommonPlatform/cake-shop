package uk.gov.justice.services.example.cakeshop.query.view.service;

import uk.gov.justice.services.example.cakeshop.persistence.IndexRepository;
import uk.gov.justice.services.example.cakeshop.persistence.entity.Index;
import uk.gov.justice.services.example.cakeshop.query.view.response.IndexView;

import java.util.UUID;

import javax.inject.Inject;

public class IndexService {

    @Inject
    private IndexRepository indexRepository;

    public IndexView findIndexBy(final String indexId) {
        final Index index = indexRepository.findBy(UUID.fromString(indexId));
        return index != null ? new IndexView(index.getIndexId(), index.getDeliveryDate()) : null;
    }
}

