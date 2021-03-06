package project.jinshang.mod_pay.mod_bankpay;

import com.abc.pay.client.JSON;
import com.abc.pay.client.TrxException;
import com.abc.pay.client.ebus.PaymentRequest;
import com.abc.pay.client.ebus.PaymentResult;
import com.abc.pay.client.ebus.QueryOrderRequest;
import com.abc.pay.client.ebus.QueryTrnxRecords;
import io.swagger.annotations.*;
import mizuki.project.core.restserver.config.BasicRet;
import mizuki.project.core.restserver.config.WebConfBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import project.jinshang.common.constant.Quantity;
import project.jinshang.common.exception.CashException;
import project.jinshang.common.exception.MyException;
import project.jinshang.common.utils.CommonUtils;
import project.jinshang.common.utils.GenerateNo;
import project.jinshang.common.utils.StringUtils;
import project.jinshang.mod_pay.bean.PayLogs;
import project.jinshang.mod_pay.bean.Trade;
import project.jinshang.mod_pay.bean.ret.PayUrlRet;
import project.jinshang.mod_pay.service.PayLogsService;
import project.jinshang.mod_pay.service.TradeService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/rest/bankpay")
@Api(tags = "第三方支付")
@Transactional(rollbackFor = Exception.class)
public class BankPayAction {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private WebConfBean webConfBean;
    @Autowired
    private TradeService tradeService;
    private SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyy/MM/dd");
    private SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("HH:mm:ss");

    @RequestMapping(value="/toPay",method= {RequestMethod.POST})
    @ApiOperation(value = "银联支付。去支付. 返回的url")
    @ApiImplicitParams({
            @ApiImplicitParam(value = "类型 1=订单，2=买家余额充值，3=卖家余额充值",name = "type",paramType = "query")
    })
    public PayUrlRet toPay(
            Model model,
            @ApiParam(required = true,value = "订单id")
            @RequestParam String orders,
            Short type,
            @ApiParam(value = "是否对公")
            @RequestParam(required = false,defaultValue = "false") boolean isCompany
    ) throws CashException {
        PayUrlRet ret = new PayUrlRet();
        Trade trade = null;
        // todo trade在生成时需要赋值 订单时间参数
        if(type== Quantity.STATE_1){ //订单
            String uuid = "order-"+GenerateNo.getOrderIdByUUId();
            trade = tradeService.buildFromOrderId(orders,Quantity.STATE_2,uuid);
        }else if (type == Quantity.STATE_2 ){
            trade = tradeService.buildFromBuyerRecharge(orders,Quantity.STATE_3);
        }else if(type == Quantity.STATE_3){
            trade = tradeService.buildFromSellerRecharge(orders,Quantity.STATE_4);
        }
        // todo test
//        trade = new Trade().setOutTradeNo(String.valueOf(System.currentTimeMillis()))
//                .setAmount(1).setSubject("test").setDatetime(System.currentTimeMillis());
//        logger.info(trade.getOutTradeNo()+" "+trade.getDatetime());

        if(trade==null){
            ret.setResult(BasicRet.ERR).setMessage("订单状态错误");
            return ret;
        }
        if(trade.getPayUrlRet()!=null && trade.getPayUrlRet().getResult()==BasicRet.ERR){
            return trade.getPayUrlRet();
        }

        //1、生成订单对象
        PaymentRequest tPaymentRequest = new PaymentRequest();
        Date date = new Date(trade.getDatetime());
        String ymd = simpleDateFormat1.format(date);
        String hms = simpleDateFormat2.format(date);
        tPaymentRequest.dicOrder.put("PayTypeID", "ImmediatePay");      //设定交易类型
        tPaymentRequest.dicOrder.put("OrderDate", ymd);                  //设定订单日期 （必要信息 - YYYY/MM/DD）
        tPaymentRequest.dicOrder.put("OrderTime", hms);                   //设定订单时间 （必要信息 - HH:MM:SS）
        tPaymentRequest.dicOrder.put("OrderNo", trade.getOutTradeNo());   //设定订单编号 （必要信息）
        tPaymentRequest.dicOrder.put("CurrencyCode", "156");             //设定交易币种
        tPaymentRequest.dicOrder.put("OrderAmount", new BigDecimal(trade.getAmount()).divide(new BigDecimal(100),2,BigDecimal.ROUND_HALF_UP).toString());      //设定交易金额
        tPaymentRequest.dicOrder.put("InstallmentMark", "0");       //分期标识
        String commodityType="0202";
        tPaymentRequest.dicOrder.put("CommodityType", commodityType);           //设置商品种类

        //2、订单明细
        LinkedHashMap orderitem = new LinkedHashMap();
        orderitem.put("ProductName", trade.getSubject());//商品名称
        tPaymentRequest.orderitems.put(1, orderitem);

        //3、生成支付请求对象
        String paymentType = "6";
        if(isCompany) paymentType="7";
        tPaymentRequest.dicRequest.put("PaymentType", paymentType);        //设定支付类型 6-银联跨行，7-对公
        tPaymentRequest.dicRequest.put("PaymentLinkType", "1");    // 设定支付接入方式.
//        if (paymentType.equals(Constants.PAY_TYPE_UCBP) && paymentLinkType.equals(Constants.PAY_LINK_TYPE_MOBILE))
//        {
//            tPaymentRequest.dicRequest.put("UnionPayLinkType","0");  //当支付类型为6，支付接入方式为2的条件满足时，需要设置银联跨行移动支付接入方式
//        }
        tPaymentRequest.dicRequest.put("NotifyType", "1");              //设定通知方式
        tPaymentRequest.dicRequest.put("ResultNotifyURL", webConfBean.getProjectDomain()+"/rest/bankpay/notify");    //设定通知URL地址
        tPaymentRequest.dicRequest.put("IsBreakAccount", "0");      //设定交易是否分账

        JSON json = tPaymentRequest.postRequest();
        //JSON json = tPaymentRequest.extendPostRequest(1);

        String ReturnCode = json.GetKeyValue("ReturnCode");
        String ErrorMessage = json.GetKeyValue("ErrorMessage");

        //返回页面
        if ("0000".equals(ReturnCode)) {
            ret.getData().setUrl(json.GetKeyValue("PaymentURL"));
            ret.setResult(BasicRet.SUCCESS);
        }else{
            ret.setResult(BasicRet.ERR).setMessage(ErrorMessage);
            logger.error("abc return error: "+ReturnCode+" "+ErrorMessage);
        }
        return ret;
    }


