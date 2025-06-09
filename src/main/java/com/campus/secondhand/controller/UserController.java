package com.campus.secondhand.controller;

import com.campus.secondhand.entity.User;
import com.campus.secondhand.mapper.UsersMapper;
import com.campus.secondhand.util.PasswordEncoderUtil;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:8081", allowCredentials = "true")
public class UserController {

    private final UsersMapper usersMapper;

    public UserController(UsersMapper usersMapper) {
        this.usersMapper = usersMapper;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> credentials) {
        Map<String, Object> result = new HashMap<>();

        try {
            String username = credentials.get("username");
            String rawPassword = credentials.get("password");

            User dbUser = usersMapper.findByUsernameWithPassword(username);

            if (dbUser != null && PasswordEncoderUtil.matchesPassword(rawPassword, dbUser.getPassword())) {
                // 登录成功
                result.put("success", true);
                result.put("message", "登录成功");

                // 返回用户信息
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", dbUser.getId());
                userInfo.put("username", dbUser.getUsername());
                userInfo.put("email", dbUser.getEmail());
                // 添加其他需要的用户信息

                result.put("userInfo", userInfo);
            } else {
                // 登录失败
                result.put("success", false);
                result.put("message", "用户名或密码错误");
                return result; // 添加返回语句
            }
        } catch (Exception e) {
            // 处理异常
            result.put("success", false);
            result.put("message", "登录过程中发生错误: " + e.getMessage());
        }

        return result;
    }

    @GetMapping("/username")
    public Map<String, Object> checkUsername(@RequestParam String username) {
        Map<String, Object> result = new HashMap<>();
        User user = usersMapper.findByUsername(username);
        result.put("success", true);
        result.put("message", "查询成功");
        result.put("data", Map.of("exists", user != null));
        return result;
    }

    //注册
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        try {
            User existingUser = usersMapper.findByUsername(user.getUsername());
            if (existingUser != null) {
                result.put("success", false);
                result.put("message", "用户名已存在");
                return result;
            }

            // 加密密码
            String encodedPassword = PasswordEncoderUtil.encodePassword(user.getPassword());
            user.setPassword(encodedPassword);

            usersMapper.insertUser(user);

            result.put("success", true);
            result.put("message", "注册成功");
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "注册失败: " + e.getMessage());
            return result;
        }
    }


    // 获取用户信息（不包含密码）
    @GetMapping("/user/info")
    public Map<String, Object> getUserInfo(@RequestParam String username) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = usersMapper.findByUsername(username);
            if (user == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }

            // 排除密码字段
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("gender", user.getGender());
            userInfo.put("phone", user.getPhone());
            userInfo.put("email", user.getEmail());
            userInfo.put("address", user.getAddress());

            result.put("success", true);
            result.put("userInfo", userInfo);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取用户信息失败: " + e.getMessage());
        }
        return result;
    }

    // 更新用户信息（不含密码）
    @PostMapping("/user/update")
    public Map<String, Object> updateUserInfo(@RequestBody Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        try {
            String username = (String) data.get("username");
            String gender = (String) data.get("gender");
            String phone = (String) data.get("phone");
            String email = (String) data.get("email");
            String address = (String) data.get("address");

            // 参数验证
            if (username == null || username.isEmpty()) {
                result.put("success", false);
                result.put("message", "用户名不能为空");
                return result;
            }

            int rows = usersMapper.updateUserInfo(
                    username,
                    gender,
                    phone,
                    email,
                    address
            );

            if (rows > 0) {
                result.put("success", true);
                result.put("message", "用户信息更新成功");
            } else {
                result.put("success", false);
                result.put("message", "用户不存在或更新失败");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "更新用户信息失败: " + e.getMessage());
        }
        return result;
    }

    // 修改密码
    @PostMapping("/user/changePassword")
    public Map<String, Object> changePassword(@RequestBody Map<String, String> data) {
        Map<String, Object> result = new HashMap<>();
        try {
            String username = data.get("username");
            String oldPassword = data.get("oldPassword");
            String newPassword = data.get("newPassword");

            // 参数验证
            if (username == null || username.isEmpty()) {
                result.put("success", false);
                result.put("message", "用户名不能为空");
                return result;
            }

            if (oldPassword == null || oldPassword.isEmpty()) {
                result.put("success", false);
                result.put("message", "原密码不能为空");
                return result;
            }

            if (newPassword == null || newPassword.isEmpty()) {
                result.put("success", false);
                result.put("message", "新密码不能为空");
                return result;
            }

            // 验证原密码是否正确
            User user = usersMapper.findByUsernameWithPassword(username);
            if (user == null) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }

            if (!PasswordEncoderUtil.matchesPassword(oldPassword, user.getPassword())) {
                result.put("success", false);
                result.put("message", "原密码不正确");
                return result;
            }

            // 加密新密码
            String encodedNewPassword = PasswordEncoderUtil.encodePassword(newPassword);

            // 更新密码
            int rows = usersMapper.changePassword(username, user.getPassword(), encodedNewPassword);

            if (rows > 0) {
                result.put("success", true);
                result.put("message", "密码修改成功");
            } else {
                result.put("success", false);
                result.put("message", "密码修改失败");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "修改密码失败: " + e.getMessage());
        }
        return result;
    }

    // 检查用户名和邮箱是否匹配（用于找回密码）
    @GetMapping("/user/checkUsernameAndEmail")
    public Map<String, Object> checkUsernameAndEmail(@RequestParam String username, @RequestParam String email) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = usersMapper.findByUsername(username);
            if (user != null && user.getEmail().equals(email)) {
                result.put("success", true);
                result.put("message", "用户名和邮箱匹配");
            } else {
                result.put("success", false);
                result.put("message", "用户名和邮箱不匹配");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "检查过程中发生错误: " + e.getMessage());
        }
        return result;
    }

    // 重置密码
    @PostMapping("/user/resetPassword")
    public Map<String, Object> resetPassword(@RequestBody Map<String, String> data) {
        Map<String, Object> result = new HashMap<>();
        try {
            String username = data.get("username");
            String newPassword = data.get("newPassword");

            // 参数验证
            if (username == null || username.isEmpty()) {
                result.put("success", false);
                result.put("message", "用户名不能为空");
                return result;
            }

            if (newPassword == null || newPassword.isEmpty()) {
                result.put("success", false);
                result.put("message", "新密码不能为空");
                return result;
            }

            // 加密新密码
            String encodedNewPassword = PasswordEncoderUtil.encodePassword(newPassword);

            int rows = usersMapper.resetPassword(username, encodedNewPassword);
            if (rows > 0) {
                result.put("success", true);
                result.put("message", "密码重置成功");
            } else {
                result.put("success", false);
                result.put("message", "密码重置失败");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "重置密码失败: " + e.getMessage());
        }
        return result;
    }
}