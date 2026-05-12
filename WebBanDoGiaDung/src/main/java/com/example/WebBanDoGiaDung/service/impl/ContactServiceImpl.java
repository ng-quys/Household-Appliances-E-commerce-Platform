package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.entity.Contact;
import com.example.WebBanDoGiaDung.repository.ContactRepository;
import com.example.WebBanDoGiaDung.service.ContactService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl extends AbstractCrudService<Contact, Integer> implements ContactService {

    private final ContactRepository repository;

    @Override
    protected JpaRepository<Contact, Integer> getRepository() {
        return repository;
    }

    @Override
    public List<Contact> findByStatus(String status) {
        return repository.findByStatus(status);
    }
}
