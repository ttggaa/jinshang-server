package project.jinshang.mod_pay.service;

import com.github.binarywang.wxpay.service.WxPayService;
import mizuki.project.core.restserver.config.BasicRet;
import net.sf.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import project.jinshang.common.constant.Quantity;
import project.jinshang.common.constant.RefundConst;
import project.jinshang.common.exception.CashException;
import project.jinshang.common.exception.MyException;
import project.jinshang.common.utils.DateUtils;
import project.jinshang.common.utils.GenerateNo;
import project.jinshang.common.utils.StatementType;
import project.jinshang.common.utils.StringUtils;
import project.jinshang.mod_admin.mod_statement.bean.BuyerStatement;
import project.jinshang.mod_admin.mod_statement.service.StatementService;
import project.jinshang.mod_cash.SalerCapitalMapper;
import project.jinshang.mod_cash.bean.BuyerCapital;
import project.jinshang.mod_cash.bean.SalerCapital;
import project.jinshang.mod_cash.service.BuyerCapitalService;
import project.jinshang.mod_company.BuyerCompanyInfoMapper;
import project.jinshang.mod_company.bean.BuyerCompanyInfo;
import project.jinshang.mod_pay.bean.PayLogs;
import project.jinshang.mod_pay.bean.PayTradeLogs;
import project.jinshang.mod_pay.mod_alipay.AlipayService;
import project.jinshang.mod_pay.mod_bankpay.AbcService;
import project.jinshang.mod_pay.mod_wxpay.MyWxPayService;
import project.jinshang.mod_product.bean.*;
import project.jinshang.mod_product.service.OrdersService;
import project.jinshang.mod_wms_middleware.WMSService;
import project.jinshang.mod_member.MemberMapper;
import project.jinshang.mod_member.bean.Member;
import project.jinshang.mod_pay.bean.Refund;
import project.jinshang.mod_pay.bean.Trade;
import project.jinshang.mod_pay.bean.ret.PayUrlRet;
import project.jinshang.mod_product.OrderProductMapper;
import project.jinshang.mod_product.OrdersMapper;

import javax.persistence.criteria.Order;
import java.math.BigDecimal;
import java.util.*;

@Service
public class TradeService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderProductMapper orderProductMapper;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private SalerCapitalMapper salerCapitalMapper;


    @Autowired
    private OrdersService ordersService;

    @Autowired
    private PayTradeLogsService payTradeLogsService;

    @Autowired
    private PayLogsService payLogsService;

    @Autowired
    private TradeService tradeService;

    @Autowired
    private AlipayService alipayService;
    @Autowired
    private MyWxPayService wxPayService;
    @Autowired
    private AbcService abcService;
    @Autowired
    private BuyerCapitalService buyerCapitalService;
    @Autowired
    private StatementService statementService;
    @Autowired
    private BuyerCompanyInfoMapper buyerCompanyInfoMapper;

    /**
     * 买家充值
     *
     * @param orders 充值订单编号
     * @return
     */
    public Trade buildFromBuyerRecharge(String orders, Short rechargeperform) {

        PayUrlRet basicRet = new PayUrlRet();
        Trade trade = new Trade();

        String uuid = "buy" + "-" + orders;

        BuyerCapital buyerCapital = buyerCapitalService.getBuyerCapitalByRechargenumber(orders);
        if (buyerCapital != null && buyerCapital.getRechargeperform() == Quantity.STATE_) {
            BuyerCapital updateCapital = new BuyerCapital();
            updateCapital.setId(buyerCapital.getId());
            updateCapital.setRechargeperform(rechargeperform);
            buyerCapitalService.updateByPrimaryKeySelective(updateCapital);

            trade.setAmount((buyerCapital.getCapital().multiply(new BigDecimal(100))).longValue());
            trade.setOutTradeNo(uuid);
            trade.setSubject("买家充值支付");
            trade.setBody("买家充值支付");
            trade.setDatetime(buyerCapital.getTradetime().getTime());
            basicRet.setResult(BasicRet.SUCCESS);
            trade.setPayUrlRet(basicRet);
        } else {
            basicRet.setResult(BasicRet.ERR);
            basicRet.setMessage("该充值信息有问题，请重新提交");
            trade.setPayUrlRet(basicRet);
        }
        return trade;
    }


    /**
     * 卖家充值
     *
     * @param orders 充值订单编号
     * @return
     */
    public Trade buildFromSellerRecharge(String orders, Short rechargeperform) {

        PayUrlRet basicRet = new PayUrlRet();
        Trade trade = new Trade();

        String uuid = "sell" + "-" + orders;

        SalerCapital salerCapital = salerCapitalMapper.getSalerCapitalByRechargenumber(orders);

        if (salerCapital != null && salerCapital.getRechargeperform() == Quantity.STATE_) {
            SalerCapital updateCapital = new SalerCapital();
            updateCapital.setId(salerCapital.getId());
            updateCapital.setRechargeperform(rechargeperform);
            salerCapitalMapper.updateByPrimaryKeySelective(updateCapital);

            trade.setAmount((salerCapital.getOrdercapital().multiply(new BigDecimal(100))).longValue());
            trade.setOutTradeNo(uuid);
            trade.setSubject("买家充值支付");
            trade.setBody("买家充值支付");
            trade.setDatetime(salerCapital.getTradetime().getTime());
            basicRet.setResult(BasicRet.SUCCESS);
            trade.setPayUrlRet(basicRet);

        } else {
            basicRet.setResult(BasicRet.ERR);
            basicRet.setMessage("该充值信息有问题，请重新提交");
            trade.setPayUrlRet(basicRet);
        }


        return trade;
    }





