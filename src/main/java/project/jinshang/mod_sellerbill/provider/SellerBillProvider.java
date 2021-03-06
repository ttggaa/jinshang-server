package project.jinshang.mod_sellerbill.provider;


import project.jinshang.common.utils.StringUtils;
import project.jinshang.mod_sellerbill.bean.SellerBillQuery;

public class SellerBillProvider {


    public String getSellerBill(SellerBillQuery query){
        StringBuilder sql = new StringBuilder();

       sql.append("select id,no,state,money,adduser,addtime,sellerusername,sellercompanyname,ordernum,totalbrokepay,totalorders,type,breaknum,totalbreak from sellerbill where id in (");
       sql.append("select sb.id from sellerbill sb,sellerbillorder sbo where sb.id=sbo.sellerbillid");

       if(query.getSellerid() != null && query.getSellerid()>0){
           sql.append(" and sb.sellerid=#{sellerid} ");
       }

       if(StringUtils.hasText(query.getOrderno())){
           sql.append(" and sbo.orderno like #{orderno} ");
       }

       if(StringUtils.hasText(query.getSellercompanyname())){
           sql.append(" and sellercompanyname like #{sellercompanyname} ");
       }

        if(query.getType()!=null && query.getType()>0){
            sql.append(" and type = #{type} ");
        }


       sql.append(") order by id desc");
       return  sql.toString();
    }


    public String getSellerBill1(SellerBillQuery query){
        StringBuilder sql = new StringBuilder();

        sql.append("select id,no,state,money,adduser,addtime,sellerusername,sellercompanyname,ordernum,totalbrokepay,totalorders,type,breaknum,totalbreak from sellerbill where 1=1 ");


        if(StringUtils.hasText(query.getSellercompanyname())){
            sql.append(" and sellercompanyname like #{sellercompanyname} ");
        }

        if(query.getType()!=null && query.getType()>0){
            sql.append(" and type = #{type} ");
        }


        sql.append(" order by id desc");
        return  sql.toString();
    }

}
