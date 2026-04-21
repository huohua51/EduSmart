package com.edusmart.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * EduSmart 后端服务启动类。
 *
 * <p>替代原 CloudBase 云函数（cloudbase-auth / cloudbase-note），
 * 提供：用户认证、笔记 CRUD、文件上传、基于 Spring AI 的笔记 AI 能力。</p>
 */
@SpringBootApplication
public class EdusmartServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EdusmartServerApplication.class, args);
    }
}
