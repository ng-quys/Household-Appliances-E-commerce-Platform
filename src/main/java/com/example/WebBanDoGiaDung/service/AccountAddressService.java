package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.dto.ProfileAddressForm;
import com.example.WebBanDoGiaDung.dto.ProfileAddressView;
import com.example.WebBanDoGiaDung.entity.AccountAddress;
import java.util.List;

public interface AccountAddressService extends CrudService<AccountAddress, Integer> {
    List<AccountAddress> findByAccountId(Integer accountId);

    List<AccountAddress> findDefaultAddresses();

    List<AccountAddress> getAddressesByCurrentAccount(Integer accountId);

    List<ProfileAddressView> getAddressViewsByCurrentAccount(Integer accountId);

    AccountAddress addAddress(Integer accountId, ProfileAddressForm form);

    AccountAddress updateAddress(Integer accountId, ProfileAddressForm form);

    void deleteAddress(Integer accountId, Integer addressId);

    void validateAddressUsableForCheckout(AccountAddress address);

    void setDefaultAddress(Integer accountId, Integer addressId);

    AccountAddress getDefaultAddress(Integer accountId);

    AccountAddress getAddressForCheckout(Integer accountId, Integer addressId);
}
