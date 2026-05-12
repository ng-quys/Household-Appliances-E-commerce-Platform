package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.entity.Contact;
import java.util.List;

public interface ContactService extends CrudService<Contact, Integer> {
    List<Contact> findByStatus(String status);
}
