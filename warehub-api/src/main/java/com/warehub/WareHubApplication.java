package com.warehub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 仓枢 WareHub 应用启动类
 * 轻量化仓库管理系统 - 单体 Spring Boot 应用
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.warehub.mapper")
public class WareHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(WareHubApplication.class, args);
        System.out.println("============================================");
        System.out.println("   仓枢 WareHub 启动成功！");
        System.out.println("   PC 管理后台: http://localhost:8080/");
        System.out.println("   移动 H5:     http://localhost:8080/m");
        System.out.println("   API 文档:    http://localhost:8080/doc.html");
        System.out.println("============================================");
    }
}
