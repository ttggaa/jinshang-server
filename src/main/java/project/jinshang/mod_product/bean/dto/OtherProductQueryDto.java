package project.jinshang.mod_product.bean.dto;

import java.util.Date;

/**
 * create : wyh
 * date : 2018/1/4
 */
public class OtherProductQueryDto {

    private  Long memberid;

    private  String username;

    private  String brand;

    private  Long levelid;

    private  Long level1id;

    private  Long level2id;

    private  Long level3id;

    private  Short pdstate;

    private Date uptimeStart;

    private  Date uptimeEnd;

    private  Date createStart;

    private  Date createEnd;

    private String productname;


    private  String shopname;




    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Long getMemberid() {
        return memberid;
    }

    public void setMemberid(Long memberid) {
        this.memberid = memberid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getLevelid() {
        return levelid;
    }

    public void setLevelid(Long levelid) {
        this.levelid = levelid;
    }

    public Long getLevel1id() {
        return level1id;
    }

    public void setLevel1id(Long level1id) {
        this.level1id = level1id;
    }

    public Long getLevel2id() {
        return level2id;
    }

    public void setLevel2id(Long level2id) {
        this.level2id = level2id;
    }

    public Long getLevel3id() {
        return level3id;
    }

    public void setLevel3id(Long level3id) {
        this.level3id = level3id;
    }

    public Short getPdstate() {
        return pdstate;
    }

    public void setPdstate(Short pdstate) {
        this.pdstate = pdstate;
    }

    public Date getUptimeStart() {
        return uptimeStart;
    }

    public void setUptimeStart(Date uptimeStart) {
        this.uptimeStart = uptimeStart;
    }

    public Date getUptimeEnd() {
        return uptimeEnd;
    }

    public void setUptimeEnd(Date uptimeEnd) {
        this.uptimeEnd = uptimeEnd;
    }

    public Date getCreateStart() {
        return createStart;
    }

    public void setCreateStart(Date createStart) {
        this.createStart = createStart;
    }

    public Date getCreateEnd() {
        return createEnd;
    }

    public void setCreateEnd(Date createEnd) {
        this.createEnd = createEnd;
    }


    public String getProductname() {
        return productname;
    }

    public void setProductname(String productname) {
        this.productname = productname;
    }


    public String getShopname() {
        return shopname;
    }

    public void setShopname(String shopname) {
        this.shopname = shopname;
    }
}
