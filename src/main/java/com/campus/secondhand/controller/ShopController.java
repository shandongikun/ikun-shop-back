package com.campus.secondhand.controller;

import com.campus.secondhand.entity.Goods;
import com.campus.secondhand.mapper.GoodsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ShopController {

    private static final Logger log = LoggerFactory.getLogger(ShopController.class);

    private final GoodsMapper goodsMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    public ShopController(GoodsMapper goodsMapper) {
        this.goodsMapper = goodsMapper;
    }

    // 查询所有商品
    @GetMapping("/goods/list")
    public ResponseEntity<?> getGoodsList() {
        try {
            List<Goods> goodsList = goodsMapper.listAllGoods();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", goodsList
            ));
        } catch (Exception e) {
            log.error("查询商品列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "系统错误：" + e.getMessage()));
        }
    }

    // 上传商品（带图片）
    @PostMapping("/goods/upload")
    public ResponseEntity<?> uploadGoodsWithImage(
            @RequestParam("goodid") String goodid,
            @RequestParam("username") String username,
            @RequestParam("goodname") String goodname,
            @RequestParam("goodprice") BigDecimal goodprice,
            @RequestParam("category") String category,
            @RequestParam("details") String details,
            @RequestParam("image") MultipartFile image
    ) {
        try {
            // 检查商品ID是否存在
            Goods existingGoods = goodsMapper.getGoodsById(goodid);
            if (existingGoods != null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "商品ID已存在"));
            }

            // 构建上传目录
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("创建上传目录: {}", uploadPath);
            }

            // 生成唯一文件名
            String originalFilename = Objects.requireNonNull(image.getOriginalFilename());
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID() + fileExtension;

            // 保存图片
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, image.getBytes());
            log.info("图片已保存: {}", filePath);

            // 构建商品对象 - 使用相对路径，与静态资源映射一致
            Goods goods = new Goods();
            goods.setGoodid(goodid);
            goods.setUsername(username);
            goods.setGoodname(goodname);
            goods.setGoodprice(goodprice);
            goods.setCategory(category);
            goods.setDetails(details);
            goods.setIng(false);


            goods.setImg("/uploads/img/" + fileName);

            // 保存到数据库
            goodsMapper.insertGoods(goods);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "商品上传成功",
                    "data", goods
            ));
        } catch (IOException e) {
            log.error("图片上传失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "图片上传失败：" + e.getMessage()));
        } catch (Exception e) {
            log.error("系统错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "系统错误：" + e.getMessage()));
        }
    }




    // 查询用户商品列表
    @GetMapping("/goods/user/{username}")
    public ResponseEntity<?> getUserGoods(@PathVariable String username) {
        try {
            List<Goods> userGoodsList = goodsMapper.listGoodsByUsername(username);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", userGoodsList // 直接返回列表，前端用 "goods" 接收
            ));
        } catch (Exception e) {
            log.error("查询用户商品列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "系统错误：" + e.getMessage()));
        }
    }


    @PostMapping("/goods/update")
    public ResponseEntity<?> updateGoods(
            @RequestParam("goodid") String goodid,
            @RequestParam(required = false) String goodname,
            @RequestParam(required = false) BigDecimal goodprice,
            @RequestParam(required = false) String category
    ) {
        try {
            Goods existingGoods = goodsMapper.getGoodsById(goodid);
            if (existingGoods == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "商品不存在"));
            }

            // 仅更新文本字段
            if (goodname != null) existingGoods.setGoodname(goodname);
            if (goodprice != null) existingGoods.setGoodprice(goodprice);
            if (category != null) existingGoods.setCategory(category);

            int result = goodsMapper.updateGoods(existingGoods);
            if (result > 0) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "商品信息更新成功",
                        "data", existingGoods
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "更新失败"));
            }

        } catch (Exception e) {
            log.error("更新商品失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "系统错误：" + e.getMessage()));
        }
    }

    // 删除商品接口
    @DeleteMapping("/goods/{goodid}")
    public ResponseEntity<?> deleteGood(@PathVariable String goodid) {
        try {
            Goods goods = goodsMapper.getGoodsById(goodid);
            if (goods == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "商品不存在"));
            }

            int result = goodsMapper.deleteGoodsById(goodid);
            if (result > 0) {
                // 删除服务器上的图片文件
                deleteImageFromServer(goods.getImg());

                return ResponseEntity.ok(Map.of("success", true, "message", "商品删除成功"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "删除失败"));
            }
        } catch (Exception e) {
            log.error("删除商品失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "系统错误：" + e.getMessage()));
        }
    }

    // 添加删除图片的辅助方法
    private void deleteImageFromServer(String imgPath) {
        if (imgPath != null && !imgPath.isEmpty()) {
            try {
                // 构建完整的文件路径（移除URL前缀，拼接本地存储路径）
                String filePath = uploadDir + imgPath.replace("/uploads/img/", "");
                Path path = Paths.get(filePath);

                // 检查文件是否存在并删除
                if (Files.exists(path)) {
                    Files.delete(path);
                    log.info("图片已删除: {}", filePath);
                }
            } catch (IOException e) {
                log.error("删除图片失败: {}", e.getMessage());
                // 可以选择抛出异常或记录警告，但不影响商品删除的主流程
            }
        }
    }


    // 根据ID查询商品详情
    @GetMapping("/goods/{goodid}")
    public ResponseEntity<?> getGoodsDetail(@PathVariable String goodid) {
        try {
            Goods goods = goodsMapper.getGoodsById(goodid);
            if (goods == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "goods", goods
            ));
        } catch (Exception e) {
            log.error("查询商品详情失败：{}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "系统错误：" + e.getMessage()));
        }
    }

    // 下单接口：接收买家用户名，更新商品状态
    @PostMapping("/order/place")
    public ResponseEntity<?> placeOrder(@RequestBody Map<String, String> orderData) {
        try {
            String goodid = orderData.get("goodid");
            String buyername = orderData.get("buyername");

            // 校验参数
            if (goodid == null || buyername == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "缺少必要参数（goodid或buyername）"
                ));
            }

            // 查询商品
            Goods goods = goodsMapper.getGoodsById(goodid);
            if (goods == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "商品不存在"
                ));
            }

            // 检查商品是否已被售出（ing=1）
            if (goods.getIng() != null && goods.getIng()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "商品已被售出"
                ));
            }

            // 更新商品的buyername和ing状态（ing暂不更新，待商家确认）
            goods.setBuyername(buyername);
            // ing=0 表示未售出，下单后仍为0，商家确认后改为1
            goodsMapper.updateGoods(goods);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "下单成功，等待商家确认"
            ));
        } catch (Exception e) {
            log.error("下单失败：{}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "系统错误：" + e.getMessage()
            ));
        }
    }




    // 商家确认出售接口：更新ing=1
    @PostMapping("/goods/confirm-sale/{goodid}")
    public ResponseEntity<?> confirmSale(@PathVariable String goodid) {
        try {
            Goods goods = goodsMapper.getGoodsById(goodid);
            if (goods == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "商品不存在"
                ));
            }

            // 更新ing为1，表示已售出
            goods.setIng(true);

            int result = goodsMapper.updateGoods(goods);

            if (result > 0) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "出售确认成功"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                        "success", false,
                        "message", "更新状态失败"
                ));
            }
        } catch (Exception e) {
            log.error("确认出售失败：{}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "系统错误：" + e.getMessage()
            ));
        }
    }


    // 获取卖家已出售的商品（ing=1 且 username=卖家用户名）
    @GetMapping("/goods/sold/seller/{username}")
    public ResponseEntity<?> getSoldGoodsBySeller(@PathVariable String username) {
        try {
            List<Goods> goodsList = goodsMapper.listSoldGoodsBySeller(username);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", goodsList
            ));
        } catch (Exception e) {
            log.error("查询卖家已出售商品失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "系统错误：" + e.getMessage()));
        }
    }

    // 获取买家已买入的商品（ing=1 且 buyername=买家用户名）
    @GetMapping("/goods/sold/buyer/{username}")
    public ResponseEntity<?> getSoldGoodsByBuyer(@PathVariable String username) {
        try {
            List<Goods> goodsList = goodsMapper.listSoldGoodsByBuyer(username);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", goodsList
            ));
        } catch (Exception e) {
            log.error("查询买家已买入商品失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "系统错误：" + e.getMessage()));
        }
    }



}