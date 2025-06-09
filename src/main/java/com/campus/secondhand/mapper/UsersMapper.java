package com.campus.secondhand.mapper;

import com.campus.secondhand.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UsersMapper {
    // 查询所有用户
    @Select("select * from users")
    List<User> queryAllUsers();

    // 通过用户名查询用户（用于登录验证，包含密码）
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsernameWithPassword(String username);

    // 通过用户名查询用户（不包含密码）
    @Select("SELECT id, username, gender, phone, email, address FROM users WHERE username = #{username}")
    User findByUsername(String username);

    // 注册添加用户
    @Insert("INSERT INTO users (username, password, gender, phone, email, address) " +
            "VALUES (#{username}, #{password}, #{gender}, #{phone}, #{email}, #{address})")
    void insertUser(User user);

    // 更新用户信息（排除密码）
    @Update("UPDATE users SET " +
            "gender = #{gender}, " +
            "phone = #{phone}, " +
            "email = #{email}, " +
            "address = #{address} " +
            "WHERE username = #{username}")
    int updateUserInfo(
            @Param("username") String username,
            @Param("gender") String gender,
            @Param("phone") String phone,
            @Param("email") String email,
            @Param("address") String address
    );

    // 修改密码（需要原密码验证）
    @Update("UPDATE users SET password = #{newPassword} WHERE username = #{username} AND password = #{oldPassword}")
    int changePassword(
            @Param("username") String username,
            @Param("oldPassword") String oldPassword,
            @Param("newPassword") String newPassword
    );

    // 重置密码（用于找回密码，不需要原密码验证）
    @Update("UPDATE users SET password = #{newPassword} WHERE username = #{username}")
    int resetPassword(
            @Param("username") String username,
            @Param("newPassword") String newPassword
    );
}