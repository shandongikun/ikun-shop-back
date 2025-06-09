package com.campus.secondhand.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class Goods {
    private String goodid;
    private String username;
    private String goodname;
    private BigDecimal goodprice;
    private String category;
    private String img;
    private String details;
    private Boolean ing;
    private String buyername;

}