package uk.gov.justice.services.cakeshop.cakeshop.persistence;

import uk.gov.justice.services.cakeshop.cakeshop.persistence.entity.CakeOrder;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CakeOrderRepository extends EntityRepository<CakeOrder, UUID> {


}