    @Resource
    private PayLogsService payLogsService;

    @RequestMapping(value="/notify",method= {RequestMethod.POST, RequestMethod.GET})
    public String notify(HttpServletRequest request) throws TrxException, CashException, MyException {
        logger.info("银联支付回调数据："+CommonUtils.getRequestParamStr(request));

        try {
            String msg = request.getParameter("MSG");
            PaymentResult tResult = new PaymentResult(msg);
            //2、判断支付结果处理状态，进行后续操作
            // 展示给用户的结果url todo ??
            String tMerchantPage = webConfBean.getProjectDomainMain();
            logger.error("abc: "+tResult.isSuccess());

//        if (tResult.isSuccess()) {
//            //3、支付成功并且验签、解析成功
//            tMerchantPage = "http://120.27.226.95";
//        }
//        else {
//            //4、支付成功但是由于验签或者解析报文等操作失败
////            tMerchantPage = "http://10.233.4.11:8080/ebusclient/ResultFail.jsp";
//        }


            String orderNo = tResult.getValue("OrderNo");
            String[] orderNo_array = orderNo.split("-");

            String total_amount = tResult.getValue("OrderAmount");
            //if(StringUtils.hasText(total_amount) && StringUtils.isNumeric(total_amount)){
                PayLogs payLogs = new PayLogs();
                payLogs.setTransactionid("");
                payLogs.setOuttradeno(orderNo);
                payLogs.setMoney(new BigDecimal(total_amount == null ? "0" : total_amount).setScale(2,BigDecimal.ROUND_HALF_UP));
                payLogs.setCreatetime(new Date());
                payLogs.setChannel("bank");
                payLogsService.insertSelective(payLogs);
           // }


            boolean res = false;
            if(orderNo_array.length==2){
                if(orderNo_array[0].equals("order")){
                    res = tradeService.notify(orderNo, "bank","");
                }else if(orderNo_array[0].equals("buy")) {
                    res = tradeService.notifyBuyerRecharge(orderNo_array[1],"",Quantity.STATE_2);
                }else if(orderNo_array[0].equals("sell")){
                    res = tradeService.notifySellerRecharge(orderNo_array[1],"");
                }
            }
            return "<URL>"+tMerchantPage+"</URL>\n" +
                    "\n" +
                    "<HTML>\n" +
                    "<HEAD>\n" +
                    "<meta http-equiv=\"refresh\" content=\"0; url='"+tMerchantPage+"'\">\n" +
                    "</HEAD>\n" +
                    "</HTML>";
        } catch (Exception e) {
           logger.info("银联支付回调错误："+e.getMessage());
           logger.error("银联支付回调错误："+e.getMessage());
           throw new MyException("银联支付回调错误",e);
        }
    }

