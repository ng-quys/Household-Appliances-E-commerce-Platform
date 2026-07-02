package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.dto.ProfileAddressForm;
import com.example.WebBanDoGiaDung.dto.ProfileAddressView;
import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.entity.AccountAddress;
import com.example.WebBanDoGiaDung.entity.District;
import com.example.WebBanDoGiaDung.entity.Province;
import com.example.WebBanDoGiaDung.entity.Ward;
import com.example.WebBanDoGiaDung.repository.AccountAddressRepository;
import com.example.WebBanDoGiaDung.repository.AccountRepository;
import com.example.WebBanDoGiaDung.repository.DistrictRepository;
import com.example.WebBanDoGiaDung.repository.ProvinceRepository;
import com.example.WebBanDoGiaDung.repository.WardRepository;
import com.example.WebBanDoGiaDung.service.AccountAddressService;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountAddressServiceImpl extends AbstractCrudService<AccountAddress, Integer> implements AccountAddressService {

    private final AccountAddressRepository repository;
    private final AccountRepository accountRepository;
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;

    @Override
    protected JpaRepository<AccountAddress, Integer> getRepository() {
        return repository;
    }

    @Override
    public List<AccountAddress> findByAccountId(Integer accountId) {
        return repository.findByAccountAccountId(accountId);
    }

    @Override
    public List<AccountAddress> findDefaultAddresses() {
        return repository.findByIsDefault(true);
    }

    @Override
    public List<AccountAddress> getAddressesByCurrentAccount(Integer accountId) {
        return repository.findByAccountAccountIdOrderByIsDefaultDescAccountAddressIdDesc(accountId);
    }

    @Override
    public List<ProfileAddressView> getAddressViewsByCurrentAccount(Integer accountId) {
        return repository.findByAccountAccountIdOrderByIsDefaultDescAccountAddressIdDesc(accountId)
                .stream()
                .map(this::toAddressView)
                .toList();
    }

    @Override
    @Transactional
    public AccountAddress addAddress(Integer accountId, ProfileAddressForm form) {
        validateForm(form);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
        AddressLocation location = resolveAndValidateLocation(form);

        List<AccountAddress> existingAddresses = getAddressesByCurrentAccount(accountId);
        boolean shouldBeDefault = existingAddresses.isEmpty() || Boolean.TRUE.equals(form.getIsDefault());
        if (shouldBeDefault) {
            clearDefaultForAccount(accountId);
        }

        AccountAddress address = new AccountAddress();
        address.setAccount(account);
        address.setProvince(location.province());
        address.setDistrict(location.district());
        address.setWard(location.ward());
        address.setAccountUsername(form.getAccountUsername().trim());
        address.setAccountPhoneNumber(form.getAccountPhoneNumber().trim());
        address.setContent(form.getContent().trim());
        address.setIsDefault(shouldBeDefault);
        return repository.save(address);
    }

    @Override
    @Transactional
    public AccountAddress updateAddress(Integer accountId, ProfileAddressForm form) {
        if (form.getAddressId() == null) {
            throw new IllegalArgumentException("Thiếu mã địa chỉ cần sửa");
        }
        validateForm(form);
        AccountAddress address = repository.findByAccountAddressIdAndAccountAccountId(form.getAddressId(), accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ hợp lệ"));
        AddressLocation location = resolveAndValidateLocation(form);

        if (Boolean.TRUE.equals(form.getIsDefault())) {
            clearDefaultForAccount(accountId);
            address.setIsDefault(true);
        }

        address.setProvince(location.province());
        address.setDistrict(location.district());
        address.setWard(location.ward());
        address.setAccountUsername(form.getAccountUsername().trim());
        address.setAccountPhoneNumber(form.getAccountPhoneNumber().trim());
        address.setContent(form.getContent().trim());
        if (!Boolean.TRUE.equals(form.getIsDefault()) && address.getIsDefault() == null) {
            address.setIsDefault(false);
        }
        return repository.save(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Integer accountId, Integer addressId) {
        AccountAddress address = repository.findByAccountAddressIdAndAccountAccountId(addressId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ hợp lệ"));
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        repository.delete(address);

        if (wasDefault) {
            List<AccountAddress> remainingAddresses = getAddressesByCurrentAccount(accountId);
            if (!remainingAddresses.isEmpty()) {
                AccountAddress nextDefault = remainingAddresses.get(0);
                clearDefaultForAccount(accountId);
                nextDefault.setIsDefault(true);
                repository.save(nextDefault);
            }
        }
    }

    @Override
    @Transactional
    public void setDefaultAddress(Integer accountId, Integer addressId) {
        AccountAddress address = repository.findByAccountAddressIdAndAccountAccountId(addressId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ hợp lệ"));
        clearDefaultForAccount(accountId);
        address.setIsDefault(true);
        repository.save(address);
    }

    @Override
    public AccountAddress getDefaultAddress(Integer accountId) {
        return repository.findByAccountAccountId(accountId).stream()
                .filter(address -> Boolean.TRUE.equals(address.getIsDefault()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản chưa có địa chỉ mặc định"));
    }

    @Override
    public AccountAddress getAddressForCheckout(Integer accountId, Integer addressId) {
        if (accountId == null || addressId == null) {
            throw new IllegalArgumentException("missing_checkout_address");
        }

        AccountAddress address = repository.findByAccountAddressIdAndAccountAccountId(addressId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("invalid_checkout_address"));
        validateAddressUsableForCheckout(address);
        return address;
    }

    private void validateForm(ProfileAddressForm form) {
        if (form == null) {
            throw new IllegalArgumentException("Dữ liệu địa chỉ không hợp lệ");
        }
        if (form.getAccountUsername() == null || form.getAccountUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Họ tên người nhận không được để trống");
        }
        if (form.getAccountPhoneNumber() == null || form.getAccountPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Số điện thoại không được để trống");
        }
        if (form.getAccountPhoneNumber().trim().length() > 10) {
            throw new IllegalArgumentException("Số điện thoại tối đa 10 ký tự");
        }
        if (form.getProvinceId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn Tỉnh/Thành phố");
        }
        if (form.getDistrictId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn Quận/Huyện");
        }
        if (form.getWardId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn Phường/Xã");
        }
        if (form.getContent() == null || form.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Địa chỉ cụ thể không được để trống");
        }
    }

    private void clearDefaultForAccount(Integer accountId) {
        List<AccountAddress> addresses = repository.findByAccountAccountId(accountId);
        for (AccountAddress item : addresses) {
            if (Boolean.TRUE.equals(item.getIsDefault())) {
                item.setIsDefault(false);
                repository.save(item);
            }
        }
    }

    private ProfileAddressView toAddressView(AccountAddress address) {
        String provinceName = address.getProvinceId() != null
                ? provinceRepository.findById(address.getProvinceId()).map(Province::getProvinceName).orElse("")
                : "";

        String districtName = address.getDistrictId() != null
                ? districtRepository.findById(address.getDistrictId()).map(District::getDistrictName).orElse("")
                : "";

        String wardName = address.getWardId() != null
                ? wardRepository.findById(address.getWardId()).map(Ward::getWardName).orElse("")
                : "";

        return ProfileAddressView.builder()
                .accountAddressId(address.getAccountAddressId())
                .accountUsername(address.getAccountUsername())
                .accountPhoneNumber(address.getAccountPhoneNumber())
                .provinceId(address.getProvinceId())
                .districtId(address.getDistrictId())
                .wardId(address.getWardId())
                .provinceName(provinceName)
                .districtName(districtName)
                .wardName(wardName)
                .content(address.getContent())
                .isDefault(Boolean.TRUE.equals(address.getIsDefault()))
                .build();
    }

    private AddressLocation resolveAndValidateLocation(ProfileAddressForm form) {
        Province province = provinceRepository.findById(form.getProvinceId())
                .orElseThrow(() -> new IllegalArgumentException("Tỉnh/Thành phố không hợp lệ"));

        District district = districtRepository.findById(form.getDistrictId())
                .orElseThrow(() -> new IllegalArgumentException("Quận/Huyện không hợp lệ"));

        Ward ward = wardRepository.findById(form.getWardId())
                .orElseThrow(() -> new IllegalArgumentException("Phường/Xã không hợp lệ"));

        if (!form.getProvinceId().equals(district.getProvinceId())) {
            throw new IllegalArgumentException("Quận/Huyện không thuộc Tỉnh/Thành phố đã chọn");
        }

        if (!form.getDistrictId().equals(ward.getDistrictId())) {
            throw new IllegalArgumentException("Phường/Xã không thuộc Quận/Huyện đã chọn");
        }

        return new AddressLocation(province, district, ward);
    }

    private record AddressLocation(Province province, District district, Ward ward) {
    }


    //giúp chặn trước khi save OrderAddress, tránh lỗi database kiểu foreign key constraint fails
    @Override
    public void validateAddressUsableForCheckout(AccountAddress address) {
        if (address == null) {
            throw new IllegalArgumentException("missing_default_address");
        }

        if (address.getProvinceId() == null
                || address.getDistrictId() == null
                || address.getWardId() == null
                || address.getContent() == null
                || address.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("invalid_default_address");
        }

        boolean provinceExists = provinceRepository.existsById(address.getProvinceId());
        boolean districtExists = districtRepository.existsById(address.getDistrictId());
        boolean wardExists = wardRepository.existsById(address.getWardId());

        if (!provinceExists || !districtExists || !wardExists) {
            throw new IllegalArgumentException("invalid_default_address");
        }
    }
}
