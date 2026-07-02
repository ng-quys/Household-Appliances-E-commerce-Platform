package com.example.WebBanDoGiaDung.repository;

import com.example.WebBanDoGiaDung.entity.AccountAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountAddressRepository extends JpaRepository<AccountAddress, Integer> {
    List<AccountAddress> findByAccountAccountId(Integer accountId);

    List<AccountAddress> findByAccountAccountIdOrderByIsDefaultDescAccountAddressIdDesc(Integer accountId);

    List<AccountAddress> findByIsDefault(Boolean isDefault);

    Optional<AccountAddress> findByAccountAddressIdAndAccountAccountId(Integer accountAddressId, Integer accountId);
}
