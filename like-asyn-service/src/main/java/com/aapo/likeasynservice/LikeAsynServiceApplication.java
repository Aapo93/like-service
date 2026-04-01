package com.aapo.likeasynservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.aapo.likeasynservice.mapper")
@SpringBootApplication
public class LikeAsynServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LikeAsynServiceApplication.class, args);
    }

}
