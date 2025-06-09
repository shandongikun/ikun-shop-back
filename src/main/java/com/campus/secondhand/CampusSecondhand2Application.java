package com.campus.secondhand;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 启动类
@SpringBootApplication
// 扫描mapper包
@MapperScan("com.campus.secondhand.mapper")
public class CampusSecondhand2Application {

    public static void main(String[] args) {
        SpringApplication.run(CampusSecondhand2Application.class, args);
    }

}
