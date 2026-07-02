package com.example.WebBanDoGiaDung;

import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class WebBanDoGiaDungApplication {

    private final CacheManager cacheManager;

    public WebBanDoGiaDungApplication(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

	public static void main(String[] args) {
		SpringApplication.run(WebBanDoGiaDungApplication.class, args);
	}

    @EventListener(ApplicationReadyEvent.class)
    public void clearProductCachesOnStartup() {
        List.of("productList", "productDetail", "productByGenre", "genreList")
                .forEach(cacheName -> {
                    var cache = cacheManager.getCache(cacheName);
                    if (cache != null) {
                        cache.clear();
                    }
                });
    }
}
