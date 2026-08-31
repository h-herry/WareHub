package com.warehub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
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
        ConfigurableApplicationContext ctx = SpringApplication.run(WareHubApplication.class, args);
        // 端口与上下文路径取自运行时配置，避免非 8080 端口部署时打印错误地址
        String port = ctx.getEnvironment().getProperty("server.port", "8080");
        String path = ctx.getEnvironment().getProperty("server.servlet.context-path", "/");
        String base = "http://localhost:" + port + (path == null || path.equals("/") ? "" : path);
        System.out.println("============================================");
        System.out.println("   仓枢 WareHub 启动成功！");
        System.out.println("   PC 管理后台: " + base + "/");
        System.out.println("   移动 H5:     " + base + "/m");
        System.out.println("   API 文档:    " + base + "/doc.html");
        System.out.println("============================================");
    }
}