    /**
     * 交易流水查询
     * @param payTypeID
     * @param orderNo
     * @return
     * @throws TrxException
     * @throws CashException
     * @throws MyException
     */
    @RequestMapping(value="/QueryTrnxRecords1",method= {RequestMethod.POST, RequestMethod.GET})
    public String MerchantQueryOrder1(String payTypeID,String orderNo) throws TrxException, CashException, MyException {
        QueryTrnxRecords tRequest = new QueryTrnxRecords();
        tRequest.dicRequest.put("SettleDate","2018/11/09");  //查询日期YYYY/MM/DD （必要信息）
        tRequest.dicRequest.put("SettleStartHour","0");  //查询开始时间段（0-23）
        tRequest.dicRequest.put("SettleEndHour","23");  //查询截止时间段（0-23）
        tRequest.dicRequest.put("ZIP","0");

//3、传送交易流水查询请求并取得交易流水
        JSON json = tRequest.postRequest();

//4、判断交易流水查询结果状态，进行后续操作
        String ReturnCode = json.GetKeyValue("ReturnCode");
        String ErrorMessage = json.GetKeyValue("ErrorMessage");
        StringBuilder sb = new StringBuilder();
        if (ReturnCode.equals("0000"))
        {
            //5、交易流水查询成功，生成交易流水对象
            sb.append("ReturnCode      = [" + json.GetKeyValue("ReturnCode") + "]\r\n");
            sb.append("ErrorMessage      = [" + json.GetKeyValue("ErrorMessage") + "]\r\n");
            sb.append("TrxType      = [" + json.GetKeyValue("TrxType") + "]\r\n");
            sb.append("DetailRecords      = [" + json.GetKeyValue("DetailRecords") + "]\r\n");
        }else {
            //6、交易流水查询失败
            System.out.println("ReturnCode   = [" + ReturnCode + "]<br>");
            System.out.println("ErrorMessage = [" + ErrorMessage + "]<br>");
        }

        return sb.toString();
    }


    //交易查询
    @RequestMapping(value="/MerchantQueryOrder",method= {RequestMethod.POST, RequestMethod.GET})
    public String MerchantQueryOrder(String payTypeID,String orderNo) throws TrxException, CashException, MyException {
        //1、生成交易查询对象
        String queryTpye = "0";
        if(queryTpye.equals("0")){
            queryTpye = "false";
        }else if (queryTpye.equals("1")){
            queryTpye="true";
        }
        StringBuilder sb = new StringBuilder();
        QueryOrderRequest tQueryRequest = new QueryOrderRequest();
        tQueryRequest.queryRequest.put("PayTypeID", payTypeID);    //设定交易类型
        tQueryRequest.queryRequest.put("OrderNo",orderNo );    //设定订单编号 （必要信息）
        tQueryRequest.queryRequest.put("QueryDetail",true );//设定查询方式
        JSON json = tQueryRequest.postRequest();
        String ReturnCode = json.GetKeyValue("ReturnCode");
        String ErrorMessage = json.GetKeyValue("ErrorMessage");
        if (ReturnCode.equals("0000"))
        {
            System.out.println("ReturnCode   = [" + ReturnCode + "]<br/>");
            System.out.println("ErrorMessage = [" + ErrorMessage + "]<br/>");
            //4、获取结果信息
            String orderInfo = json.GetKeyValue("Order");
            if (orderInfo.length() < 1)
            {
                System.out.println("查询结果为空<br/>");
            } else{
                //1、还原经过base64编码的信息
                //Base64 tBase64 = new Base64();
                String orderDetail = new String(orderInfo);
                json.setJsonString(orderDetail);
               // sb.append("订单明细" + orderDetail + "<br/>");

                //if(queryTpye.equals("0")) {
                    sb.append("PayTypeID      = [" + json.GetKeyValue("PayTypeID") + "]<br/>");
                    sb.append("OrderNo      = [" + json.GetKeyValue("OrderNo") + "]<br/>");
                    sb.append("OrderDate      = [" + json.GetKeyValue("OrderDate") + "]<br/>");
                    sb.append("OrderTime      = [" + json.GetKeyValue("OrderTime") + "]<br/>");
                    sb.append("OrderAmount      = [" + json.GetKeyValue("OrderAmount") + "]<br/>");
                    sb.append("Status      = [" + json.GetKeyValue("Status") + "]<br/>");
              //  }
            }
        }
        else
        {
            //6、商户订单查询失败
            sb.append("ReturnCode   = [" + ReturnCode + "]<br/>");
            sb.append("ErrorMessage = [" + ErrorMessage + "]<br/>");
        }

        return sb.toString();
    }
}
