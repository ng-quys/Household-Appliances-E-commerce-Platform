package com.example.WebBanDoGiaDung.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.SaveMode;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession(
        redisNamespace = "web-ban-do-gia-dung:session",
        maxInactiveIntervalInSeconds = 1800,
        saveMode = SaveMode.ALWAYS
)
public class RedisSessionConfig {
}
