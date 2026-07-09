package com.warehub.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 多端入口路由：单 JAR 托管三端静态资源
 * / → PC 管理后台, /m → 移动 H5, /mp → 小程序 H5 兜底
 * SPA 深链回退到对应 index.html；真实静态资源由 Spring 静态资源处理器优先匹配
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String admin() {
        // 使用 redirect 而非 forward：浏览器地址变为 /admin/index.html，
        // 前端 index.html 内资源为相对路径 ./assets/*，重定向后才能正确解析为 /admin/assets/*，避免白屏
        return "redirect:/admin/index.html";
    }

    @GetMapping("/m")
    public String mobile() {
        return "redirect:/mobile/index.html";
    }

    @GetMapping("/mp")
    public String mp() {
        // 小程序 H5 兜底：复用移动 H5（如未来有独立 mp 构建，改为 /mp/index.html）
        return "redirect:/mobile/index.html";
    }

    /** 移动端 SPA 深链回退（hash 路由下极少用到，统一重定向到移动端入口，保证资源路径正确） */
    @GetMapping("/m/{path}")
    public String mobileDeep(@PathVariable String path) {
        return "redirect:/mobile/index.html";
    }

    @GetMapping("/mp/{path}")
    public String mpDeep(@PathVariable String path) {
        return "redirect:/mobile/index.html";
    }
}
