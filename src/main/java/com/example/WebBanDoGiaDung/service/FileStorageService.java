package com.example.WebBanDoGiaDung.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeProductImage(MultipartFile file);

    String storeBrandImage(MultipartFile file);
}
