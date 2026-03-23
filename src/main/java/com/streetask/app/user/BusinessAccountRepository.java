package com.streetask.app.user;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface BusinessAccountRepository extends CrudRepository<BusinessAccount, UUID> {

    Optional<BusinessAccount> findById(UUID id);

    Optional<BusinessAccount> findByTaxId(String taxId);

    Optional<BusinessAccount> findByEmailAndTaxId(String email, String taxId);

    Boolean existsByTaxId(String taxId);

    List<BusinessAccount> findAllByRequestStatus(RequestStatus requestStatus);

}
