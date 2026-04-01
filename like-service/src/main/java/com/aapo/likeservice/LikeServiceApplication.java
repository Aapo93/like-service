package com.aapo.likeservice;

import com.aapo.api.config.DefaultFeignConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.aapo.api.client", defaultConfiguration = DefaultFeignConfig.class)
@SpringBootApplication
public class LikeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LikeServiceApplication.class, args);
    }

}
