package com.example.bizagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.bizagent.modules.*.mapper")
public class BizAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(BizAgentApplication.class, args);
    }
}
