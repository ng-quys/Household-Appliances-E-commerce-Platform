package com.example.WebBanDoGiaDung.controller.api;

import com.example.WebBanDoGiaDung.service.BrandService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/brandapi")
public class BrandApiController {

    private final BrandService brandService;

    public BrandApiController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping
    public Object getBrands() {
        return brandService.findAll().stream()
                .map(b -> Map.of("brand_id", b.getBrandId(), "brand_name", b.getBrandName()))
                .toList();
    }
}
