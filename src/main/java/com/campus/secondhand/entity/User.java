package com.campus.secondhand.entity;

import lombok.Data;

//@Data注解：自动生成getter/setter方法/toString方法
@Data
public class User {
    private Integer id;
    private String username;
    private String password;
    private String gender;
    private String phone;
    private String email;
    private String address;

}