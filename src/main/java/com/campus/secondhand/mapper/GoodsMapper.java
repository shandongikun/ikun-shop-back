package com.campus.secondhand.mapper;

import com.campus.secondhand.entity.Goods;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface GoodsMapper {

    /**
     * 插入商品（包含 ing 字段）
     */
    @Insert("INSERT INTO goods " +
            "(goodid, username, goodname, goodprice, category, img, details, ing, buyername) " +
            "VALUES " +
            "(#{goodid}, #{username}, #{goodname}, #{goodprice}, #{category}, #{img}, #{details}, #{ing}, #{buyername})")
    void insertGoods(Goods goods);

    /**
     * 根据ID查询商品
     */
    @Select("SELECT * FROM goods WHERE goodid = #{goodid}")
    Goods getGoodsById(String goodid);

    /**
     * 查询所有未售出的商品（ing != 1）
     */
    @Select("SELECT * FROM goods WHERE ing != 1")
    List<Goods> listAllGoods();

    /**
     * 根据用户名查询未售出的商品（ing != 1）
     */

    @Select("SELECT * FROM goods WHERE username = #{username} AND ing != 1")
    List<Goods> listGoodsByUsername(String username);

    /**
     * 更新商品（包含 ing 和 buyername 字段）
     */
    @Update("UPDATE goods " +
            "SET goodname = #{goodname}, " +
            "goodprice = #{goodprice}, " +
            "category = #{category}, " +
            "ing = #{ing}, " +
            "buyername = #{buyername} " +
            "WHERE goodid = #{goodid}")
    int updateGoods(Goods goods);

    /**
     * 删除商品
     */
    @Delete("DELETE FROM goods WHERE goodid = #{goodid}")
    int deleteGoodsById(String goodid);


    // 查询卖家已出售的商品（ing=1 且 username=卖家用户名）
    @Select("SELECT * FROM goods WHERE ing = 1 AND username = #{username}")
    List<Goods> listSoldGoodsBySeller(String username);

    // 查询买家已买入的商品（ing=1 且 buyername=买家用户名）
    @Select("SELECT * FROM goods WHERE ing = 1 AND buyername = #{username}")
    List<Goods> listSoldGoodsByBuyer(String username);
}