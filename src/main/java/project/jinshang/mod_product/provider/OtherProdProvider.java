package project.jinshang.mod_product.provider;

import org.apache.ibatis.annotations.Param;
import project.jinshang.common.utils.StringUtils;
import project.jinshang.mod_product.bean.dto.OtherProductQueryDto;

/**
 * create : wyh
 * date : 2018/1/4
 */
public class OtherProdProvider {

    public  String listOtherProd(@Param("queryDto")OtherProductQueryDto queryDto){
        StringBuilder sql = new StringBuilder();

        sql.append("select P.*,M.username,SC.companyname,SC.shopname from productinfo P,Member M,sellercompanyinfo SC where P.memberid=M.id and M.id=SC.memberid and  producttype='其他' and pdstate != 6 ");
        if(queryDto.getMemberid() != null && queryDto.getMemberid()>0){
            sql.append(" and P.memberid=#{queryDto.memberid} ");
        }


        if(StringUtils.hasText(queryDto.getShopname())){
            sql.append(" and SC.shopname like '%"+queryDto.getShopname()+"%' ");
        }



        if(StringUtils.hasText(queryDto.getUsername())){
            sql.append(" and M.username like '%"+queryDto.getUsername()+"%'");
        }

        if(StringUtils.hasText(queryDto.getBrand())){
            sql.append(" and P.brand like '%"+queryDto.getBrand()+"%' ");
        }

        if(queryDto.getLevel1id() != null){
            sql.append(" and P.level1id=#{queryDto.level1id} ");
        }

        if(queryDto.getLevel2id() != null){
            sql.append(" and P.level2id=#{queryDto.level2id} ");
        }

        if(queryDto.getLevel3id() != null){
            sql.append(" and P.level3id=#{queryDto.level3id} ");
        }

        if(queryDto.getPdstate() != null && queryDto.getPdstate() >=0){
            sql.append(" and P.pdstate=#{queryDto.pdstate} ");
        }

        if(queryDto.getUptimeStart() != null){
            sql.append(" and P.updatetime >= #{queryDto.uptimeStart} ");
        }
        if(queryDto.getUptimeEnd() != null){
            sql.append(" and P.updatetime < #{queryDto.uptimeEnd} ");
        }

        if(queryDto.getCreateStart() != null){
            sql.append(" and P.createtime >= #{queryDto.createStart} ");
        }

        if(queryDto.getCreateStart() != null){
            sql.append(" and P.createtime < #{queryDto.createEnd} ");
        }


        if(queryDto.getProductname() != null && !"".equals(queryDto.getProductname())){
            sql.append(" and P.productname like '%"+queryDto.getProductname()+"%' ");
        }

        return  sql.toString();
    }




    public  String listOtherProdForAdminExcel(@Param("queryDto")OtherProductQueryDto queryDto){
        StringBuilder sql = new StringBuilder();

        sql.append("select P.*,M.username,SC.companyname,SC.shopname from productinfo P,Member M,sellercompanyinfo SC where P.memberid=M.id and M.id=SC.memberid and  producttype='其他' and pdstate != 6 ");
        if(queryDto.getMemberid() != null && queryDto.getMemberid()>0){
            sql.append(" and P.memberid=#{queryDto.memberid} ");
        }

        if(StringUtils.hasText(queryDto.getShopname())){
            sql.append(" and SC.shopname like '"+queryDto.getShopname()+"' ");
        }


        if(StringUtils.hasText(queryDto.getUsername())){
            sql.append(" and M.username like '%"+queryDto.getUsername()+"%' ");
        }

        if(StringUtils.hasText(queryDto.getBrand())){
            sql.append(" and P.brand like '%"+queryDto.getBrand()+"%' ");
        }

        if(queryDto.getLevel1id() != null){
            sql.append(" and P.level1id=#{queryDto.level1id} ");
        }

        if(queryDto.getLevel2id() != null){
            sql.append(" and P.level2id=#{queryDto.level2id} ");
        }

        if(queryDto.getLevel3id() != null){
            sql.append(" and P.level3id=#{queryDto.level3id} ");
        }

        if(queryDto.getPdstate() != null && queryDto.getPdstate() >=0){
            sql.append(" and P.pdstate=#{queryDto.pdstate} ");
        }

        if(queryDto.getUptimeStart() != null){
            sql.append(" and P.updatetime >= #{queryDto.uptimeStart} ");
        }
        if(queryDto.getUptimeEnd() != null){
            sql.append(" and P.updatetime < #{queryDto.uptimeEnd} ");
        }

        if(queryDto.getCreateStart() != null){
            sql.append(" and P.createtime >= #{queryDto.createStart} ");
        }

        if(queryDto.getCreateStart() != null){
            sql.append(" and P.createtime < #{queryDto.createEnd} ");
        }

        if(queryDto.getProductname() != null && !"".equals(queryDto.getProductname())){
            sql.append(" and P.productname like '%"+queryDto.getProductname()+"%' ");
        }

        return  sql.toString();
    }





    public  String listOtherProdForSellerExcel(@Param("queryDto")OtherProductQueryDto queryDto){
        StringBuilder sql = new StringBuilder();

        sql.append("select P.*,M.username,SC.companyname,SC.shopname from productinfo P,Member M,sellercompanyinfo SC where P.memberid=M.id and M.id=SC.memberid and  producttype='其他' and pdstate != 6 ");
        if(queryDto.getMemberid() != null && queryDto.getMemberid()>0){
            sql.append(" and P.memberid=#{queryDto.memberid} ");
        }


        if(StringUtils.hasText(queryDto.getShopname())){
            sql.append(" and SC.shopname=#{queryDto.shopname} ");
        }



        if(StringUtils.hasText(queryDto.getUsername())){
            sql.append(" and M.username=#{queryDto.username} ");
        }

        if(StringUtils.hasText(queryDto.getBrand())){
            sql.append(" and P.brand = #{queryDto.brand} ");
        }

        if(queryDto.getLevel1id() != null){
            sql.append(" and P.level1id=#{queryDto.level1id} ");
        }

        if(queryDto.getLevel2id() != null){
            sql.append(" and P.level2id=#{queryDto.level2id} ");
        }

        if(queryDto.getLevel3id() != null){
            sql.append(" and P.level3id=#{queryDto.level3id} ");
        }

        if(queryDto.getPdstate() != null && queryDto.getPdstate() >=0){
            sql.append(" and P.pdstate=#{queryDto.pdstate} ");
        }

        if(queryDto.getUptimeStart() != null){
            sql.append(" and P.updatetime >= #{queryDto.uptimeStart} ");
        }
        if(queryDto.getUptimeEnd() != null){
            sql.append(" and P.updatetime < #{queryDto.uptimeEnd} ");
        }

        if(queryDto.getCreateStart() != null){
            sql.append(" and P.createtime >= #{queryDto.createStart} ");
        }

        if(queryDto.getCreateStart() != null){
            sql.append(" and P.createtime < #{queryDto.createEnd} ");
        }


        if(queryDto.getProductname() != null && !"".equals(queryDto.getProductname())){
            sql.append(" and P.productname like '%"+queryDto.getProductname()+"%' ");
        }

        return  sql.toString();
    }




}