//    public Trade buildFromOrderId(String orders, Short paytype,String uuid) throws CashException {
//        PayUrlRet basicRet = new PayUrlRet();
//        Trade trade = new Trade();
//
//        List<Orders> ordersList = ordersMapper.getOrdersByInIds(orders);
//        if(ordersList.size()==0){
//            throw new CashException("订单不存在，请联系网站管理员");
//        }
//
//
//        List<OrderProduct> orderProducts = orderProductMapper.getOrderProductByInOrderids(orders);
//        if(orderProducts.size()==0){
//            throw  new CashException("订单商品部存在，请联系网站管理员");
//        }
//
//        Long ordertime = System.currentTimeMillis();
//
//        BigDecimal sumpay = new BigDecimal(0);
//        boolean flag = false;
//        String pdname = "紧商订单商品信息";
//
//        //判断订单状态
//        //订单类型0=立即发货1=远期全款2=远期定金3=远期余款
//        //订单状态0=待付款1=待发货3=待收货4=待验货5=已完成7=已关闭8=备货中9=备货完成
//        for(Orders od : ordersList){
//            if(od.getOrdertype() == Quantity.STATE_0 || od.getOrdertype() == Quantity.STATE_1 || od.getOrdertype() == Quantity.STATE_2){
//                if(od.getOrderstatus() != Quantity.STATE_0){
//                    throw   new CashException(od.getOrderno()+"订单状态不正确，不可付款");
//                }
//            }else if(od.getOrdertype() == Quantity.STATE_3){
//                if(od.getOrderstatus() != Quantity.STATE_9){
//                    throw new CashException(od.getOrderno()+"订单状态不正确，不可付款");
//                }
//            }else{
//                throw new CashException(od.getOrderno()+"订单状态不正确，不可付款");
//            }
//        }
//
//        List<PayTradeLogs> payTradeLogsList = new ArrayList<>();
//
//        for (Orders order : ordersList) {
//            PayTradeLogs logs = new PayTradeLogs();
//            logs.setOrderid(order.getId());
//            logs.setOrderno(order.getOrderno());
//            logs.setOuttradeno(uuid);
//            logs.setCreatetime(new Date());
//            logs.setPaytype(paytype);
//            logs.setPaystates(Quantity.STATE_0);
//
//
//            Orders updateOrder = new Orders();
//            updateOrder.setId(order.getId());
//            //定金和余款必须是同一种支付方式
//            if (order.getOrdertype() == Quantity.STATE_3) {
//                if (order.getPaytype() != paytype) {
//                    //basicRet.setResult(BasicRet.ERR);
//                    //basicRet.setMessage("定金和余款不是同一种支付方式");
//                    //trade.setPayUrlRet(basicRet);
//                    //return trade;
//                    throw new CashException("定金和余款不是同一种支付方式");
//                }
//                //updateOrder.setYuuuid(uuid);
//                updateOrder.setYuordertime(ordertime);
//                logs.setOrdertype(Quantity.STATE_2);
//            } else {
//                //updateOrder.setUuid(uuid);
//                updateOrder.setOrdertime(ordertime);
//                logs.setOrdertype(Quantity.STATE_1);
//            }
//            ordersMapper.updateByPrimaryKeySelective(updateOrder);
//
//            payTradeLogsList.add(logs);
//        }
//
//        payTradeLogsService.batchInsert(payTradeLogsList);
//
//
//        //计算总金额
//        for (OrderProduct orderProduct : orderProducts) {
//            //不是远期
//            if (orderProduct.getProtype() == Quantity.STATE_0) {
//                //没有支付
//                if (orderProduct.getPaystate() == Quantity.STATE_0) {
//                    sumpay = sumpay.add(orderProduct.getActualpayment());
//                } else {
//                    flag = true;
//                }
//            }
//            //全款
//            if (orderProduct.getProtype() == Quantity.STATE_1) {
//                if (orderProduct.getPaystate() == Quantity.STATE_0) {
//                    sumpay = sumpay.add(orderProduct.getActualpayment());
//                } else {
//                    flag = true;
//                }
//            }
//            //定金
//            if (orderProduct.getProtype() == Quantity.STATE_2) {
//                //定金付
//                if (orderProduct.getPaystate() == Quantity.STATE_0) {
//                    sumpay = sumpay.add(orderProduct.getPartpay());
//                    //余款支付
//                } else if (orderProduct.getPaystate() == Quantity.STATE_2) {
//                    sumpay = sumpay.add(orderProduct.getYupay().add(orderProduct.getFreight()));
//                } else {
//                    flag = true;
//                }
//            }
//        }
//
//        if (flag) {
//            throw new CashException("有订单已提交支付");
//        } else {
//            trade.setAmount((sumpay.multiply(new BigDecimal(100))).longValue());
//            trade.setOutTradeNo(uuid);
//            trade.setSubject("订单支付");
//            trade.setBody(pdname);
//            trade.setDatetime(ordertime);
//            basicRet.setResult(BasicRet.SUCCESS);
//            trade.setPayUrlRet(basicRet);
//        }
//        return trade;
//    }




    public Trade buildFromOrderId(String orders, Short paytype,String uuid) throws CashException {
        PayUrlRet basicRet = new PayUrlRet();
        Trade trade = new Trade();

        List<Orders> ordersList = ordersMapper.getOrdersByInIds(orders);
        if(ordersList.size()==0){
            throw new CashException("订单不存在，请联系网站管理员");
        }


        List<OrderProduct> orderProducts = orderProductMapper.getOrderProductByInOrderids(orders);
        if(orderProducts.size()==0){
            throw  new CashException("订单商品部存在，请联系网站管理员");
        }

        Long ordertime = System.currentTimeMillis();

        BigDecimal sumpay = new BigDecimal(0);
        boolean flag = false;
        String pdname = "紧商订单商品信息";

        //判断订单状态
        //订单类型0=立即发货1=远期全款2=远期定金3=远期余款
        //订单状态0=待付款1=待发货3=待收货4=待验货5=已完成7=已关闭8=备货中9=备货完成
        for(Orders od : ordersList){
            if(od.getOrdertype() == Quantity.STATE_0 || od.getOrdertype() == Quantity.STATE_1 || od.getOrdertype() == Quantity.STATE_2){
                if(od.getOrderstatus() != Quantity.STATE_0){
                    throw   new CashException(od.getOrderno()+"订单状态不正确，不可付款");
                }
            }else if(od.getOrdertype() == Quantity.STATE_3){
                if(od.getOrderstatus() != Quantity.STATE_9){
                    throw new CashException(od.getOrderno()+"订单状态不正确，不可付款");
                }
            }else{
                throw new CashException(od.getOrderno()+"订单状态不正确，不可付款");
            }
        }

        List<PayTradeLogs> payTradeLogsList = new ArrayList<>();

        for (Orders order : ordersList) {
            PayTradeLogs logs = new PayTradeLogs();
            logs.setOrderid(order.getId());
            logs.setOrderno(order.getOrderno());
            logs.setOuttradeno(uuid);
            logs.setCreatetime(new Date());
            logs.setPaytype(paytype);
            logs.setPaystates(Quantity.STATE_0);


            Orders updateOrder = new Orders();
            updateOrder.setId(order.getId());

            if(order.getOrdertype() == Quantity.STATE_2) { //远期订金
                updateOrder.setOrdertime(ordertime);
                logs.setOrdertype(Quantity.STATE_3);
                sumpay = sumpay.add(order.getDeposit());
            }else if (order.getOrdertype() == Quantity.STATE_3) { //定金和余款必须是同一种支付方式
                if (order.getPaytype() != paytype) {
                    throw new CashException("定金和余款不是同一种支付方式");
                }
                updateOrder.setYuordertime(ordertime);
                logs.setOrdertype(Quantity.STATE_2);

                //远期余款
                sumpay = sumpay.add(order.getBalance());
            } else {
                updateOrder.setOrdertime(ordertime);
                logs.setOrdertype(Quantity.STATE_1);
                sumpay = sumpay.add(order.getActualpayment());
            }
            ordersMapper.updateByPrimaryKeySelective(updateOrder);

            payTradeLogsList.add(logs);
        }

        payTradeLogsService.batchInsert(payTradeLogsList);


        /*
        //计算总金额
        for (OrderProduct orderProduct : orderProducts) {
            //不是远期
            if (orderProduct.getProtype() == Quantity.STATE_0) {
                //没有支付
                if (orderProduct.getPaystate() == Quantity.STATE_0) {
                    sumpay = sumpay.add(orderProduct.getActualpayment());
                } else {
                    flag = true;
                }
            }
            //全款
            if (orderProduct.getProtype() == Quantity.STATE_1) {
                if (orderProduct.getPaystate() == Quantity.STATE_0) {
                    sumpay = sumpay.add(orderProduct.getActualpayment());
                } else {
                    flag = true;
                }
            }
            //定金
            if (orderProduct.getProtype() == Quantity.STATE_2) {
                //定金付
                if (orderProduct.getPaystate() == Quantity.STATE_0) {
                    sumpay = sumpay.add(orderProduct.getPartpay());
                    //余款支付
                } else if (orderProduct.getPaystate() == Quantity.STATE_2) {
                    sumpay = sumpay.add(orderProduct.getYupay().add(orderProduct.getFreight()));
                } else {
                    flag = true;
                }
            }
        }
        */

        trade.setAmount((sumpay.multiply(new BigDecimal(100))).longValue());
        trade.setOutTradeNo(uuid);
        trade.setSubject("订单支付");
        trade.setBody(pdname);
        trade.setDatetime(ordertime);
        basicRet.setResult(BasicRet.SUCCESS);
        trade.setPayUrlRet(basicRet);
        return trade;
    }



    /**
     * 保存资金明细和修改订单状态
     *
     * @param ordersList 订单数组（orderno:price）
     */
    private void payMethod(List<Orders> ordersList, short type, String transactionid) {
        //一个订单一条资金记录
        List<BuyerCapital> buyerCapitals = new ArrayList<BuyerCapital>();
        List<SalerCapital> salerCapitals = new ArrayList<SalerCapital>();
        List<BuyerCapital> statementList=new ArrayList<>();
        for (Orders order : ordersList) {

            String transactionNo = GenerateNo.getTransactionNo();
            Date tranTime = new Date();
            //创建订单资金明细
            BuyerCapital buyerOrderCapital = createBuyerOrderCapital(order, type, tranTime, transactionNo, transactionid);
            statementList.add(buyerOrderCapital);
            SalerCapital salerCapital = createSalerOrderCapital(order, tranTime, transactionNo, transactionid);

            buyerCapitals.add(buyerOrderCapital);

            if (salerCapital.getRefundamount() == null) {
                salerCapital.setRefundamount(new BigDecimal(0));
            }

            salerCapitals.add(salerCapital);
        }
        //批量保存买家卖家资金明细
        if (buyerCapitals.size() > Quantity.STATE_0) {
            buyerCapitalService.insertAll(buyerCapitals);
        }
        if (salerCapitals.size() > Quantity.STATE_0) {
            salerCapitalMapper.insertAll(salerCapitals);
        }
        if (statementList.size()>Quantity.STATE_0){
            for (BuyerCapital buyerCapital:statementList) {
                buyerCapitalService.insertStateSelective(buyerCapital,true);
            }
        }
    }

    /**
     * 创建买家订单资金明细
     *
     * @param order
     * @param type
     * @param tranTime
     * @param tradeNo
     * @return
     */
    private BuyerCapital createBuyerOrderCapital(Orders order, Short type, Date tranTime, String tradeNo, String transactionid) {

        BuyerCapital buyerCapital = new BuyerCapital();
        buyerCapital.setTradetime(tranTime);
        buyerCapital.setOrderno(order.getOrderno());

        buyerCapital.setPaytype(type);
        buyerCapital.setMemberid(order.getMemberid());
        buyerCapital.setTradeno(tradeNo);
        buyerCapital.setRechargestate(Quantity.STATE_1);
        buyerCapital.setTransactionid(transactionid);

        OrderProductExample orderProductExample = new OrderProductExample();
        orderProductExample.createCriteria().andOrdernoEqualTo(order.getOrderno());

        List<OrderProduct> orderProducts = orderProductMapper.selectByExample(orderProductExample);
        BigDecimal capital = new BigDecimal(0);

        /*
        //立即发货
        if (order.getOrdertype() == Quantity.STATE_0) {
            if (order.getOrderstatus() == Quantity.STATE_0) {
                buyerCapital.setCapitaltype(Quantity.STATE_0);
                for (OrderProduct orderProduct : orderProducts) {
                    capital = capital.add(orderProduct.getActualpayment());
                }
                //买家订单资金明细
                buyerCapital.setCapital(capital);
                buyerCapital.setRemark("订单金额");
            }
        }
        //远期全款
        if (order.getOrdertype() == Quantity.STATE_1) {
            if (order.getOrderstatus() == Quantity.STATE_0) {
                buyerCapital.setCapitaltype(Quantity.STATE_9);
                for (OrderProduct orderProduct : orderProducts) {
                    capital = capital.add(orderProduct.getActualpayment());
                }
                buyerCapital.setCapital(capital);
                buyerCapital.setRemark("订单金额-远期全款");
            }
        }
        //定金
        if (order.getOrdertype() == Quantity.STATE_2) {
            if (order.getOrderstatus() == Quantity.STATE_0) {
                buyerCapital.setCapitaltype(Quantity.STATE_7);
                for (OrderProduct orderProduct : orderProducts) {
                    capital = capital.add(orderProduct.getPartpay());
                }
                buyerCapital.setCapital(capital);
                buyerCapital.setRemark("订单金额-远期订金");

            }
        }
        //余款
        if (order.getOrdertype() == Quantity.STATE_3) {
            if (order.getOrderstatus() == Quantity.STATE_9) {
                buyerCapital.setCapitaltype(Quantity.STATE_8);
                for (OrderProduct orderProduct : orderProducts) {
                    capital = capital.add(orderProduct.getYupay().add(orderProduct.getFreight()));
                }
                buyerCapital.setCapital(capital);
                buyerCapital.setRemark("订单金额-远期余款");
            }
        }
        */


        //立即发货
        if (order.getOrdertype() == Quantity.STATE_0) {
            if (order.getOrderstatus() == Quantity.STATE_0) {
                buyerCapital.setCapitaltype(Quantity.STATE_0);
                capital = order.getActualpayment();
                //买家订单资金明细
                buyerCapital.setCapital(capital);
                buyerCapital.setRemark("订单金额");
            }
        }
        //远期全款
        if (order.getOrdertype() == Quantity.STATE_1) {
            if (order.getOrderstatus() == Quantity.STATE_0) {
                buyerCapital.setCapitaltype(Quantity.STATE_9);
                capital = order.getActualpayment();
                buyerCapital.setCapital(capital);
                buyerCapital.setRemark("订单金额-远期全款");
            }
        }
        //定金
        if (order.getOrdertype() == Quantity.STATE_2) {
            if (order.getOrderstatus() == Quantity.STATE_0) {
                buyerCapital.setCapitaltype(Quantity.STATE_7);
                capital = order.getDeposit();
                buyerCapital.setCapital(capital);
                buyerCapital.setRemark("订单金额-远期订金");
            }
        }
        //余款
        if (order.getOrdertype() == Quantity.STATE_3) {
            if (order.getOrderstatus() == Quantity.STATE_9) {
                buyerCapital.setCapitaltype(Quantity.STATE_8);
                capital = order.getBalance();
                buyerCapital.setCapital(capital);
                buyerCapital.setRemark("订单金额-远期余款");
            }
        }


        return buyerCapital;
    }


    /**
     * 创建卖家订单资金
     *
     * @param order
     * @param TranNo
     * @param tranTime
     * @return
     */
    public SalerCapital createSalerOrderCapital(Orders order, Date tranTime, String TranNo, String transactionid) {
        SalerCapital salerCapital = new SalerCapital();
        salerCapital.setTradetime(tranTime);
        salerCapital.setOrderno(order.getOrderno());
        salerCapital.setTradeno(TranNo);
        salerCapital.setRechargestate(Quantity.STATE_1);
        salerCapital.setMemberid(order.getSaleid());
        salerCapital.setTransactionid(transactionid);


//        Member seller = memberMapper.selectByPrimaryKey(order.getSaleid());

        OrderProductExample orderProductExample = new OrderProductExample();
        orderProductExample.createCriteria().andOrdernoEqualTo(order.getOrderno());
        List<OrderProduct> orderProducts = orderProductMapper.selectByExample(orderProductExample);

        BigDecimal capital = new BigDecimal(0);
        /*
        //立即发货
        if (order.getOrdertype() == Quantity.STATE_0) {
            if (order.getOrderstatus() == Quantity.STATE_0) {
                salerCapital.setCapitaltype(Quantity.STATE_0);
                for (OrderProduct orderProduct : orderProducts) {
                    capital = capital.add(orderProduct.getActualpayment());
                }
                //买家订单资金明细
                salerCapital.setOrdercapital(capital);
                salerCapital.setRemark("订单金额");
            }
        }
        //远期全款
        if (order.getOrdertype() == Quantity.STATE_1) {
            if (order.getOrderstatus() == Quantity.STATE_0) {
                salerCapital.setCapitaltype(Quantity.STATE_9);
                for (OrderProduct orderProduct : orderProducts) {
                    capital = capital.add(orderProduct.getActualpayment());
                }
                salerCapital.setOrdercapital(capital);
                salerCapital.setRemark("订单金额-远期全款");
            }
        }
        //定金
        if (order.getOrdertype() == Quantity.STATE_2) {
            if (order.getOrderstatus() == Quantity.STATE_0) {
                salerCapital.setCapitaltype(Quantity.STATE_10);
                for (OrderProduct orderProduct : orderProducts) {
                    capital = capital.add(orderProduct.getPartpay().add(orderProduct.getFreight()));
                }
                salerCapital.setOrdercapital(capital);
                salerCapital.setRemark("订单金额-远期定金");

            }
        }
        //余款
        if (order.getOrdertype() == Quantity.STATE_3) {
            if (order.getOrderstatus() == Quantity.STATE_9) {
                salerCapital.setCapitaltype(Quantity.STATE_8);
                for (OrderProduct orderProduct : orderProducts) {
                    capital = capital.add(orderProduct.getYupay());
                }
                salerCapital.setOrdercapital(capital);
                salerCapital.setRemark("订单金额-远期余款");
            }
        }*/


        //立即发货
        if (order.getOrdertype() == Quantity.STATE_0) {
            if (order.getOrderstatus() == Quantity.STATE_0) {
                salerCapital.setCapitaltype(Quantity.STATE_0);
//                for (OrderProduct orderProduct : orderProducts) {
//                    capital = capital.add(orderProduct.getActualpayment());
//                }
                capital = order.getActualpayment();
                //买家订单资金明细
                salerCapital.setOrdercapital(capital);
                salerCapital.setRemark("订单金额");
            }
        }
        //远期全款
        if (order.getOrdertype() == Quantity.STATE_1) {
            if (order.getOrderstatus() == Quantity.STATE_0) {
                salerCapital.setCapitaltype(Quantity.STATE_9);
//                for (OrderProduct orderProduct : orderProducts) {
//                    capital = capital.add(orderProduct.getActualpayment());
//                }
                capital = order.getActualpayment();
                salerCapital.setOrdercapital(capital);
                salerCapital.setRemark("订单金额-远期全款");
            }
        }
        //定金
        if (order.getOrdertype() == Quantity.STATE_2) {
            if (order.getOrderstatus() == Quantity.STATE_0) {
                salerCapital.setCapitaltype(Quantity.STATE_10);
//                for (OrderProduct orderProduct : orderProducts) {
//                    capital = capital.add(orderProduct.getPartpay());
//                }
                capital = order.getDeposit();

                salerCapital.setOrdercapital(capital);
                salerCapital.setRemark("订单金额-远期定金");

            }
        }
        //余款
        if (order.getOrdertype() == Quantity.STATE_3) {
            if (order.getOrderstatus() == Quantity.STATE_9) {
                salerCapital.setCapitaltype(Quantity.STATE_8);
//                for (OrderProduct orderProduct : orderProducts) {
//                    capital = capital.add(orderProduct.getYupay().add(orderProduct.getFreight()));
//                }
                capital = order.getBalance();
                salerCapital.setOrdercapital(capital);
                salerCapital.setRemark("订单金额-远期余款");
            }
        }



//        if (seller != null) {
//            //增加卖家冻结金额
//            seller.setSellerfreezebanlance(seller.getSellerfreezebanlance().add(capital));
//            memberMapper.updateByPrimaryKeySelective(seller);
//        }


        return salerCapital;
    }


    /**
     * 买家充值结果回调:
     * order: 充值单号
     * channel: alipay / wxpay
     */
    public boolean notifyBuyerRecharge(String order, String transactionid,short type) throws CashException {
        BuyerCapital buyerCapital = buyerCapitalService.getBuyerCapitalByRechargenumber(order);

        if (buyerCapital != null) {
            if(buyerCapital.getRechargestate() == Quantity.STATE_1){
               return false;
            }

            BuyerCapital updateCapital = new BuyerCapital();
            updateCapital.setId(buyerCapital.getId());
            updateCapital.setRechargestate(Quantity.STATE_1);
            updateCapital.setSuccesstime(new Date());
            updateCapital.setTransactionid(transactionid);
            buyerCapitalService.updateByPrimaryKeySelective(updateCapital);

            //插入对账单
            BuyerCapital updateCapital1=buyerCapitalService.getById(updateCapital.getId());
            updateCapital1.setPaytype(type);
            BuyerStatement buyerStatement=new BuyerStatement();
            buyerStatement.setMemberid(updateCapital1.getMemberid());
            buyerStatement.setContractno(updateCapital1.getRechargenumber());
            buyerStatement.setType((short) StatementType.StType6.getTyep());
            buyerStatement.setDeliveryamount(Quantity.BIG_DECIMAL_0);
            buyerStatement.setReceiptamount(updateCapital1.getCapital());
            buyerStatement.setInvoiceamount(Quantity.BIG_DECIMAL_0);
            buyerStatement.setPaytype(updateCapital1.getPaytype());
            buyerStatement.setCreatetime(new Date());
            buyerStatement.setPayno(updateCapital1.getTransactionid());
//            Member member=memberMapper.getMemberByid(updateCapital1.getMemberid());
            BuyerCompanyInfo buyerCompanyInfo=buyerCompanyInfoMapper.getBuyerCompanyInfoByMemberId(buyerCapital.getMemberid());
            if (buyerCompanyInfo!=null){
                buyerStatement.setRemark(updateCapital1.getMemberid()+"\t\n"+buyerCompanyInfo.getCompanyname());
            }else {
                buyerStatement.setRemark(updateCapital1.getMemberid()+"");
            }
            statementService.insertStatement(buyerStatement);
            //修改买家资金
            memberMapper.updateBuyerMemberBalanceInDb(buyerCapital.getMemberid(), buyerCapital.getCapital());

            return true;
        }

        return false;
    }


    /**
     * 卖家充值结果回调:F
     * order: 充值单号
     */
    public boolean notifySellerRecharge(String order, String transactionid) throws CashException {
        SalerCapital salerCapital = salerCapitalMapper.getSalerCapitalByRechargenumber(order);
        if (salerCapital != null) {
            if(salerCapital.getRechargestate() == Quantity.STATE_1){
               return false;
            }

            SalerCapital updateSaler = new SalerCapital();
            updateSaler.setId(salerCapital.getId());
            updateSaler.setRechargestate(Quantity.STATE_1);
            updateSaler.setSuccesstime(new Date());
            updateSaler.setTransactionid(transactionid);

            salerCapitalMapper.updateByPrimaryKeySelective(updateSaler);

            memberMapper.updateSellerMemberBalanceInDb(salerCapital.getMemberid(), salerCapital.getOrdercapital(), new BigDecimal(0));

            return true;
        }

        return false;
    }


    @Autowired
    private WMSService wmsService;


    /**
     * 支付结果回调:
     * outTradeNo: 订单号
     * channel: alipay / wxpay
     * transactionid  第三方订单号
     */
/*
    public boolean notify(String outTradeNo, String channel, String transactionid) {
        // 做订单的支付成功处理
        // 注意重复消费的问题
        // 处理失败 则返回false, 并 logger.error
        List<Orders> ordersList = getOrdersByUUID(outTradeNo);

        if(ordersList != null && ordersList.size()>0){
            for(Orders order : ordersList) {
                if (order.getOrderstatus() == Quantity.STATE_7) {
                    order.setOrderstatus(Quantity.STATE_0);
                }
            }
        }

        if (ordersList == null || ordersList.size() == 0) {
            ordersList = getOrdersByYUUUID(outTradeNo);
        }

        if (ordersList.size() > 0) {
            //Member member = memberMapper.selectByPrimaryKey(ordersList.get(0).getMemberid());
            Member updateMember = new Member();
            updateMember.setId(ordersList.get(0).getMemberid());
            updateMember.setIsbuy(Quantity.STATE_2);
            memberMapper.updateByPrimaryKeySelective(updateMember);
        }


        //创建资金明细
        if (channel.equals("alipay")) {
            payMethod(ordersList, Quantity.STATE_0, transactionid);
        }
        if (channel.equals("wxpay")) {
            payMethod(ordersList, Quantity.STATE_1, transactionid);
        }
        if (channel.equals("bank")) {
            payMethod(ordersList, Quantity.STATE_2, transactionid);
        }

        StringBuffer orderids = new StringBuffer();
        for (Orders order : ordersList) {

            if (order.getOrderstatus() == Quantity.STATE_7) {
                order.setOrderstatus(Quantity.STATE_0);
            }

            orderids.append(order.getId()).append(",");
            if (channel.equals("alipay")) {
                order.setPaytype(Quantity.STATE_0);
            }
            if (channel.equals("wxpay")) {
                order.setPaytype(Quantity.STATE_1);
            }
            if (channel.equals("bank")) {
                order.setPaytype(Quantity.STATE_2);
            }
            order.setPaymenttime(new Date());
            order.setTransactionid(transactionid);

            //立即发货
            if (order.getOrdertype() == Quantity.STATE_0) {
                if (order.getOrderstatus() == Quantity.STATE_0) {
                    order.setOrderstatus(Quantity.STATE_1);
                }
            }
            //远期全款
            if (order.getOrdertype() == Quantity.STATE_1) {
                if (order.getOrderstatus() == Quantity.STATE_0) {
                    order.setOrderstatus(Quantity.STATE_8);
                }
            }
            //定金
            if (order.getOrdertype() == Quantity.STATE_2) {
                if (order.getOrderstatus() == Quantity.STATE_0) {
                    order.setOrdertype(Quantity.STATE_3);
                    order.setOrderstatus(Quantity.STATE_8);
                }
            }
            //余款
            if (order.getOrdertype() == Quantity.STATE_3) {
                if (order.getOrderstatus() == Quantity.STATE_9) {
                    order.setOrderstatus(Quantity.STATE_1);
                }
            }


            ordersMapper.updateByPrimaryKeySelective(order);
            // wms
        }


        ordersService.smsNotifySellerToOrders(ordersList);

        wmsService.synOrders(ordersList);
        if (ordersList.size() > 0) {
            orderids.deleteCharAt(orderids.length() - 1);
        }
        //进行订单的主动下单，向中间件管理平台post数据
        ordersService.initiativeOrderIssue(orderids.toString());
        List<OrderProduct> orderProducts = orderProductMapper.getOrderProductByInOrderids(orderids.toString());

        //计算总金额
        for (OrderProduct orderProduct : orderProducts) {
            //不是远期
            if (orderProduct.getProtype() == Quantity.STATE_0) {
                orderProduct.setPaystate(Quantity.STATE_1);
            }
            //全款
            if (orderProduct.getProtype() == Quantity.STATE_1) {
                orderProduct.setPaystate(Quantity.STATE_1);
            }
            //定金
            if (orderProduct.getProtype() == Quantity.STATE_2) {
                //定金付
                if (orderProduct.getPaystate() == Quantity.STATE_0) {
                    orderProduct.setPaystate(Quantity.STATE_2);
                    //余款支付
                } else if (orderProduct.getPaystate() == Quantity.STATE_2) {
                    orderProduct.setPaystate(Quantity.STATE_3);
                }
            }
            orderProductMapper.updateByPrimaryKeySelective(orderProduct);
        }


        for(Orders orders : ordersList){
            //操作日志
            OperateLog operateLog = new OperateLog();
            operateLog.setContent("订单已支付");
            operateLog.setOpid(orders.getMemberid());
            operateLog.setOpname(orders.getMembername());
            operateLog.setOptime(new Date());
            operateLog.setOptype(Quantity.STATE_0);
            operateLog.setOrderid(orders.getId());
            ordersService.saveOperatelog(operateLog);
        }

        return true;
    }
*/

    public boolean notify(String outTradeNo, String channel, String transactionid) throws MyException {
        // 做订单的支付成功处理
        // 注意重复消费的问题
        // 处理失败 则返回false, 并 logger.error

        List<PayTradeLogs> list = payTradeLogsService.getByOuttradeno(outTradeNo);

        List<Orders> ordersList = new ArrayList<>();
        for(PayTradeLogs payTradeLogs : list){
            Orders order = ordersService.getOrdersById(payTradeLogs.getOrderid());

            if(Arrays.asList(new Short[]{1,3,4,5,8}).contains(order.getOrderstatus())){
                logger.error("outTradeNo："+outTradeNo+"---transactionid:"+transactionid);
                throw new MyException("订单状态不合法，请联系网站客服");
            }

            if(order==null) continue;
            Orders updateOrder = new Orders();
            updateOrder.setId(order.getId());

            if(payTradeLogs.getOrdertype() == Quantity.STATE_1){
                order.setUuid(outTradeNo);
                updateOrder.setUuid(outTradeNo);
            }else if(payTradeLogs.getOrdertype() == Quantity.STATE_2){
                order.setYuuuid(outTradeNo);
                updateOrder.setYuuuid(outTradeNo);
            }

            if (order.getOrderstatus() == Quantity.STATE_7) {
                order.setOrderstatus(Quantity.STATE_0);
                updateOrder.setOrderstatus(Quantity.STATE_0);
            }

            if (channel.equals("alipay")) {
                updateOrder.setPaytype(Quantity.STATE_0);
            }
            if (channel.equals("wxpay")) {
                updateOrder.setPaytype(Quantity.STATE_1);
            }
            if (channel.equals("bank")) {
                updateOrder.setPaytype(Quantity.STATE_2);
            }

            updateOrder.setTransactionid(transactionid);
            updateOrder.setPaymenttime(new Date());
            order.setPaymenttime(new Date());



            //立即发货
            if (order.getOrdertype() == Quantity.STATE_0) {
                if (order.getOrderstatus() == Quantity.STATE_0) {
                    updateOrder.setOrderstatus(Quantity.STATE_1);
                }
            }
            //远期全款
            if (order.getOrdertype() == Quantity.STATE_1) {
                if (order.getOrderstatus() == Quantity.STATE_0) {
                    updateOrder.setOrderstatus(Quantity.STATE_8);
                }
            }
            //定金
            if (order.getOrdertype() == Quantity.STATE_2) {
                if (order.getOrderstatus() == Quantity.STATE_0) {
                    updateOrder.setOrdertype(Quantity.STATE_3);
                    updateOrder.setOrderstatus(Quantity.STATE_8);
                }
            }
            //余款
            if (order.getOrdertype() == Quantity.STATE_3) {
                if (order.getOrderstatus() == Quantity.STATE_9) {
                    updateOrder.setOrderstatus(Quantity.STATE_1);
                }
            }

            List<OrderProduct> orderProducts = orderProductMapper.getByOrderid(order.getId());
            for (OrderProduct orderProduct : orderProducts) {
                OrderProduct updateProduct = new OrderProduct();
                updateProduct.setId(orderProduct.getId());

                //不是远期
                if (orderProduct.getProtype() == Quantity.STATE_0) {
                    updateProduct.setPaystate(Quantity.STATE_1);
                }
                //全款
                if (orderProduct.getProtype() == Quantity.STATE_1) {
                    updateProduct.setPaystate(Quantity.STATE_1);
                }
                //定金
                if (orderProduct.getProtype() == Quantity.STATE_2) {
                    //定金付
                    if (orderProduct.getPaystate() == Quantity.STATE_0) {
                        updateProduct.setPaystate(Quantity.STATE_2);
                        //余款支付
                    } else if (orderProduct.getPaystate() == Quantity.STATE_2) {
                        updateProduct.setPaystate(Quantity.STATE_3);
                    }
                }
                orderProductMapper.updateByPrimaryKeySelective(updateProduct);
            }
            ordersMapper.updateByPrimaryKeySelective(updateOrder);


            PayTradeLogs updateLogs = new PayTradeLogs();
            updateLogs.setId(payTradeLogs.getId());
            updateLogs.setPaystates(Quantity.STATE_1);
            payTradeLogsService.updateByPrimaryKeySelective(updateLogs);

            ordersList.add(order);
        }

        //创建资金明细
        if (channel.equals("alipay")) {
            payMethod(ordersList, Quantity.STATE_0, transactionid);
        }
        if (channel.equals("wxpay")) {
            payMethod(ordersList, Quantity.STATE_1, transactionid);
        }
        if (channel.equals("bank")) {
            payMethod(ordersList, Quantity.STATE_2, transactionid);
        }

        //ordersService.smsNotifySellerToOrders(ordersList);
        wmsService.synOrders(ordersList);

        if (ordersList.size() > 0) {
            //Member member = memberMapper.selectByPrimaryKey(ordersList.get(0).getMemberid());
            Member updateMember = new Member();
            updateMember.setId(ordersList.get(0).getMemberid());
            updateMember.setIsbuy(Quantity.STATE_2);
            //判断是否下过单 第一次下单设置标签名称为A+ size为1代表第一次下单 且为已支付的
            List<Orders>  list1  = ordersService.findOrdersByuseridAndOrderStatus(ordersList.get(0).getMemberid());
            if(list1.size()==1){
                updateMember.setLabelname("A+");
            }

            memberMapper.updateByPrimaryKeySelective(updateMember);
        }

        ordersService.smsNotifySellerToOrders(ordersList);

        for(Orders orders : ordersList){
            //操作日志
            OperateLog operateLog = new OperateLog();
            operateLog.setContent("订单已支付");
            operateLog.setOpid(orders.getMemberid());
            operateLog.setOpname(orders.getMembername());
            operateLog.setOptime(new Date());
            operateLog.setOptype(Quantity.STATE_0);
            operateLog.setOrderid(orders.getId());
            ordersService.saveOperatelog(operateLog);
        }

        return true;
    }


    public List<Orders> getOrdersByUUID(String outTradeNo) {
        OrdersExample ordersExample = new OrdersExample();
        ordersExample.createCriteria().andUuidEqualTo(outTradeNo);
        return ordersMapper.selectByExample(ordersExample);
    }

    public List<Orders> getOrdersByYUUUID(String outTradeNo) {
        OrdersExample ordersExample = new OrdersExample();
        ordersExample.createCriteria().andYuuuidEqualTo(outTradeNo);
        return ordersMapper.selectByExample(ordersExample);
    }

    public Refund buildRefund(String orderId) {
        // todo 构建refund
        Refund refund = new Refund();
        List<Orders> ordersList = getOrdersByUUID(orderId);
        refund.setOutTradeNo(orderId);

        return refund;
    }




    /**
     * 第三方退款
     * @param refund
     * @return
     * @throws CashException
     */
    public boolean backMoney(Refund refund) throws CashException {
        boolean result = false;
        try {
            if("alipay".equals(refund.getChannel())){
                result = alipayService.refund(refund);
            }else if("wxpay".equals(refund.getChannel())){
                result = wxPayService.refund(refund);
            }else {
                result = abcService.refund(refund);
            }
        }catch (Exception e){
            throw  new CashException("退款出现错误，请联系网站客服");
        }

       if(!result)  throw  new CashException("退款出现错误，请联系网站客服");

        return  true;
    }


    /**
     * 获取第三方支付的金额
     * @param outTradeNo
     * @return
     */
    public BigDecimal getTotalAmout(String outTradeNo){
        BigDecimal totalAmout = new BigDecimal(0);

        PayLogs payLogs = payLogsService.getByOuttradeno(outTradeNo);
        if(payLogs != null){
            return  payLogs.getMoney();
        }

        totalAmout =  ordersService.getSumActualpaymentByUUID(outTradeNo);
        if(totalAmout == null){
            return  Quantity.BIG_DECIMAL_0;
        }
        return  totalAmout;
    }


    public String getPayChannel(Short paytype){
        String channel = null;
        if(paytype == Quantity.STATE_0){
            channel = RefundConst.ALI_PAY;
        }else if(paytype == Quantity.STATE_1){
            channel = RefundConst.WX_PAY;
        }else if(paytype == Quantity.STATE_2){
            channel = RefundConst.BANK_PAY;
        }
        return channel;
    }





    public void test(String orderno,String uuid,String transactionid,String total_amount,String  channel,String payDate) throws CashException, MyException {

        Orders order = ordersService.getOrdersByOrderNo(orderno);
        if(order == null){
            System.out.println("订单不存在");
            return;
        }

        Short paytype = -1;
        //0=支付宝1=微信2=银行卡
        if(channel.equals("alipay")){
            paytype = 0;
        }else if(channel.equals("wxpay")){
            paytype = 1;
        }else if(channel.equals("bank")){
            paytype = 2;
        }


//        PayLogs payLogs = new PayLogs();
//        payLogs.setTransactionid(transactionid);
//        payLogs.setOuttradeno(uuid);
//        payLogs.setMoney(new BigDecimal(total_amount));
//        payLogs.setCreatetime(new Date());
//        payLogs.setChannel(channel);
//        payLogsService.insertSelective(payLogs);


        PayTradeLogs logs = new PayTradeLogs();
        logs.setOrderid(order.getId());
        logs.setOrderno(order.getOrderno());
        logs.setOuttradeno(uuid);
        logs.setCreatetime(new Date());
        logs.setPaytype(paytype);
        logs.setPaystates(Quantity.STATE_0);

        Long ordertime = DateUtils.StrToDate(payDate).getTime();

        Orders updateOrder = new Orders();
        updateOrder.setId(order.getId());
        //定金和余款必须是同一种支付方式
        if (order.getOrdertype() == Quantity.STATE_3) {
            if (order.getPaytype() != paytype) {
                //basicRet.setResult(BasicRet.ERR);
                //basicRet.setMessage("定金和余款不是同一种支付方式");
                //trade.setPayUrlRet(basicRet);
                //return trade;
                throw new CashException("定金和余款不是同一种支付方式");
            }
            //updateOrder.setYuuuid(uuid);
            updateOrder.setYuordertime(ordertime);

            logs.setOrdertype(Quantity.STATE_2);
        } else {
            //updateOrder.setUuid(uuid);
            updateOrder.setOrdertime(ordertime);
            logs.setOrdertype(Quantity.STATE_1);
        }

        notify(uuid,channel,transactionid);

        ordersMapper.updateByPrimaryKeySelective(updateOrder);
        payTradeLogsService.insertSelective(logs);


        System.out.println("完成");
    }


    public void testForCharge(String rechargeNo,String uuid,String transactionid,String total_amount,String  channel,String payDate) throws CashException, MyException {

//        Orders order = ordersService.getOrdersByOrderNo(orderno);
//        if(order == null){
//            System.out.println("订单不存在");
//            return;
//        }

        Short paytype = -1;
        //0=支付宝1=微信2=银行卡
        if(channel.equals("alipay")){
            paytype = 0;
        }else if(channel.equals("wxpay")){
            paytype = 1;
        }else if(channel.equals("bank")){
            paytype = 2;
        }

        String rechargeNo1="buy-"+rechargeNo;

        PayLogs payLogs = new PayLogs();
        payLogs.setTransactionid(transactionid);
        payLogs.setOuttradeno(rechargeNo1);
        payLogs.setMoney(new BigDecimal(total_amount));
        payLogs.setCreatetime(new Date());
        payLogs.setChannel(channel);
        payLogsService.insertSelective(payLogs);
//        ordersMapper.updateByPrimaryKeySelective(updateOrder);

        this.notifyBuyerRecharge(rechargeNo,transactionid,Quantity.STATE_0);
        System.out.println(notify(uuid,channel,transactionid));
    }


}
