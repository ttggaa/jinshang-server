package project.jinshang.mod_product;

import com.alipay.api.AlipayApiException;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import mizuki.project.core.restserver.config.BasicRet;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import project.jinshang.common.bean.MemberLogOperator;
import project.jinshang.common.bean.PageRet;
import project.jinshang.common.constant.AdminAuthorityConst;
import project.jinshang.common.constant.AgentDeliveryAddressConst;
import project.jinshang.common.constant.AppConstant;
import project.jinshang.common.constant.Quantity;
import project.jinshang.common.exception.CashException;
import project.jinshang.common.exception.MyException;
import project.jinshang.common.utils.*;
import project.jinshang.mod_activity.bean.LimitTimeProd;
import project.jinshang.mod_activity.bean.LimitTimeStore;
import project.jinshang.mod_admin.mod_commondata.service.CommonDataValueService;
import project.jinshang.mod_admin.mod_returnreason.bean.ReturnReason;
import project.jinshang.mod_admin.mod_returnreason.service.ReturnReasonService;
import project.jinshang.mod_admin.mod_statement.bean.BuyerStatement;
import project.jinshang.mod_admin.mod_statement.service.StatementService;
import project.jinshang.mod_admin.mod_transet.bean.TransactionSetting;
import project.jinshang.mod_admin.mod_transet.service.TransactionSettingService;
import project.jinshang.mod_cash.bean.BuyerCapital;
import project.jinshang.mod_cash.bean.SalerCapital;
import project.jinshang.mod_cash.service.BuyerCapitalService;
import project.jinshang.mod_cash.service.SalerCapitalService;
import project.jinshang.mod_company.bean.BuyerCompanyInfo;
import project.jinshang.mod_company.service.BuyerCompanyService;
import project.jinshang.mod_member.bean.Admin;
import project.jinshang.mod_member.bean.Member;
import project.jinshang.mod_member.service.AdminService;
import project.jinshang.mod_member.service.AdminUserService;
import project.jinshang.mod_member.service.MemberRateSettingService;
import project.jinshang.mod_member.service.MemberService;
import project.jinshang.mod_pay.bean.Refund;
import project.jinshang.mod_pay.mod_alipay.AlipayService;
import project.jinshang.mod_pay.mod_bankpay.AbcService;
import project.jinshang.mod_pay.mod_wxpay.MyWxPayService;
import project.jinshang.mod_pay.service.PayLogsService;
import project.jinshang.mod_pay.service.TradeService;
import project.jinshang.mod_product.bean.*;
import project.jinshang.mod_product.bean.dto.LogisticsInfoDto;
import project.jinshang.mod_product.bean.dto.OrderFrightDto;
import project.jinshang.mod_product.bean.dto.OrdersRet;
import project.jinshang.mod_product.bean.dto.StoreState;
import project.jinshang.mod_product.service.*;
import project.jinshang.mod_shippingaddress.bean.ShippingAddress;
import project.jinshang.mod_shippingaddress.service.ShippingAddressService;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by Administrator on 2017/11/9.
 */
@RestController
@RequestMapping("/rest/admin/orders")
@SessionAttributes({AppConstant.ADMIN_SESSION_NAME})
@Api(tags = "后台订单模块", description = "后台订单模块")
@Transactional(rollbackFor = Exception.class)
public class AdminOrdersAction {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private ShopCarService shopCarService;

    @Autowired
    private MemberRateSettingService memberRateSettingService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private ShippingAddressService shippingAddressService;

    @Autowired
    private TransactionSettingService transactionSettingService;

    @Autowired
    private MemberOperateLogService memberOperateLogService;

    @Autowired
    private MyWxPayService wxPayService;
    @Autowired
    private AlipayService alipayService;

    @Autowired
    private CommonDataValueService commonDataValueService;

    @Autowired
    private OrderProductBackService orderProductBackService;

    @Autowired
    private AbcService abcService;

    @Autowired
    private BuyerCapitalService buyerCapitalService;

    @Autowired
    private BillingRecordService billingRecordService;
    @Autowired
    private ProductInfoService productInfoService;

    @Autowired
    private BuyerCompanyService buyerCompanyService;


    @Autowired
    private OrderProductLogService orderProductLogService;


    @Autowired
    private OrderProductServices orderProductServices;

    @Autowired
    private OrderStoreStateLogService orderStoreStateLogService;

    @Autowired
    private OrderProductBackInfoService orderProductBackInfoService;

    @Autowired
    private ProductStoreService productStoreService;

    @Autowired
    private PayLogsService payLogsService;

    //2018年6月1日14:09:57
    //添加业务员信息
    @Autowired
    private AdminUserService adminUserService;
    //添加业务员信息
    @Autowired
    private AdminService adminService;

    @Autowired
    private TradeService tradeService;

    @Autowired
    private FreightService freightService;
    @Autowired
    private SalerCapitalService salerCapitalService;
    @Autowired
    private ReturnReasonService returnReasonService;

    @Autowired
    private LogisticsInfoService logisticsInfoService;
    @Autowired
    private StatementService statementService;

    @Autowired
    private YhqUseService yhqUseService;


    //远期全款打折率
    private static final BigDecimal allPayRate = new BigDecimal(0.99);

    @Value("${spring.profiles.active}")
    private String profile;

    MemberLogOperator memberLogOperator = new MemberLogOperator();

    private class OrderCarBackRet extends BasicRet {

        private class Data {
            private List<OrderProductBackInfo> orderProductBackInfos;
            private OrderProductBackInfo orderProductBackInfo;
            private List<OrderProduct> orderProducts;
            private OrderProduct orderProduct;

            public List<OrderProductBackInfo> getOrderProductBackInfos() {
                return orderProductBackInfos;
            }

            public void setOrderProductBackInfos(List<OrderProductBackInfo> orderProductBackInfos) {
                this.orderProductBackInfos = orderProductBackInfos;
            }

            public OrderProductBackInfo getOrderProductBackInfo() {
                return orderProductBackInfo;
            }

            public void setOrderProductBackInfo(OrderProductBackInfo orderProductBackInfo) {
                this.orderProductBackInfo = orderProductBackInfo;
            }

            public List<OrderProduct> getOrderProducts() {
                return orderProducts;
            }

            public void setOrderProducts(List<OrderProduct> orderProducts) {
                this.orderProducts = orderProducts;
            }

            public OrderProduct getOrderProduct() {
                return orderProduct;
            }

            public void setOrderProduct(OrderProduct orderProduct) {
                this.orderProduct = orderProduct;
            }
        }

        private Data data = new Data();

        public Data getData() {
            return data;
        }

        public void setData(Data data) {
            this.data = data;
        }
    }

    private class OrderCarRet extends BasicRet {
        private class Data {

            List<BillRecordComplex> listComplex;
            private List<BillType> billTypes;

            private SellerBillRecord sellerBillRecord;
            private List<Orders> ordersList;

            private OrderProductBack orderProductBack;

            private BigDecimal penalty;

            private String returnreason;

            List<LogisticsInfoDto> logisticsInfos;

            private List<ShippingAddress> shippingAddresses;

            private BigDecimal bigDecimal;

            private Orders orders;

            private List<OrderProduct> orderProducts;

            private OrderProduct orderProduct;

            private BillingRecord billingRecord;

            private BillRecordComplex billRecordComplex;

            private Map<String, BigDecimal> map;

            List<OperateLog> operateLogs;

            private String expressurl;

            public String getExpressurl() {
                return expressurl;
            }

            public void setExpressurl(String expressurl) {
                this.expressurl = expressurl;
            }

            public List<BillRecordComplex> getListComplex() {
                return listComplex;
            }

            public void setListComplex(List<BillRecordComplex> listComplex) {
                this.listComplex = listComplex;
            }

            public List<OperateLog> getOperateLogs() {
                return operateLogs;
            }

            public void setOperateLogs(List<OperateLog> operateLogs) {
                this.operateLogs = operateLogs;
            }

            public Map<String, BigDecimal> getMap() {
                return map;
            }

            public void setMap(Map<String, BigDecimal> map) {
                this.map = map;
            }

            public BillRecordComplex getBillRecordComplex() {
                return billRecordComplex;
            }

            public void setBillRecordComplex(BillRecordComplex billRecordComplex) {
                this.billRecordComplex = billRecordComplex;
            }

            public BillingRecord getBillingRecord() {
                return billingRecord;
            }

            public void setBillingRecord(BillingRecord billingRecord) {
                this.billingRecord = billingRecord;
            }

            public OrderProduct getOrderProduct() {
                return orderProduct;
            }

            public void setOrderProduct(OrderProduct orderProduct) {
                this.orderProduct = orderProduct;
            }

            public List<OrderProduct> getOrderProducts() {
                return orderProducts;
            }

            public void setOrderProducts(List<OrderProduct> orderProducts) {
                this.orderProducts = orderProducts;
            }

            public Orders getOrders() {
                return orders;
            }

            public void setOrders(Orders orders) {
                this.orders = orders;
            }

            public BigDecimal getBigDecimal() {
                return bigDecimal;
            }

            public void setBigDecimal(BigDecimal bigDecimal) {
                this.bigDecimal = bigDecimal;
            }

            public List<ShippingAddress> getShippingAddresses() {
                return shippingAddresses;
            }

            public void setShippingAddresses(List<ShippingAddress> shippingAddresses) {
                this.shippingAddresses = shippingAddresses;
            }

            private int orderNum;

            private boolean selfsupport;

            public boolean isSelfsupport() {
                return selfsupport;
            }

            public void setSelfsupport(boolean selfsupport) {
                this.selfsupport = selfsupport;
            }

            public int getOrderNum() {
                return orderNum;
            }

            public void setOrderNum(int orderNum) {
                this.orderNum = orderNum;
            }

            public List<Orders> getOrdersList() {
                return ordersList;
            }

            public void setOrdersList(List<Orders> ordersList) {
                this.ordersList = ordersList;
            }

            public OrderProductBack getOrderProductBack() {
                return orderProductBack;
            }

            public void setOrderProductBack(OrderProductBack orderProductBack) {
                this.orderProductBack = orderProductBack;
            }

            public SellerBillRecord getSellerBillRecord() {
                return sellerBillRecord;
            }

            public void setSellerBillRecord(SellerBillRecord sellerBillRecord) {
                this.sellerBillRecord = sellerBillRecord;
            }

            public List<BillType> getBillTypes() {
                return billTypes;
            }

            public void setBillTypes(List<BillType> billTypes) {
                this.billTypes = billTypes;
            }

            public BigDecimal getPenalty() {
                return penalty;
            }

            public void setPenalty(BigDecimal penalty) {
                this.penalty = penalty;
            }

            public String getReturnreason() {
                return returnreason;
            }

            public void setReturnreason(String returnreason) {
                this.returnreason = returnreason;
            }

            public List<LogisticsInfoDto> getLogisticsInfos() {
                return logisticsInfos;
            }

            public void setLogisticsInfos(List<LogisticsInfoDto> logisticsInfos) {
                this.logisticsInfos = logisticsInfos;
            }
        }

        private Data data = new Data();

        public Data getData() {
            return data;
        }

        public void setData(Data data) {
            this.data = data;
        }
    }


    @RequestMapping(value = "/getMemberOperateLogList", method = RequestMethod.POST)
    @ApiOperation(value = "后台获取操作日志列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ipaddress", value = "买家名称", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "memberid", value = "卖家名称", required = false, paramType = "query", dataType = "long"),
            @ApiImplicitParam(name = "membername", value = "线上线下订单标识0=线上1=线下", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "content", value = "支付方式0=支付宝1=微信2=银行卡3=余额4=授信", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "startTime", value = "开始时间", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "endTime", value = "结束时间", required = false, paramType = "query", dataType = "string"),
    })
    public PageRet getMemberOperateLogList(Model model, int pageNo, int pageSize, MemberQueryParam param) {
        PageRet pageRet = new PageRet();
        PageInfo pageInfo = ordersService.getMemberOperateLogList(pageNo, pageSize, param);
        pageRet.data.setPageInfo(pageInfo);
        pageRet.setResult(BasicRet.SUCCESS);
        pageRet.setMessage("返回成功");
        return pageRet;
    }
    @RequestMapping(value = "/getAdminOperateLogList", method = RequestMethod.POST)
    @ApiOperation(value = "后台获取管理员操作日志列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "adminname", value = "管理员名称" ,required = false, paramType = "query" ,dataType = "string"),
            @ApiImplicitParam(name = "type", value = "操作类型1新增2删除3修改0所有" ,required = false, paramType = "query" ,dataType = "int"),
            @ApiImplicitParam(name = "startTime", value = "开始时间", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "endTime", value = "结束时间", required = false, paramType = "query", dataType = "string"),
    })
    public PageRet getAdminOperateLogList(Model model ,int pageNo ,int pageSize, AdminQueryParam param){
        PageRet pageRet = new PageRet();
        PageInfo pageInfo = ordersService.getAdminOperateLogList(pageNo, pageSize, param);
        pageRet.data.setPageInfo(pageInfo);
        pageRet.setResult(BasicRet.SUCCESS);
        pageRet.setMessage("返回成功");
        return pageRet;
    }

    @RequestMapping(value = "/getAllMemberOrderList", method = RequestMethod.POST)
    @ApiOperation(value = "后台获取订单列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "memberName", value = "买家名称", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "sellerName", value = "卖家名称", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "isonline", value = "线上线下订单标识0=线上1=线下", required = false, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "payType", value = "支付方式0=支付宝1=微信2=银行卡3=余额4=授信", required = false, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "orderType", value = "订单类型0=立即发货1=远期全款2=远期定金3=远期余款", required = false, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "orderNo", value = "订单号", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "invoiceheadup", value = "开票抬头", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "code", value = "合同号", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "tranNo", value = "交易号", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "transactionid", value = "支付宝交易号", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "clerkname", value = "客服人员", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "waysalesman", value = "介绍人", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "stand", value = "规格", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "startTime", value = "开始时间", required = false, paramType = "query", dataType = "date"),
            @ApiImplicitParam(name = "endTime", value = "结束时间", required = false, paramType = "query", dataType = "date"),
            @ApiImplicitParam(name = "orderState", value = "订单状态0=待付款1=待发货3=待收货4=待验货5=已完成7=已关闭8=备货中9=备货完成11=卖家违约订单", required = false, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "orderStates", value = "订单状态(如果多个状态就以逗号分隔)0=待付款1=待发货3=待收货4=待验货5=已完成7=已关闭8=备货中9=备货完成11=卖家违约订单", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "deliverytype", value = "发货模式 0-卖家直发，1-平台代发", required = false, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "registerTimeStart", value = "注册开始时间", required = false, paramType = "query", dataType = "date"),
            @ApiImplicitParam(name = "registerTimeEnd", value = "注册结束时间", required = false, paramType = "query", dataType = "date"),
            @ApiImplicitParam(name = "deliverytype", value = "发货方式 0-卖家直发，1-平台代发", required = false, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "shipto", value = "收货人", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "sendstatus", value = "发货状态 全部订单为不传 1为待发货（查询待发货和部分发货订单）3为待收货订单(查待收货和部分发货) 10为部分发货订单(只查10部分发货)", required = false, paramType = "query", dataType = "long"),
            @ApiImplicitParam(name = "overtime", value = "超时时间 全部为不传 1=超时1天 2=超时2天 3=超时3天", required = false, paramType = "query", dataType = "int"),

    })
    @PreAuthorize("hasAuthority('" + AdminAuthorityConst.ORDERMANAGEMENT + "')")
    public PageRet getAllMemberOrderList(Model model, OrderQueryParam param) {
        PageRet pageRet = new PageRet();
        PageInfo pageInfo = ordersService.getAllMemberOrdersList(param);
        pageRet.data.setPageInfo(pageInfo);
        pageRet.setResult(BasicRet.SUCCESS);
        pageRet.setMessage("返回成功");
        return pageRet;
    }

    @RequestMapping(value = "/getAllMemberOrderCountList", method = RequestMethod.POST)
    @ApiOperation(value = "后台获取订单统计列表")
    @ApiImplicitParams({

            @ApiImplicitParam(name = "memberid", value = "会员ID", paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "sellerName", value = "卖家名称", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "startTime", value = "开始时间", paramType = "query", dataType = "date"),
            @ApiImplicitParam(name = "endTime", value = "结束时间", paramType = "query", dataType = "date")
    })
    @PreAuthorize("hasAuthority('" + AdminAuthorityConst.ORDERMANAGEMENT + "')")
    public PageRet getAllMemberOrderCountList(OrderQueryParam param) {
        PageRet pageRet = new PageRet();
        PageInfo pageInfo = ordersService.getAllMemberOrdersCountList(param);
        pageRet.data.setPageInfo(pageInfo);
        pageRet.setResult(BasicRet.SUCCESS);
        pageRet.setMessage("返回成功");
        return pageRet;
    }

    @RequestMapping(value = "/getAllMemberOrderCountDetail", method = RequestMethod.POST)
    @ApiOperation(value = "后台获取订单统计详情")
    @ApiImplicitParams({

            @ApiImplicitParam(name = "memberid", value = "会员ID", paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "sellerName", value = "卖家名称", paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "startTime", value = "开始时间", paramType = "query", dataType = "date"),
            @ApiImplicitParam(name = "endTime", value = "结束时间", paramType = "query", dataType = "date")
    })
    @PreAuthorize("hasAuthority('" + AdminAuthorityConst.ORDERMANAGEMENT + "')")
    public PageRet getAllMemberOrderCountDetail(OrderQueryParam param) {
        PageRet pageRet = new PageRet();
        PageInfo pageInfo = ordersService.getAllMemberOrderCountDetail(param);
        pageRet.data.setPageInfo(pageInfo);
        pageRet.setResult(BasicRet.SUCCESS);
        pageRet.setMessage("返回成功");
        return pageRet;
    }

    @RequestMapping(value = "/getAllMemberOrderCountPrint", method = RequestMethod.POST)
    @ApiOperation(value = "后台打印订单统计详情")
    @PreAuthorize("hasAuthority('" + AdminAuthorityConst.ORDERMANAGEMENT + "')")
    public PageRet getAllMemberOrderCountPrint(Long[] orderids) {
        PageRet pageRet = new PageRet();
        PageInfo pageInfo = ordersService.getAllMemberOrderCountPrint(orderids);
        pageRet.data.setPageInfo(pageInfo);
        pageRet.setResult(BasicRet.SUCCESS);
        pageRet.setMessage("返回成功");
        return pageRet;
    }

    @RequestMapping(value = "/getAllMemberFutureOrderList", method = RequestMethod.POST)
    @ApiOperation(value = "后台获取远期订单列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "memberName", value = "买家名称", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "sellerName", value = "卖家名称", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "isonline", value = "线上线下订单标识0=线上1=线下", required = false, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "payType", value = "支付方式0=支付宝1=微信2=银行卡3=余额4=授信", required = false, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "orderType", value = "订单类型0=立即发货1=远期全款2=远期定金3=远期余款", required = false, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "orderNo", value = "订单号", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "code", value = "合同号", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "tranNo", value = "交易号", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "clerkname", value = "客服人员", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "waysalesman", value = "介绍人", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "stand", value = "规格", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "startTime", value = "开始时间", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "endTime", value = "结束时间", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "orderState", value = "订单状态0=待付款1=待发货3=待收货4=待验货5=已完成7=已关闭8=备货中9=备货完成11=卖家违约订单", required = false, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "deliverytype", value = "发货模式 0-卖家直发，1-平台代发", required = false, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "presellconfim", value = "卖家确认远期预售：0=卖家未确认该远期订单，1=卖家已确认接收该远期订单，2=卖家已确认不接收该远期订单", required = false, paramType = "query", dataType = "int", defaultValue = "-1"),
            @ApiImplicitParam(name = "prestocktimeStart", value = "卖家预计备货完成时间-开始", required = false, paramType = "query", dataType = "date"),
            @ApiImplicitParam(name = "prestocktimeEnd", value = "卖家预计备货完成时间-结束", required = false, paramType = "query", dataType = "date"),
    })
    @PreAuthorize("hasAuthority('" + AdminAuthorityConst.ORDERMANAGEMENT + "')")
    public PageRet getAllMemberFutureOrderList(Model model, OrderQueryParam param) {
        PageRet pageRet = new PageRet();
        PageInfo pageInfo = ordersService.getAllMemberOrdersList(param);
        pageRet.data.setPageInfo(pageInfo);
        pageRet.setResult(BasicRet.SUCCESS);
        pageRet.setMessage("返回成功");
        return pageRet;
    }

    @RequestMapping(value = "/sendOutGoods", method = RequestMethod.POST)
    @ApiOperation(value = "发货")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "orderno", value = "订单编号", required = true, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "logisticscompany", value = "运输方式", required = true, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "couriernumber", value = "运输单号", required = true, paramType = "query", dataType = "String"),
    })
    @PreAuthorize("hasAuthority('" + AdminAuthorityConst.ORDERMANAGEMENT + "')")
    public BasicRet sendOutGoods(Model model, String orderno, String logisticscompany, String couriernumber, HttpServletRequest request) {

        Admin admin = (Admin) model.asMap().get(AppConstant.ADMIN_SESSION_NAME);

        BasicRet basicRet = new BasicRet();
        Orders orders = ordersService.getOrdersByOrderNo(orderno);
        if (orders != null && orders.getDeliverytype() == Quantity.STATE_1) {
            if (orders.getOrderstatus() == Quantity.STATE_3) {

               /* orders.setLogisticscompany(logisticscompany);
                orders.setCouriernumber(couriernumber);
                orders.setDeliveryno(GenerateNo.getInvoiceNo());

                orders.setSellerdeliverytime(new Date());


                ordersService.updateSingleOrder(orders);*/
               //新增物流信息  要注意 商家发给平台产生一条 平台发给买家产生一条 orderno都是一条。买家端时候 只取最新一条
               LogisticsInfo logisticsInfo = new LogisticsInfo();
               logisticsInfo.setOrderid(orders.getId());
               logisticsInfo.setOrderno(orders.getOrderno());
               logisticsInfo.setLogisticscompany(logisticscompany);
               logisticsInfo.setCouriernumber(couriernumber);
               logisticsInfo.setDeliveryno(GenerateNo.getInvoiceNo());
               logisticsInfo.setTime(new Date());
               logisticsInfoService.insertLogisticsInfo(logisticsInfo);


                //保存操作日志
                OperateLog operateLog = new OperateLog();
                operateLog.setContent("平台将代发商品发货");
                operateLog.setOpid(admin.getId());
                operateLog.setOpname(admin.getUsername());
                operateLog.setOptime(new Date());
                operateLog.setOptype(Quantity.STATE_0);
                operateLog.setOrderid(orders.getId());
                operateLog.setOrderno(orders.getOrderno());
                ordersService.saveOperatelog(operateLog);

                basicRet.setMessage("发货成功");
                basicRet.setResult(BasicRet.SUCCESS);
                //用户日志
                memberLogOperator.saveMemberLog(null, admin, "代发货订单由平台发货完成,订单编号为：" + orderno, "", request, memberOperateLogService);
                return basicRet;
            } else if (orders.getOrderstatus() == Quantity.STATE_7) {
                basicRet.setMessage("买家已取消订单，发货失败");
                basicRet.setResult(BasicRet.ERR);
                return basicRet;
            } else {
                basicRet.setMessage("卖家还未将此订单发货");
                basicRet.setResult(BasicRet.ERR);
                return basicRet;
            }
        } else {
            basicRet.setMessage("没有此订单");
            basicRet.setResult(BasicRet.ERR);
            return basicRet;
        }
    }


    @RequestMapping(value = "/getOrderTotalNum", method = RequestMethod.POST)
    @ApiOperation(value = "后台获取订单总额货款，总销售额，总订货量，总发货量,总佣金数")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "memberName", value = "买家名称", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "sellerName", value = "卖家名称", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "isonline", value = "线上线下订单标识0=线上1=线下", required = false, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "payType", value = "支付方式0=支付宝1=微信2=银行卡3=余额4=授信", required = false, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "orderType", value = "订单类型0=立即发货1=远期全款2=远期定金3=远期余款", required = false, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "orderNo", value = "订单号", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "code", value = "合同号", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "transactionid", value = "支付宝交易号", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "tranNo", value = "交易号", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "clerkname", value = "客服人员", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "waysalesman", value = "介绍人", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "stand", value = "规格", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "startTime", value = "开始时间", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "endTime", value = "结束时间", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "orderState", value = "订单状态0=待付款1=待发货3=待收货4=待验货5=已完成7=已关闭8=备货中9=备货完成11=卖家违约订单", required = false, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "orderStates", value = "订单状态(如果多个状态就以逗号分隔)0=待付款1=待发货3=待收货4=待验货5=已完成7=已关闭8=备货中9=备货完成11=卖家违约订单", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "registerTimeStart", value = "注册开始时间", required = false, paramType = "query", dataType = "date"),
            @ApiImplicitParam(name = "registerTimeEnd", value = "注册结束时间", required = false, paramType = "query", dataType = "date"),
            @ApiImplicitParam(name = "deliverytype", value = "发货方式 0-卖家直发，1-平台代发", required = false, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "shipto", value = "收货人", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "sendstatus", value = "发货状态 全部订单为不传 1为待发货（查询待发货和部分发货订单）3为待收货订单(查待收货和部分发货) 10为部分发货订单(只查10部分发货)", required = false, paramType = "query", dataType = "long"),

    })
    public OrderCarRet getOrderTotalNum(OrderQueryParam param) {
        OrderCarRet orderCarRet = new OrderCarRet();
        Map<String, BigDecimal> map = new HashMap<String, BigDecimal>();
        if(param.getStand()!=null){
            param.setStand(param.getStand().toUpperCase());
        }
        OrderQueryParam q1 = new OrderQueryParam();
        BeanUtils.copyProperties(param, q1);
        BigDecimal ordersSum = ordersService.getOrdersSum(q1);

        OrderQueryParam q2 = new OrderQueryParam();
        BeanUtils.copyProperties(param, q2);
        BigDecimal orderFreight = ordersService.getOrderFreight(q2);

        OrderQueryParam q3 = new OrderQueryParam();
        BeanUtils.copyProperties(param, q3);
        BigDecimal ordersTotalNum = ordersService.getOrdersTotalNum(q3);

        OrderQueryParam q4 = new OrderQueryParam();
        BeanUtils.copyProperties(param, q4);
        BigDecimal ordersTotalDeliveryNum = ordersService.getOrdersTotalDeliveryNum(q4);

        BigDecimal orderSumBroker = ordersService.getOrdersSumBroker(param);

        map.put("ordersSum", ordersSum);
        map.put("orderFreight", orderFreight);
        map.put("ordersTotalNum", ordersTotalNum);
        map.put("ordersTotalDeliveryNum", ordersTotalDeliveryNum);
        map.put("orderSumBroker", orderSumBroker);

        orderCarRet.data.map = map;
        orderCarRet.setMessage("返回成功");
        orderCarRet.setResult(BasicRet.SUCCESS);
        return orderCarRet;
    }


    @RequestMapping(value = "/getOrdersByOrderNoArr", method = RequestMethod.POST)
    @ApiOperation(value = "根据订单编号获取订单[一组订单编号]")
    public OrdersRet getOrdersByOrderNoArr(Long[] orderids) {
        OrdersRet ordersRet = new OrdersRet();

        List<Orders> list = ordersService.getOrdersByIds(orderids);
        for (Orders orders : list) {
            if (orders.getDeliverytype() == 1) {  //如果是代理发货，设置为代理发货地址
                orders.setProvince(AgentDeliveryAddressConst.province);
                orders.setCity(AgentDeliveryAddressConst.city);
                orders.setArea(AgentDeliveryAddressConst.province);
                orders.setReceivingaddress(AgentDeliveryAddressConst.address);
                orders.setShipto(AgentDeliveryAddressConst.linkman);
                orders.setPhone(AgentDeliveryAddressConst.tel);
            }

            List<OrderProduct> orderProductList = orderProductServices.getOrderProductByOrderId(orders.getId(),new Short[]{0,3});

            List<Long> orderproductids = new ArrayList<>();
            orderProductList.stream().forEach(orderProduct -> orderproductids.add(orderProduct.getId()));
            List<OrderProductLog> orderProductLogList = orderProductLogService.getProductinfoByOrderproductids(orderproductids);

            List<OrderProduct> retOrderProdList = new ArrayList<>();
            for (OrderProduct orderProduct : orderProductList) {
                if(orderProduct.getNum().compareTo(Quantity.BIG_DECIMAL_0) <=0) continue;
                List packageList = JinshangUtils.toCovertPacking(StringUtils.nvl(orderProduct.getPddesc()));
                orderProduct.setPackageList(packageList);

                for (OrderProductLog opl : orderProductLogList) {
                    if (opl.getOrderproductid().equals(orderProduct.getId())) {
                        orderProduct.getExtend().put("productinfo", opl.getProductinfojson());
                    }
                }

                retOrderProdList.add(orderProduct);
            }
            orders.setOrderProducts(retOrderProdList);


            BuyerCompanyInfo buyerCompanyInfo = buyerCompanyService.getBuyerCompanyInfoByMemberId(orders.getMemberid());
            Member member = memberService.getMemberById(orders.getMemberid());
            if (buyerCompanyInfo != null) {
                orders.setBuyercompanyname(buyerCompanyInfo.getCompanyname());
            }

            orders.setBuyerRealname(member.getRealname());
        }

        ordersRet.getData().setOrdersList(list);
        ordersRet.setResult(BasicRet.SUCCESS);
        ordersRet.setMessage("查询成功");

        return ordersRet;
    }



    @RequestMapping(value = "/printSendGoods", method = RequestMethod.POST)
    @ApiOperation(value = "批量打印发货单[一组订单编号]")
    public OrdersRet printSendGoods(Long[] orderids) {
        OrdersRet ordersRet = new OrdersRet();
        BigDecimal ordersPrice=Quantity.BIG_DECIMAL_0;
        List<Orders> list = ordersService.getOrdersByIds(orderids);
        for (Orders orders : list) {
            if (orders.getDeliverytype() == 1) {  //如果是代理发货，设置为代理发货地址
                orders.setProvince(AgentDeliveryAddressConst.province);
                orders.setCity(AgentDeliveryAddressConst.city);
                orders.setArea(AgentDeliveryAddressConst.province);
                orders.setReceivingaddress(AgentDeliveryAddressConst.address);
                orders.setShipto(AgentDeliveryAddressConst.linkman);
                orders.setPhone(AgentDeliveryAddressConst.tel);
            }

            List<OrderProduct> orderProductList = orderProductServices.getByOrderid(orders.getId());

            List<Long> orderproductids = new ArrayList<>();
            orderProductList.stream().forEach(orderProduct -> orderproductids.add(orderProduct.getId()));
            List<OrderProductLog> orderProductLogList = orderProductLogService.getProductinfoByOrderproductids(orderproductids);

            List<OrderProduct> retOrderProdList = new ArrayList<>();
            for (OrderProduct orderProduct : orderProductServices.margeOrderProduct(orderProductList)) {
                if(orderProduct.getNum().compareTo(Quantity.BIG_DECIMAL_0) <=0) continue;
                List packageList = JinshangUtils.toCovertPacking(StringUtils.nvl(orderProduct.getPddesc()));
                orderProduct.setPackageList(packageList);

                for (OrderProductLog opl : orderProductLogList) {
                    if (opl.getOrderproductid().equals(orderProduct.getId())) {
                        orderProduct.getExtend().put("productinfo", opl.getProductinfojson());
                    }
                }
                ordersPrice=ordersPrice.add(orderProduct.getActualpayment()).add(orderProduct.getDiscountpay());
                DecimalFormat decimalFormat=new DecimalFormat("#.00");
//                BigDecimal actualpayment=new BigDecimal(orderProduct.getNum().multiply(orderProduct.getPrice()).toString()).setScale(2,BigDecimal.ROUND_HALF_UP);
                BigDecimal actualpayment=orderProduct.getActualpayment().add(orderProduct.getDiscountpay());
                orderProduct.setActualpayment(new BigDecimal(decimalFormat.format(actualpayment)));
                retOrderProdList.add(orderProduct);
            }


            orders.setOrderProducts(retOrderProdList);
            orders.setTotalprice(ordersPrice);
            BuyerCompanyInfo buyerCompanyInfo = buyerCompanyService.getBuyerCompanyInfoByMemberId(orders.getMemberid());
            Member member = memberService.getMemberById(orders.getMemberid());
            if (buyerCompanyInfo != null) {
                orders.setBuyercompanyname(buyerCompanyInfo.getCompanyname());
            }

            orders.setBuyerRealname(member.getRealname());
        }

        ordersRet.getData().setOrdersList(list);
        ordersRet.setResult(BasicRet.SUCCESS);
        ordersRet.setMessage("查询成功");

        return ordersRet;
    }


    @RequestMapping(value = "/getOrdersByOrderNo", method = RequestMethod.POST)
    @ApiOperation(value = "根据订单编号获取订单")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "orderno", value = "订单编号", required = true, paramType = "query", dataType = "string"),
    })
    @PreAuthorize("hasAuthority('" + AdminAuthorityConst.ORDERMANAGEMENT + "')")
    public OrderCarRet getOrdersByOrderNo(String orderno) throws IOException {
        OrderCarRet orderCarRet = new OrderCarRet();
        orderCarRet.setMessage("返回成功");
        orderCarRet.setResult(BasicRet.SUCCESS);
        Orders orders = ordersService.getOrdersByOrderNo(orderno);
        //取消时间：2018年6月2日10:08:44 原因:直接添加到订单保存数据库了
//        AdminUser AdminUser = adminUserService.getAdminUserByUserid(orders.getMemberid());
//        if (AdminUser != null) {
//            Admin admin = adminService.getById(AdminUser.getAdminid());
//            orders.setClerkname(admin.getRealname());
//            orders.setClerknamePhone(admin.getMobile());
//        }
        List<OrderProduct> productList = orderProductServices.getByOrderNo(orderno);
        orderCarRet.data.orderProducts = productList;
        orderCarRet.data.orders = orders;



        //根据订单号去logisticsinfo表查询物流
        List<LogisticsInfoDto> logisticsInfoList = logisticsInfoService.selectByOrderNo(orderno);

        String url = "";
        if(logisticsInfoList!=null && logisticsInfoList.size()>0) {
            setExpressurl(logisticsInfoList);
        }
        //app端接口为了兼容是可以使用下面那条 只取最新的一条物流信息
        //orderCarRet.data.expressurl = url;
        orderCarRet.data.logisticsInfos = logisticsInfoList;
        return orderCarRet;
    }

    private void setExpressurl(List<LogisticsInfoDto> logisticsInfoList) {
        String url;
        for (LogisticsInfoDto logisticsInfoDto : logisticsInfoList) {
            if (StringUtils.hasText(logisticsInfoDto.getLogisticscompany()) && StringUtils.hasText(logisticsInfoDto.getCouriernumber())) {
                List<String> list = commonDataValueService.getcommonDataValue("物流公司");
                for (String vl : list) {
                    String[] vlStr = vl.split("-");
                    if(logisticsInfoDto != null) {
                        if (vlStr[0].equals(logisticsInfoDto.getLogisticscompany())) {
                            //物流查询
                            url = ExpressUtils.searchkuaiDiInfo(vlStr[1], logisticsInfoDto.getCouriernumber());
                            logisticsInfoDto.setExpressurl(url);
                            break;
                         }
                    }else{
                        logisticsInfoDto.setExpressurl("");
                    }
                }
            }
        }
    }


    @RequestMapping(value = "/getOrdersByOrderNoWithBuyerinfo", method = RequestMethod.POST)
    @ApiOperation(value = "根据订单编号获取订单-包含买家和买家公司信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "orderno", value = "订单编号", required = true, paramType = "query", dataType = "string"),
    })
    @PreAuthorize("hasAuthority('" + AdminAuthorityConst.ORDERMANAGEMENT + "')")
    public OrdersBuyerRet getOrdersByOrderNoWithBuyerinfo(String orderno) {
        OrdersBuyerRet orderCarRet = new OrdersBuyerRet();

        Orders orders = ordersService.getOrdersByOrderNo(orderno);

// //取消时间：2018年6月2日10:08:44 原因:直接添加到订单保存数据库了
//        AdminUser AdminUser = adminUserService.getAdminUserByUserid(orders.getMemberid());
//        if (AdminUser != null) {
//            Admin admin = adminService.getById(AdminUser.getAdminid());
//            orders.setClerkname(admin.getRealname());
//            orders.setClerknamePhone(admin.getMobile());
//        }
        orderCarRet.data.orders = orders;

        if (orders != null) {
            Member member = memberService.getMemberById(orders.getMemberid());
            BuyerCompanyInfo buyerCompanyInfo = buyerCompanyService.getBuyerCompanyInfoByMemberId(orders.getMemberid());

            orderCarRet.data.buyMember = member;
            orderCarRet.data.buyerCompanyInfo = buyerCompanyInfo;
        }

        orderCarRet.setMessage("返回成功");
        orderCarRet.setResult(BasicRet.SUCCESS);
        return orderCarRet;
    }


    private class OrdersBuyerRet extends BasicRet {
        private class OrderData {
            private Orders orders;
            private Member buyMember;
            private BuyerCompanyInfo buyerCompanyInfo;

            public Orders getOrders() {
                return orders;
            }

            public void setOrders(Orders orders) {
                this.orders = orders;
            }

            public Member getBuyMember() {
                return buyMember;
            }

            public void setBuyMember(Member buyMember) {
                this.buyMember = buyMember;
            }

            public BuyerCompanyInfo getBuyerCompanyInfo() {
                return buyerCompanyInfo;
            }

            public void setBuyerCompanyInfo(BuyerCompanyInfo buyerCompanyInfo) {
                this.buyerCompanyInfo = buyerCompanyInfo;
            }
        }


        private OrderData data = new OrderData();

        public OrderData getData() {
            return data;
        }

        public void setData(OrderData data) {
            this.data = data;
        }
    }


    @RequestMapping(value = "/getOrderProductByOrderNo", method = RequestMethod.POST)
    @ApiOperation(value = "根据订单编号获取商品信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "orderno", value = "订单编号", required = true, paramType = "query", dataType = "string"),
    })
    @PreAuthorize("hasAuthority('" + AdminAuthorityConst.ORDERMANAGEMENT + "')")
    public OrderCarRet getOrderProductByOrderNo(String orderno) {
        OrderCarRet orderCarRet = new OrderCarRet();
        orderCarRet.setMessage("返回成功");
        orderCarRet.setResult(BasicRet.SUCCESS);
        List<OrderProduct> productList=ordersService.getOrderProductByOrderNo(orderno);;
        for (OrderProduct orderProduct:productList) {
            DecimalFormat decimalFormat=new DecimalFormat("#.00");
            BigDecimal actualpayment=new BigDecimal(orderProduct.getNum().multiply(orderProduct.getPrice()).toString()).setScale(2,BigDecimal.ROUND_HALF_UP);
            orderProduct.setActualpayforcontract(new BigDecimal(decimalFormat.format(actualpayment)));
        }
        orderCarRet.data.orderProducts = productList;
        orderCarRet.data.orderProducts.forEach(orderProduct -> orderProduct.getExtend().put("productInfo", productInfoService.getById(orderProduct.getPdid())));
        return orderCarRet;
    }

    @RequestMapping(value = "/getOrderProductById", method = RequestMethod.POST)
    @ApiOperation(value = "根据商品订章id获取商品信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "订单商品id", required = true, paramType = "query", dataType = "long"),
    })
    public OrderCarRet getOrderProductById(long id) {
        OrderCarRet orderCarRet = new OrderCarRet();
        orderCarRet.setMessage("返回成功");
        orderCarRet.setResult(BasicRet.SUCCESS);
        OrderProduct orderProduct = ordersService.getOrderProductById(id);
        ProductInfo info = ordersService.getProductInfoByPrimeKey(orderProduct.getPdid());
        orderCarRet.data.orderProduct = orderProduct;
        orderCarRet.data.selfsupport = info.getSelfsupport();
        return orderCarRet;
    }

    @RequestMapping(value = "/getBillingRecordByOrderNo", method = RequestMethod.POST)
    @ApiOperation(value = "根据订单编号获取开票信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "orderid", value = "订单id", required = true, paramType = "query", dataType = "string"),
    })
    public OrderCarRet getBillingRecordByOrderNo(String orderid) {
        OrderCarRet orderCarRet = new OrderCarRet();
        orderCarRet.setMessage("返回成功");
        orderCarRet.setResult(BasicRet.SUCCESS);
        orderCarRet.data.billingRecord = ordersService.getBillingRecordByOrderNo(orderid);
        return orderCarRet;
    }

    @RequestMapping(value = "/getOrderProductBack", method = RequestMethod.POST)
    @ApiOperation(value = "获取退货列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "startTime", value = "开始时间", required = false, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "endTime", value = "结束时间", required = false, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "code", value = "交易号", required = false, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "pdName", value = "商品名称", required = false, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "memberName", value = "买家", required = false, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "sellerName", value = "卖家", required = false, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "orderNo", value = "订单号", required = false, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "backNo", value = "退货号", required = false, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "state", value = "退货状态0=待卖家处理1=卖家同意待收货2=卖家同意直接退款3=卖家收到货同意退款4=卖家不同意5=买家同意验货6=买家申请异议7=平台同意退货不扣违约金8=平台同意退货扣违约金9=平台转入待验收10=退货成功", required = true, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "pageNo", value = "开始页", required = true, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "pageSize", value = "页数", required = true, paramType = "query", dataType = "int"),
    })
    @PreAuthorize("hasAuthority('" + AdminAuthorityConst.RETURNMANAGEMENT + "')")
    public PageRet getOrderProductBack(BackQueryParam backQueryParam, int pageNo, int pageSize) {
        PageRet pageRet = new PageRet();
        pageRet.setMessage("返回成功");
        pageRet.setResult(BasicRet.SUCCESS);

        PageInfo<OrderProductBackView> pageInfo = ordersService.getOrderProductBackList(pageNo, pageSize, backQueryParam);


        pageRet.data.setPageInfo(pageInfo);
        return pageRet;
    }

    @RequestMapping(value = "/listShippingAddress", method = RequestMethod.POST)
    @ApiOperation("列出卖家收货地址列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "sellerid", value = "卖家id", required = true, paramType = "query", dataType = "int"),
    })
    public PageRet listSellerShippingAddress(@RequestParam(required = true, defaultValue = "1") int pageNo,
                                             @RequestParam(required = true, defaultValue = "10") int pageSize, Model model, Long sellerid) {
        PageRet pageRet = new PageRet();
        PageInfo pageInfo = shippingAddressService.listAllShippingAddress(pageNo, pageSize, sellerid, Quantity.STATE_1);
        pageRet.data.setPageInfo(pageInfo);
        pageRet.setResult(BasicRet.SUCCESS);
        return pageRet;
    }


    @RequestMapping(value = "/getOrderProductBackByOrderProductId", method = RequestMethod.POST)
    @ApiOperation(value = "根据商品id获取退货详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "订单商品id", required = true, paramType = "query", dataType = "long"),
    })
    public BasicRet getOrderProductBackByOrderProductId(Long id) {
        OrderCarRet orderCarRet = new OrderCarRet();
        OrderProductBack orderProductBack = ordersService.getOrderProductBackByOrderProductID(id);

        if (orderProductBack != null) {
//              orderProductBack.setActualpayment( ordersService.getAllpayByOrderNoAndPdidAndPdNo(orderProductBack.getOrderno(),orderProductBack.getPdid(),orderProductBack.getPdno()));

            List<OrderProduct> orderProductList = ordersService.getOrderProdByOrderNoAndPdidAndPdNo(orderProductBack.getOrderno(), orderProductBack.getPdid(), orderProductBack.getPdno());
            BigDecimal actualpayment = new BigDecimal(0);
            for (OrderProduct op : orderProductList) {
                actualpayment = op.getActualpayment().subtract(op.getFreight()).add(actualpayment);
            }
            orderProductBack.setActualpayment(actualpayment);
        }

        //根据退货原因 查询违约金比例
        ReturnReason returnReason = returnReasonService.selectByReturnReason(orderProductBack.getReturnbackreason());
        if(returnReason !=null) {
            BigDecimal penalty = returnReason.getPenalty();
            orderCarRet.data.penalty = penalty;
            orderCarRet.data.returnreason = returnReason.getReturnreason();
        }

        orderCarRet.data.orderProductBack = orderProductBack;
        String url = "";
        if (StringUtils.hasText(orderProductBack.getLogisticscompany()) && StringUtils.hasText(orderProductBack.getLogisticsno())) {
            List<String> lists = commonDataValueService.getcommonDataValue("物流公司");
            for (String vl : lists) {
                String[] vlStr = vl.split("-");
                if (orderProductBack.getLogisticscompany().equals(vlStr[0])) {
                    //物流查询
                    url = ExpressUtils.searchkuaiDiInfo(vlStr[1], orderProductBack.getLogisticsno());
                    break;
                }
            }
        }
        orderCarRet.data.expressurl = url;
        orderCarRet.setMessage("返回成功");
        orderCarRet.setResult(BasicRet.SUCCESS);
        return orderCarRet;
    }


    @RequestMapping(value = "/updateOrderProductBack", method = RequestMethod.POST)
    @ApiOperation(value = "修改退货申请")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "退货申请id", required = true, paramType = "query", dataType = "long"),
            @ApiImplicitParam(name = "state", value = "退货状态0=待卖家处理1=卖家同意待收货2=卖家同意直接退款3=卖家收到货同意退款4=卖家不同意5=买家同意验货6=买家申请异议7=平台同意退货不扣违约金8=平台同意退货扣违约金9=平台转入待验收10=退货成功11=撤消", required = false, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "logisticsno", value = "退货单号", required = false, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "logisticscompany", value = "退货物流公司", required = false, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "backaddress", value = "退货地址", required = false, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "contact", value = "退货联系人", required = false, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "contactphone", value = "退货联系人电话", required = false, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "sellernotagree", value = "卖家不同意原因", required = false, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "adminreason", value = "平台处理说明", required = false, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "adminstate", value = "平台处理意见0=正常1=不扣违约金2=扣违约金3=转入待验收", required = false, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "province", value = "省", required = false, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "city", value = "市", required = false, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "area", value = "区", required = false, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "backtype", value = "退货类型0=仅退款1=退货退款2=部分退货", required = true, paramType = "query", dataType = "int"),
    })
    @PreAuthorize("hasAuthority('" + AdminAuthorityConst.RETURNMANAGEMENT + "')")
    public BasicRet updateOrderProductBack(Model model, ProductBackModel productBackModel, HttpServletRequest request) throws CashException {

        Admin admin = (Admin) model.asMap().get(AppConstant.ADMIN_SESSION_NAME);

        BasicRet basicRet = new BasicRet();

        Short state = productBackModel.getState();

        if (state != Quantity.STATE_7 && state != Quantity.STATE_8 && state != Quantity.STATE_9) {
            basicRet.setMessage("操作状态不合法");
            basicRet.setResult(BasicRet.ERR);
            return basicRet;
        }


        productBackModel.setState(null);
        productBackModel.setPic(null);
        productBackModel.setBackexplain(null);
        OrderProductBack orderProductBack = ordersService.getBackgoodsOrderProductBack(productBackModel);
        if (orderProductBack == null) {
            return new BasicRet(BasicRet.ERR, "未查询到退货商品信息");
        }


        if (orderProductBack.getState() == Quantity.STATE_11) {
            basicRet.setMessage("退货已撤消");
            basicRet.setResult(BasicRet.ERR);
            return basicRet;
        } else if (orderProductBack.getState() == Quantity.STATE_10) {
            basicRet.setMessage("退货已完成");
            basicRet.setResult(BasicRet.ERR);
            return basicRet;
        } else {
            orderProductBack.setState(state);
            ProductStore store = null;
            LimitTimeStore limitTimeStore = null;
            LimitTimeProd limitTimeProd = null;
            Orders orders = ordersService.getOrdersById(orderProductBack.getOrderid());
            if (orders.getIsonline() == Quantity.STATE_0) {
                store = ordersService.getProductStore(orderProductBack.getPdid(), orderProductBack.getPdno(), orderProductBack.getStoreid());
            }
            if (orders.getIsonline() == Quantity.STATE_2) {
                limitTimeStore = shopCarService.getLimitTimeStore(orderProductBack.getStoreid());
                limitTimeProd = shopCarService.getLimitTimeProd(orderProductBack.getPdid(), orderProductBack.getLimitid());
            }

            if (orderProductBack != null) {
                OrderProduct orderProduct = ordersService.getOrderProductById(orderProductBack.getOrderpdid());
                List<OrderProduct> list1 = ordersService.getOrderProductByOrderId(orderProductBack.getOrderid());
                if (state != null) {
                    if (state == Quantity.STATE_7) {
                        //退货验收
                        orderProduct.setBackstate(Quantity.STATE_2);

                        //平台同意退货扣违约金
                    } else if (state == Quantity.STATE_8) {
                        //退货验收
                        orderProduct.setBackstate(Quantity.STATE_2);

                        //平台转入待验收
                    } else if (state == Quantity.STATE_9) {
                        //部分退货
                        if (orderProductBack.getBacktype() == Quantity.STATE_2) {
                            for (OrderProduct op : list1) {
                                //找出部分退货相同的商品，删除这个部分退货，并加数量和总价到原来的商品
                                if (op.getPdid().longValue() == orderProductBack.getPdid().longValue() && op.getPdno().equals(orderProductBack.getPdno()) && op.getBackstate() == Quantity.STATE_0) {
                                    op.setNum(op.getNum().add(orderProduct.getNum()));
                                    op.setActualpayment(new BigDecimal(op.getNum().multiply(op.getPrice()).add(op.getFreight()).toString()).setScale(2,BigDecimal.ROUND_HALF_UP).subtract(op.getDiscountpay()).subtract(orderProduct.getDiscountpay()));
                                    op.setDiscountpay(op.getDiscountpay().add(orderProduct.getDiscountpay()));
                                    ordersService.updateOrderProduct(op);
                                    ordersService.deleteOrderProduct(orderProduct.getId());
                                    break;
                                    //如果部分退货全部退掉，找不到原来的商品，就更新状态
                                } else {
                                    orderProduct.setBackstate(Quantity.STATE_0);
                                }
                            }
                        } else {
                            orderProduct.setBackstate(Quantity.STATE_0);
                        }
                    }

                    ordersService.updateOrderProduct(orderProduct);
                    ordersService.updateOrderProductBack(orderProductBack);

                    List<OrderProduct> list = ordersService.getOrderProductByOrderId(orderProductBack.getOrderid(), orderProductBack.getOrderpdid());

                    boolean flag = true;
                    for (OrderProduct op : list) {
                        if (op.getBackstate() != Quantity.STATE_3) {
                            flag = false;
                            break;
                        }
                    }
                    //判断订单中商品是否都退货完成，就结束订单
                    if (orderProduct.getBackstate() == Quantity.STATE_3 && flag) {
                        //删除开票申请记录
                        ordersService.deleteBillRecord(orders.getOrderno());
                        orders.setOrderstatus(Quantity.STATE_7);
                        ordersService.updateSingleOrder(orders);
                    }
                }
            } else {
                basicRet.setMessage("没有退货申请记录");
                basicRet.setResult(BasicRet.ERR);
                return basicRet;
            }

            //保存操作日志
            OperateLog operateLog = new OperateLog();
            //货状态0=待卖家处理1=卖家同意待收货2=卖家同意直接退款3=卖家收到货同意退款4=卖家不同意5=买家同意验货6=买家申请异议7=平台同意退货不扣违约金8=平台同意退货扣违约金9=平台转入待验收10=退货成功11=撤消
            operateLog.setOpid(admin.getId());
            operateLog.setOpname(admin.getUsername());
            operateLog.setOptime(new Date());
            operateLog.setOptype(Quantity.STATE_1);
            operateLog.setOrderid(orderProductBack.getOrderid());
            operateLog.setOrderno(orderProductBack.getOrderno());
            operateLog.setOrderpdid(orderProductBack.getOrderpdid());


            operateLog.setContent(JinshangUtils.orderProductBackStateMess(orderProductBack.getState()));
            ordersService.saveOperatelog(operateLog);


            //保存用户日志
            memberLogOperator.saveMemberLog(null, admin, "修改退货申请，退货申请id为：" + productBackModel.getId(), "/rest/admin/orders/updateOrderProductBack", request, memberOperateLogService);
            basicRet.setMessage("修改成功");
            basicRet.setResult(BasicRet.SUCCESS);
            return basicRet;
        }
    }




    @RequestMapping(value = "/updateOrderProductNum", method = RequestMethod.POST)
    @ApiOperation(value = "修改订单商品数量")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "orderno", value = "退货单号", required = true, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "orderProductJson", value = "订单产品json [{\"id\":1909,\"num\":0.10}]", required = true, paramType = "query", dataType = "String"),
    })
    public BasicRet updateOrderProductNum(@RequestParam(required = true) String orderno,
                                          @RequestParam(required = true) String orderProductJson, Model model) throws WxPayException, AlipayApiException, CashException, MyException {

        Admin admin = (Admin) model.asMap().get(AppConstant.ADMIN_SESSION_NAME);

        /*//用来记录orderproduct表中还没有更新时购买商品的数量
        List<Map<String,Object>>pdnumList = new ArrayList<Map<String,Object>>();*/

        Orders orders = ordersService.getOrdersByOrderNo(orderno);
        Short isticket=orders.getIsticket();
        //运费
        BigDecimal orderFreight = orders.getFreight();
        //订单总价
        BigDecimal orderTotalprice = orders.getActualpayment();
        //实付款
        //BigDecimal orderActualpayment = orders.getActualpayment();

        BigDecimal discountpay=Quantity.BIG_DECIMAL_0;
        BigDecimal totalDiscountpay=Quantity.BIG_DECIMAL_0;

        if (orders == null) {
            return new BasicRet(BasicRet.ERR, "订单不存在");
        }

        if (orders.getOrderstatus() != Quantity.STATE_1) { //未发货订单可以修改商品数量
            return new BasicRet(BasicRet.ERR, "只有未发货订单可以修改商品数量");
        }

        if (orders.getIsonline() == Quantity.STATE_2) {
            return new BasicRet(BasicRet.ERR, "限时购订单不可修改");
        }

        if (orders.getOrdertype() != Quantity.STATE_0) {
            return new BasicRet(BasicRet.ERR, "远期订单不可修改");
        }

        List<OrderProduct> updateOrderProductList = GsonUtils.toList(orderProductJson, OrderProduct.class);
        List<OrderProduct> orderProductList = ordersService.getOrderProductByOrderId(orders.getId());

        Set<Long> orderprodidSet = new HashSet<>();


        if (updateOrderProductList.size() != orderProductList.size()) {
            return new BasicRet(BasicRet.ERR, "提交订单商品的数量与实际数量不符");
        }

        for (OrderProduct orderProduct : orderProductList) {
            for (OrderProduct updateP : updateOrderProductList) {
                if (orderProduct.getId().equals(updateP.getId())) {
                    orderprodidSet.add(orderProduct.getId());
                }
            }
        }

        if (orderProductList.size() != orderprodidSet.size()) {
            return new BasicRet(BasicRet.ERR, "提交订单商品与实际商品不对应");
        }

        BigDecimal totalProductMoney = Quantity.BIG_DECIMAL_0;  //所有商品总金额
        BigDecimal totalWeight = Quantity.BIG_DECIMAL_0;  //所有商品总重量
        BigDecimal totalNum = Quantity.BIG_DECIMAL_0; //所有数量

        BigDecimal oldTotalProductMoney = Quantity.BIG_DECIMAL_0;  //所有商品总金额 (没有修改数量和运费前)
        BigDecimal oldTotalWeight = Quantity.BIG_DECIMAL_0;  //所有商品总重量(没有修改数量和运费前)




        List<OrderProductModel> saveOrderProductList = new ArrayList<>();

        for (OrderProduct orderProduct : orderProductList) {
            for (OrderProduct updateP : updateOrderProductList) {
                if (orderProduct.getId().equals(updateP.getId())) {
                    if (orderProduct.getNum().compareTo(updateP.getNum()) < 0) {
                        throw new RuntimeException(orderProduct.getPdname() + "要修改的数量不可比原订单数量多");
                    }

                    if (orderProduct.getProtype() != Quantity.STATE_0) {
                        throw new RuntimeException(orderProduct.getPdname() + "为远期订单商品，远期订单不可修改");
                    }

                    ProductInfo productInfo = ordersService.getProductInfoByPrimeKey(orderProduct.getPdid());

                    if (productInfo == null) {
                        throw new MyException("商品id为" + orderProduct.getPdid() + "的商品不存在");
                    }

                    ProductStore productStore1 = ordersService.getProductStore(orderProduct.getPdid(), orderProduct.getPdno(), orderProduct.getStoreid());

                    if (productStore1 == null) {
                        throw new MyException("商品id为" + orderProduct.getPdid() + "的库存信息不存在");
                    }
                    discountpay=orderProduct.getDiscountpay()==null?Quantity.BIG_DECIMAL_0:orderProduct.getDiscountpay();
                    OrderProductModel saveOrderProduct = new OrderProductModel();
                    saveOrderProduct.setId(updateP.getId());
                    saveOrderProduct.setOldProductNum(orderProduct.getNum());

                    if (updateP.getNum().compareTo(Quantity.BIG_DECIMAL_0) == 0) { //全部不发了
                        saveOrderProduct.setNum(updateP.getNum());
                        saveOrderProduct.setPrice(orderProduct.getPrice());
                        saveOrderProduct.setFreight(Quantity.BIG_DECIMAL_0);
                        saveOrderProduct.setActualpayment(Quantity.BIG_DECIMAL_0);
                        saveOrderProduct.setDiscountpay(discountpay);
                        totalNum = totalNum.add(updateP.getNum());
                    } else {
                        saveOrderProduct.setNum(updateP.getNum());
                        saveOrderProduct.setPrice(orderProduct.getPrice());


                        BigDecimal oldActualpayment = orderProduct.getActualpayment();
                        //退货金额计算公式:退货数量x单价-(退货数量/总数量)X产品的优惠金额
                        BigDecimal pdbackNum=orderProduct.getNum().subtract(updateP.getNum());
                        //subtractMoney是退款金额
//                        BigDecimal subtractMoney=new BigDecimal(pdbackNum.multiply(orderProduct.getPrice()).subtract(pdbackNum.divide(orderProduct.getNum(),5,BigDecimal.ROUND_HALF_UP).multiply(discountpay)).toString()).setScale(2,BigDecimal.ROUND_HALF_UP);
                        BigDecimal subtractMoney=new BigDecimal(oldActualpayment.multiply(pdbackNum).divide(orderProduct.getNum(),5,BigDecimal.ROUND_HALF_UP).toString()).setScale(2,BigDecimal.ROUND_HALF_UP);
                        saveOrderProduct.setActualpayment(new BigDecimal(oldActualpayment.subtract(subtractMoney).toString()).setScale(2,BigDecimal.ROUND_HALF_UP));
                        //重新核算优惠金额,担心采用 (优惠金额/订单数量)*剩余数量 的方式会导致精度丢失，故直接相减
//                        discountpay=new BigDecimal(saveOrderProduct.getNum().multiply(orderProduct.getPrice()).subtract(saveOrderProduct.getActualpayment()).toString()).setScale(2,BigDecimal.ROUND_HALF_UP);
                        discountpay=new BigDecimal(saveOrderProduct.getNum().multiply(discountpay).divide(orderProduct.getNum(),5,BigDecimal.ROUND_HALF_UP).toString()).setScale(2,BigDecimal.ROUND_HALF_UP);
                        saveOrderProduct.setDiscountpay(discountpay);
                        totalProductMoney = totalProductMoney.add(saveOrderProduct.getActualpayment());
                        totalWeight = totalWeight.add(updateP.getNum().multiply(productStore1.getWeight()));
                        totalNum = totalNum.add(updateP.getNum());

                        //TODO  兼容老数据
                        if(orders.getOrderfright() == null) {
                            //计算运费
                            BigDecimal figtht = BigDecimal.valueOf(0);
                            if (orders.getIsonline() == Quantity.STATE_0) {
                                figtht = freightService.getFreight(productStore1.getFreightmode(), productStore1.getWeight().multiply(saveOrderProduct.getNum()), orders.getProvince(), orders.getCity());
                                saveOrderProduct.setFreight(figtht);
                            } else if (orders.getIsonline() == Quantity.STATE_2) {
                                saveOrderProduct.setFreight(BigDecimal.valueOf(0));
                            }
                        }
                    }

                    totalDiscountpay=totalDiscountpay.add(discountpay);
                    saveOrderProductList.add(saveOrderProduct);

                    if (ordersService.updateOrderProductForModifyProductnum(saveOrderProduct) != 1) {
                        throw new MyException("修改订单商品id:" + saveOrderProduct.getId() + "失败，请联系网站管理员");
                    }

                    //将退货的商品信息记录到orderproductbackinfo表中
                    int a = orderProduct.getNum().compareTo(saveOrderProduct.getNum());
                    if (a != 0) {
                        OrderProductBackInfo orderProductBackInfo = new OrderProductBackInfo();
                        orderProductBackInfo.setOrderno(orderProduct.getOrderno());
                        orderProductBackInfo.setPdid(orderProduct.getPdid());
                        orderProductBackInfo.setBackno("TH" + UUID.randomUUID());
                        orderProductBackInfo.setBacknum(orderProduct.getNum().subtract(saveOrderProduct.getNum()));
                        orderProductBackInfo.setBacktype(Quantity.STATE_2);
                        orderProductBackInfo.setBackstate(Quantity.STATE_0);
                        orderProductBackInfo.setBacktime(new Date());
                        orderProductBackInfoService.addOrderProductBackInfo(orderProductBackInfo);
                    }

                }
            }

            //计算商家没有修改数量和运费之前的 用于退回运费差额的计算。
            if(orders.getIsmodifyfreight() == Quantity.STATE_1){
                ProductStore oldProductStore = productStoreService.getProductStore(orderProduct.getPdid(), orderProduct.getPdno(), orders.getStoreid());
                oldTotalProductMoney = oldTotalProductMoney.add(orderProduct.getActualpayment());
                oldTotalWeight = oldTotalWeight.add(orderProduct.getNum().multiply(oldProductStore.getWeight()));
            }
        }


        /*
        BigDecimal totalFreight = Quantity.BIG_DECIMAL_0;
        BigDecimal totalPrice = Quantity.BIG_DECIMAL_0;
        for (OrderProduct op : saveOrderProductList) {
            totalFreight = totalFreight.add(op.getFreight());
            totalPrice = totalPrice.add(op.getActualpayment());
        }*/



        //totalFreight.setScale(2, BigDecimal.ROUND_HALF_UP);
        //totalPrice.setScale(2, BigDecimal.ROUND_HALF_UP);

        //计算修改后需要交纳的运费金额
        BigDecimal updateFreightMoney = Quantity.BIG_DECIMAL_0;

        //定义一个变量 用于存放修改商品订购数量后的运费（根据运费模板算出来的） 用于退回运费差额的计算。
        BigDecimal updateNumBeforeFreightMoney = Quantity.BIG_DECIMAL_0;


       if(orders.getOrderfright() != null) {
           if (orders.getOrderfright() != 1 && orders.getOrderfright() != 2 && orders.getOrderfright() != 3 && totalWeight.compareTo(Quantity.BIG_DECIMAL_0) == 1) {
               OrderFrightDto orderFrightDto = GsonUtils.toBean(orders.getFrighttemplate(), OrderFrightDto.class);
               updateFreightMoney = freightService.getFreightByOrderFrightDto(orderFrightDto, totalProductMoney.add(totalDiscountpay), totalWeight, orders.getProvince(), orders.getCity());
               updateNumBeforeFreightMoney = updateFreightMoney;
           } else {
               updateFreightMoney = Quantity.BIG_DECIMAL_0;
               updateNumBeforeFreightMoney = updateFreightMoney;
           }
       }else{
           //TODO 兼容老数据
           for (OrderProduct op : saveOrderProductList) {
               updateFreightMoney = updateFreightMoney.add(op.getFreight());
//               totalPrice = totalPrice.add(op.getActualpayment());
           }
       }


        //未修改运费的退运费金额 = 修改商品订购数量前的运费 - 修改商品订购数量后的运费
        //退回的运费 = （未修改运费的退运费金额 / 未修改的运费金额） * 修改后的运费金额。
        BigDecimal oldFreightMoney  = Quantity.BIG_DECIMAL_0; // 修改商品订购数量前的根据模板计算的运费
        BigDecimal compareFreightMoney = Quantity.BIG_DECIMAL_0; //未修改运费的退运费金额
        BigDecimal backFreightMoney = Quantity.BIG_DECIMAL_0; //退回的运费

        if(orders.getOrderfright() != 1 && orders.getOrderfright() != 2 && orders.getOrderfright() != 3) {
            oldFreightMoney = getOldFreightMoney(orders, oldTotalProductMoney, oldTotalWeight);
        }

        //如果有改过运费 都以改过运费的为准,但不包含修改数量为0的
        if(orders.getIsmodifyfreight() == 1 && totalNum.compareTo(new BigDecimal(0))>0){
            updateFreightMoney = orders.getFreight();
        }

        BigDecimal updateTotalMoney = BigDecimal.ZERO;

        //修改过运费的 商品总价+(修改后总运费-退回的运费)
        if(orders.getIsmodifyfreight() == 1 && orders.getFreight().compareTo(new BigDecimal(0))>0 && oldFreightMoney.compareTo(new BigDecimal(0))>0 && orders.getOrderfright() != 1 && orders.getOrderfright() != 2 && orders.getOrderfright() != 3) {
            //计算出来的运费差额
            backFreightMoney = getBackFreightMoney(orders, oldTotalProductMoney, oldTotalWeight, updateFreightMoney, updateNumBeforeFreightMoney);
            updateTotalMoney = new BigDecimal(totalProductMoney.add(updateFreightMoney.subtract(backFreightMoney)).toString()).setScale(2, BigDecimal.ROUND_HALF_UP); //修改后的订单的总金额（货款+运费）
        }else{
            //没有修改过运费的 商品总价+运费
            updateTotalMoney = new BigDecimal(totalProductMoney.add(updateFreightMoney).toString()).setScale(2, BigDecimal.ROUND_HALF_UP);  //修改后的订单的总金额（货款+运费）
        }

        //单个或者多个商品全部数量修改为0的情况
        if(orders.getIsmodifyfreight() == 1 && totalNum.compareTo(new BigDecimal(0))==0){
            updateTotalMoney = new BigDecimal(totalProductMoney.add(new BigDecimal(0)).toString()).setScale(2,BigDecimal.ROUND_HALF_UP);  //修改后的订单的总金额（货款+运费）
        }

        //退款金额
        BigDecimal backMoney = new BigDecimal(orderTotalprice.subtract(updateTotalMoney).toString()).setScale(2, BigDecimal.ROUND_HALF_UP);

        if (backMoney.compareTo(Quantity.BIG_DECIMAL_0) < 0) {
            throw new MyException("退款金额不可少于0");
        }


        Orders updateOrders = new Orders();
        updateOrders.setId(orders.getId());

        //如果退款金额与订单总额相等
        if (backMoney.compareTo(orderTotalprice) == 0) {
            updateOrders.setOrderstatus(Quantity.STATE_7);
            updateOrders.setReason("平台取消订单");
            updateOrders.setTotalprice(Quantity.BIG_DECIMAL_0);
            updateOrders.setActualpayment(Quantity.BIG_DECIMAL_0);
            updateOrders.setFreight(Quantity.BIG_DECIMAL_0);
            ordersService.deleteBillRecord(orders.getId().toString());
        } else {
            if(orders.getIsmodifyfreight() == 1){
                updateOrders.setFreight(updateFreightMoney.subtract(backFreightMoney));
            }else {
                updateOrders.setFreight(updateFreightMoney);
            }
            updateOrders.setTotalprice(updateTotalMoney.add(totalDiscountpay));
            updateOrders.setActualpayment(updateTotalMoney);

            //修改开票金额
            if (orders.getIsbilling() == Quantity.STATE_1) {
                billingRecordService.updateAdminDecOrderProductnum(orders.getId().toString(), orders.getMemberid(), backMoney.multiply(new BigDecimal("-1")));
            }
        }
        updateOrders.setDiscountprice(totalDiscountpay);
        ordersService.updateSingleOrder(updateOrders);

        //操作日志
        OperateLog operateLog = new OperateLog();
        operateLog.setContent("后台修改订单商品数量，退款" + backMoney);
        operateLog.setOpid(admin.getId());
        operateLog.setOpname(admin.getUsername());
        operateLog.setOptime(new Date());
        operateLog.setOptype(Quantity.STATE_0);
        operateLog.setOrderid(orders.getId());
        operateLog.setOrderno(orders.getOrderno());
        ordersService.saveOperatelog(operateLog);


        if (backMoney.compareTo(Quantity.BIG_DECIMAL_0) > 0) {
            //买家退款资金明细
            BuyerCapital buyerCapital1 = null;

            //判断退回到余额还是授信
            Date tranTime = new Date();
            BuyerCapital buyerCapital = ordersService.getBuyerCapitalByNoType(orders.getOrderno(), Quantity.STATE_0);
            if (buyerCapital != null) {
                //退回到余额
                if (buyerCapital.getPaytype() == Quantity.STATE_3) {
                    memberService.updateBuyerMemberBalanceInDb(orders.getMemberid(), backMoney);
                    buyerCapital1 = createBuyerBackPay(orders, backMoney, tranTime, Quantity.STATE_3);
                }
                //退回到授信
                if (buyerCapital.getPaytype() == Quantity.STATE_4) {
                    buyerCapital1 = createBuyerBackPay(orders, backMoney, tranTime, Quantity.STATE_4);
                    memberService.updateBuyerMemberCreditBalanceInDb(orders.getMemberid(), backMoney.multiply(Quantity.BIG_DECIMAL_MINUS_1), backMoney);
                }


                //退回到支付宝或微信
                if (buyerCapital.getPaytype() == Quantity.STATE_0 ||
                        buyerCapital.getPaytype() == Quantity.STATE_1 ||
                        buyerCapital.getPaytype() == Quantity.STATE_2) {
                    String uuid = orders.getUuid();
                    if (uuid != null) {
                        Refund refund = new Refund();
                        refund.setOutTradeNo(uuid);
                        refund.setChannel(tradeService.getPayChannel(orders.getPaytype()));
                        refund.setRefundAmount((backMoney.multiply(new BigDecimal("100"))).longValue());
                        refund.setRefundReason("订单退款");
                        BigDecimal totalAmout = tradeService.getTotalAmout(uuid);
                        refund.setTotalAmount((totalAmout.multiply(new BigDecimal("100"))).longValue());
                        boolean result = tradeService.backMoney(refund);

                        if (result) {
                            if (orders.getPaytype() == Quantity.STATE_0) {
                                buyerCapital1 = createBuyerBackPay(orders, backMoney, tranTime, Quantity.STATE_0);
                            } else if (orders.getPaytype() == Quantity.STATE_1) {
                                buyerCapital1 = createBuyerBackPay(orders, backMoney, tranTime, Quantity.STATE_1);
                            } else if (orders.getPaytype() == Quantity.STATE_2) {
                                buyerCapital1 = createBuyerBackPay(orders, backMoney, tranTime, Quantity.STATE_2);
                            }
                        } else {
                            throw new CashException("退款失败");
                        }
                    }
                }

                SalerCapital salerCapital =  createSalerBackPay(orders,backMoney,tranTime);

                if (buyerCapital1 != null) {
                    buyerCapitalService.insertSelective(buyerCapital1);
                }
                salerCapitalService.insertSelective(salerCapital);
                if (buyerCapital1!=null&&buyerCapital1.getPaytype()!= Quantity.STATE_3&&buyerCapital1.getPaytype()!= Quantity.STATE_4){
                    Member member=memberService.getMemberById(orders.getMemberid());
                    BuyerStatement buyerStatement=ordersService.createBuyerStateForBack(orders,backMoney,new Date(),buyerCapital1.getPaytype(), (short) StatementType.StType5.getTyep(),member,null,false);
                    statementService.insertStatement(buyerStatement);
                }
            } else {
                throw new MyException("未查询到该订单的付款信息");
            }
        }

        //商品数量被管理员修改 短信通知买家
        List<Orders> list = new ArrayList<>();
        list.add(orders);
        ordersService.smsNotifyAdminProductNum(list);



        return new BasicRet(BasicRet.SUCCESS, "修改成功");
    }

    /**
     * 算出需要退的运费
     * @param orders
     * @param oldTotalProductMoney
     * @param oldTotalWeight
     * @param updateFreightMoney
     * @param updateNumBeforeFreightMoney
     * @return
     * @throws MyException
     */
    private BigDecimal getBackFreightMoney(Orders orders, BigDecimal oldTotalProductMoney, BigDecimal oldTotalWeight, BigDecimal updateFreightMoney, BigDecimal updateNumBeforeFreightMoney) throws MyException {
        BigDecimal oldFreightMoney;
        BigDecimal compareFreightMoney;
        BigDecimal backFreightMoney;OrderFrightDto oldOrderFrightDto = GsonUtils.toBean(orders.getFrighttemplate(), OrderFrightDto.class);
        oldFreightMoney = freightService.getFreightByOrderFrightDto(oldOrderFrightDto, oldTotalProductMoney, oldTotalWeight, orders.getProvince(), orders.getCity());
        compareFreightMoney = oldFreightMoney.subtract(updateNumBeforeFreightMoney);
        //这里divide加2是 BigDecimal的divide方法进行除法时当不整除，出现无限循环小数时，就会抛异常
        backFreightMoney = (compareFreightMoney.divide(oldFreightMoney,5, RoundingMode.HALF_UP)).multiply(updateFreightMoney).setScale(2,BigDecimal.ROUND_HALF_UP);
        return backFreightMoney;
    }

    /**
     * 算出修改数量后 将这个数量传入根据运费模板算出运费
     * 例如数量为1千支 12元运费 修改为0.1千支还是12元
     * @param orders
     * @param oldTotalProductMoney
     * @param oldTotalWeight
     * @return
     * @throws MyException
     */
    private BigDecimal getOldFreightMoney(Orders orders, BigDecimal oldTotalProductMoney, BigDecimal oldTotalWeight) throws MyException {
        BigDecimal oldFreightMoney;
        OrderFrightDto oldOrderFrightDto = GsonUtils.toBean(orders.getFrighttemplate(), OrderFrightDto.class);
        oldFreightMoney = freightService.getFreightByOrderFrightDto(oldOrderFrightDto, oldTotalProductMoney, oldTotalWeight, orders.getProvince(), orders.getCity());
        return oldFreightMoney;
    }


    @RequestMapping(value = "/updateOrderFreight", method = RequestMethod.POST)
    @ApiOperation(value = "订单修改运费")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ordersid", value = "订单id", required = true, paramType = "query", dataType = "long"),
            @ApiImplicitParam(name = "freight", value = "修改后的运费", required = true, paramType = "query", dataType = "double"),
    })
    @PreAuthorize("hasAuthority('" + AdminAuthorityConst.CHANGELOGISTIC + "')")
    public BasicRet updateOrderFreight(Model model, Long ordersid, BigDecimal freight, HttpServletRequest request) {
        BasicRet basicRet = new BasicRet();
        Admin admin = (Admin) model.asMap().get(AppConstant.ADMIN_SESSION_NAME);
        Orders orders = ordersService.getOrdersById(ordersid);
        if (orders != null) {
            //判断订单状态 只有待付款才允许修改
            if(orders.getOrderstatus() != Quantity.STATE_0){
                basicRet.setResult(BasicRet.ERR);
                basicRet.setMessage("不是待付款状态，不允许修改运费");
                return basicRet;
            }
            if(orders.getIsticket() == Quantity.STATE_1){
                basicRet.setResult(BasicRet.ERR);
                basicRet.setMessage("使用优惠券的订单不允许修改运费");
                return basicRet;
            }
            if(!StringUtils.isNumeric(freight.toString())){
                basicRet.setResult(BasicRet.ERR);
                basicRet.setMessage("运费请填写数字");
                return basicRet;
            }
            if(freight.compareTo(new BigDecimal(0)) < 0){
                basicRet.setResult(BasicRet.ERR);
                basicRet.setMessage("运费不能为负数");
                return basicRet;
            }
            if(orders.getFreight().compareTo(freight) == 0){
                basicRet.setResult(BasicRet.ERR);
                basicRet.setMessage("修改后的运费价格和之前的运费相同");
                return basicRet;
            }



            List<OrderProduct> orderProductList = orderProductServices.getByOrderNo(orders.getOrderno());

            BigDecimal totalProductMoney = Quantity.BIG_DECIMAL_0;  //所有商品总金额(不包含运费)
            BigDecimal orderTotalPrice = Quantity.BIG_DECIMAL_0; //订单总价(含运费)
            BigDecimal productMoney = Quantity.BIG_DECIMAL_0;  //一个商品的金额


            BigDecimal deposit = Quantity.BIG_DECIMAL_0;  //远期定金
            BigDecimal balance = Quantity.BIG_DECIMAL_0;  //远期余款
            BigDecimal allPay = Quantity.BIG_DECIMAL_0;  //远期全款


            //重新计算总价和加入运费
            if(orderProductList!=null && orderProductList.size()>0) {
                for (OrderProduct orderProduct : orderProductList) {
                    productMoney = orderProduct.getPrice().multiply(orderProduct.getNum()).setScale(2,BigDecimal.ROUND_HALF_UP);
                    if(orders.getOrdertype() == Quantity.STATE_1) {
                        //远期全款
                        allPay = allPay.add(productMoney.setScale(2,BigDecimal.ROUND_HALF_UP));
                    }
                    if(orders.getOrdertype() == Quantity.STATE_2) {
                        //远期定金
                        deposit = deposit.add(orderProduct.getPartpay());
                        //修改运费的话 远期定金的 余款要加上运费
                        balance = (balance.add(orderProduct.getYupay())).add(freight);
                    }
                    totalProductMoney =  totalProductMoney.add(productMoney);
                }
                orderTotalPrice = totalProductMoney.add(freight);

                //将重新计算后的总价和运费存入
                Orders updateOrders = new Orders();
                updateOrders.setId(orders.getId());
                updateOrders.setFreight(freight);
                //actualpayment是已经减去优惠金额的 totalprice是没有减去优惠券金额的
                //没有使用优惠券的时候
                if (orders.getDiscountprice().compareTo(Quantity.BIG_DECIMAL_0) == 0) {
                    updateOrders.setActualpayment(orderTotalPrice);
                    if(orders.getOrdertype() == Quantity.STATE_1) {
                        updateOrders.setAllpay(allPay.add(freight));
                    }
                } else {
                    //使用优惠券的时候
                    BigDecimal newActualpayment = orderTotalPrice.subtract(orders.getDiscountprice());
                    BigDecimal newAllpay = (allPay.add(freight)).subtract(orders.getDiscountprice());
                    updateOrders.setActualpayment(newActualpayment);
                    if(orders.getOrdertype() == Quantity.STATE_1) {
                        updateOrders.setAllpay(newAllpay);
                    }
                }
                updateOrders.setTotalprice(orderTotalPrice);
                updateOrders.setBalance(balance);
                updateOrders.setDeposit(deposit);
                //设置为已修改过运费
                updateOrders.setIsmodifyfreight(Quantity.STATE_1);
                ordersService.updateOrders(updateOrders);

                //修改开票金额
                if(orders.getIsbilling() == Quantity.STATE_1){
                    //直接用修改过后的运费减去修改前的运费 差额就是开票要加的
                    BigDecimal comparefreight = freight.subtract(orders.getFreight());
                    billingRecordService.updateAdminDecOrderProductnum(orders.getId().toString(),orders.getMemberid(),comparefreight);
                }
            }
        } else {
            basicRet.setResult(BasicRet.ERR);
            basicRet.setMessage("订单不存在");
            return basicRet;
        }

        //保存操作日志
        OperateLog operateLog = new OperateLog();
        operateLog.setContent(freight + "运费修改");
        operateLog.setContent("平台管理员将原运费"+orders.getFreight()+"改成"+freight);
        operateLog.setOpid(admin.getId());
        operateLog.setOpname(admin.getRealname());
        operateLog.setOptime(new Date());
        operateLog.setOptype(Quantity.STATE_0);
        operateLog.setOrderid(orders.getId());
        operateLog.setOrderno(orders.getOrderno());
        ordersService.saveOperatelog(operateLog);
        basicRet.setResult(BasicRet.SUCCESS);
        basicRet.setMessage("修改成功");
        //用户日志
        memberLogOperator.saveMemberLog(null,admin, "订单运费修改为：" + freight, "/rest/admin/orders/updateOrderFreight",request, memberOperateLogService);
        return basicRet;
    }


/*
    @RequestMapping(value = "/updateOrderProductBack",method = RequestMethod.POST)
    @ApiOperation(value = "修改退货申请")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id",value = "退货申请id",required = true,paramType = "query",dataType = "long"),
            @ApiImplicitParam(name = "state",value = "退货状态0=待卖家处理1=卖家同意待收货2=卖家同意直接退款3=卖家收到货同意退款4=卖家不同意5=买家同意验货6=买家申请异议7=平台同意退货不扣违约金8=平台同意退货扣违约金9=平台转入待验收10=退货成功11=撤消",required = false,paramType = "query",dataType = "int"),
            @ApiImplicitParam(name = "logisticsno",value = "退货单号",required = false,paramType = "query",dataType = "String"),
            @ApiImplicitParam(name = "logisticscompany",value = "退货物流公司",required = false,paramType = "query",dataType = "String"),
            @ApiImplicitParam(name = "backaddress",value = "退货地址",required = false,paramType = "query",dataType = "String"),
            @ApiImplicitParam(name = "contact",value = "退货联系人",required = false,paramType = "query",dataType = "String"),
            @ApiImplicitParam(name = "contactphone",value = "退货联系人电话",required = false,paramType = "query",dataType = "String"),
            @ApiImplicitParam(name = "sellernotagree",value = "卖家不同意原因",required = false,paramType = "query",dataType = "String"),
            @ApiImplicitParam(name = "adminreason",value = "平台处理说明",required = false,paramType = "query",dataType = "String"),
            @ApiImplicitParam(name = "adminstate",value = "平台处理意见0=正常1=不扣违约金2=扣违约金3=转入待验收",required = false,paramType = "query",dataType = "int"),
            @ApiImplicitParam(name = "province",value = "省",required = false,paramType = "query",dataType = "String"),
            @ApiImplicitParam(name = "city",value = "市",required = false,paramType = "query",dataType = "String"),
            @ApiImplicitParam(name = "area",value = "区",required = false,paramType = "query",dataType = "String"),
            @ApiImplicitParam(name = "backtype",value = "退货类型0=仅退款1=退货退款2=部分退货",required = true,paramType = "query",dataType = "int"),
    })
    @PreAuthorize("hasAuthority('" + AdminAuthorityConst.RETURNMANAGEMENT + "')")
    public BasicRet updateOrderProductBack(Model model, ProductBackModel productBackModel, HttpServletRequest request) throws CashException {

        Admin member = (Admin) model.asMap().get(AppConstant.ADMIN_SESSION_NAME);

        BasicRet basicRet = new BasicRet();

        Short state = productBackModel.getState();

        if(state != Quantity.STATE_7 && state != Quantity.STATE_8 && state != Quantity.STATE_9){
            basicRet.setMessage("操作状态不合法");
            basicRet.setResult(BasicRet.ERR);
            return basicRet;
        }


        Short adminstate = productBackModel.getAdminstate();
        OrderProductBack orderProductBack = ordersService.getOrderProductBackById(productBackModel.getId());

        if(orderProductBack.getState()==Quantity.STATE_11){
            basicRet.setMessage("退货已撤消");
            basicRet.setResult(BasicRet.ERR);
            return basicRet;
        }else if(orderProductBack.getState()==Quantity.STATE_10){
            basicRet.setMessage("退货已完成");
            basicRet.setResult(BasicRet.ERR);
            return basicRet;
        }else {
            orderProductBack.setState(state);
            ProductStore store = null;
            LimitTimeStore limitTimeStore = null;
            LimitTimeProd limitTimeProd = null;
            Orders orders = ordersService.getOrdersById(orderProductBack.getOrderid());
            if(orders.getIsonline()==Quantity.STATE_0){
                store = ordersService.getProductStore(orderProductBack.getPdid(),orderProductBack.getPdno(),orderProductBack.getStoreid());
            }
            if(orders.getIsonline()==Quantity.STATE_2){
                limitTimeStore = shopCarService.getLimitTimeStore(orderProductBack.getStoreid());
                limitTimeProd = shopCarService.getLimitTimeProd(orderProductBack.getPdid(),orderProductBack.getLimitid());
            }
            if(adminstate!=null){
                orderProductBack.setAdminstate(adminstate);
            }
            if(StringUtils.hasText(productBackModel.getLogisticsno())){
                orderProductBack.setLogisticsno(productBackModel.getLogisticsno());
            }
            if(StringUtils.hasText(productBackModel.getLogisticscompany())){
                orderProductBack.setLogisticscompany(productBackModel.getLogisticscompany());
            }
            if(StringUtils.hasText(productBackModel.getBackaddress())){
                orderProductBack.setBackaddress(productBackModel.getBackaddress());
            }
            if(StringUtils.hasText(productBackModel.getContact())){
                orderProductBack.setContact(productBackModel.getContact());
            }
            if(StringUtils.hasText(productBackModel.getContactphone())){
                orderProductBack.setContactphone(productBackModel.getContactphone());
            }
            if(StringUtils.hasText(productBackModel.getSellernotagree())){
                orderProductBack.setSellernotagree(productBackModel.getSellernotagree());
            }
            if(StringUtils.hasText(productBackModel.getAdminreason())){
                orderProductBack.setAdminreason(productBackModel.getAdminreason());
            }
            if(StringUtils.hasText(productBackModel.getProvince())){
                orderProductBack.setProvince(productBackModel.getProvince());
            }
            if(StringUtils.hasText(productBackModel.getCity())){
                orderProductBack.setCity(productBackModel.getCity());
            }
            if(StringUtils.hasText(productBackModel.getArea())){
                orderProductBack.setArea(productBackModel.getArea());
            }
            if(productBackModel.getBacktype()!=null){
                orderProductBack.setBacktype(productBackModel.getBacktype());
            }
            if(orderProductBack!=null){
                OrderProduct orderProduct = ordersService.getOrderProductById(orderProductBack.getOrderpdid());
                List<OrderProduct> list1 = ordersService.getOrderProductByOrderId(orderProductBack.getOrderid());
                if(state!=null){
                    //卖家同意待验货
                    if(state==Quantity.STATE_1){
                        orderProduct.setBackstate(Quantity.STATE_2);
                        //卖家同意直接退款或卖家收到货同意退款
                    } else if(state==Quantity.STATE_2||state==Quantity.STATE_3){
                        orderProduct.setBackstate(Quantity.STATE_3);
                        handleBackGoods(member,orderProductBack,orderProduct,orders);
                        orderProductBack.setState(Quantity.STATE_10);
                        //增加库存
                        if(orders.getIsonline()==Quantity.STATE_0){
                            store.setPdstorenum(store.getPdstorenum().add(orderProductBack.getPdnum()));
                            ordersService.updateProductStore(store);
                        }
                        if(orders.getIsonline()==Quantity.STATE_2){
                            limitTimeStore.setStorenum(limitTimeStore.getStorenum().add(orderProductBack.getPdnum()));
                            limitTimeStore.setSalesnum(limitTimeStore.getSalesnum().subtract(orderProductBack.getPdnum()));
                            shopCarService.updateLimitTimeStore(limitTimeStore);
                            limitTimeProd.setSalestotalnum(limitTimeProd.getSalestotalnum().subtract(orderProduct.getNum()));
                            shopCarService.updateLimitTimeProd(limitTimeProd);
                        }
                        //买家同意验货
                    }else if(state==Quantity.STATE_5){
                        orderProduct.setBackstate(Quantity.STATE_0);
                        //买家不同意申请异议
                    }else if(state==Quantity.STATE_6){
                        orderProduct.setBackstate(Quantity.STATE_4);
                        orderProductBack.setDissidencetime(new Date());
                        //平台同意退货不扣违约金
                    }else if(state==Quantity.STATE_7){
                        //只退款
                        if(orderProductBack.getBacktype()==Quantity.STATE_0){
                            handleBackGoods(member,orderProductBack,orderProduct,orders);
                            orderProduct.setBackstate(Quantity.STATE_3);
                            orderProductBack.setState(Quantity.STATE_10);
                            //增加库存
                            if(orders.getIsonline()==Quantity.STATE_0){
                                store.setPdstorenum(store.getPdstorenum().add(orderProductBack.getPdnum()));
                                ordersService.updateProductStore(store);
                            }
                            if(orders.getIsonline()==Quantity.STATE_2){
                                limitTimeStore.setStorenum(limitTimeStore.getStorenum().add(orderProductBack.getPdnum()));
                                limitTimeStore.setSalesnum(limitTimeStore.getSalesnum().subtract(orderProductBack.getPdnum()));
                                shopCarService.updateLimitTimeStore(limitTimeStore);
                                limitTimeProd.setSalestotalnum(limitTimeProd.getSalestotalnum().subtract(orderProduct.getNum()));
                                shopCarService.updateLimitTimeProd(limitTimeProd);
                            }
                        }else{
                            //退货验收
                            orderProduct.setBackstate(Quantity.STATE_2);
                        }
                        //平台同意退货扣违约金
                    }else if(state==Quantity.STATE_8){
                        //只退款
                        if(orderProductBack.getBacktype()==Quantity.STATE_0){
                            handleBackGoods(member,orderProductBack,orderProduct,orders);
                            orderProduct.setBackstate(Quantity.STATE_3);
                            orderProductBack.setState(Quantity.STATE_10);
                            //增加库存
                            if(orders.getIsonline()==Quantity.STATE_0){
                                store.setPdstorenum(store.getPdstorenum().add(orderProductBack.getPdnum()));
                                ordersService.updateProductStore(store);
                            }
                            if(orders.getIsonline()==Quantity.STATE_2){
                                limitTimeStore.setStorenum(limitTimeStore.getStorenum().add(orderProductBack.getPdnum()));
                                limitTimeStore.setSalesnum(limitTimeStore.getSalesnum().subtract(orderProductBack.getPdnum()));
                                shopCarService.updateLimitTimeStore(limitTimeStore);
                                limitTimeProd.setSalestotalnum(limitTimeProd.getSalestotalnum().subtract(orderProduct.getNum()));
                                shopCarService.updateLimitTimeProd(limitTimeProd);
                            }
                        }else{
                            //退货验收
                            orderProduct.setBackstate(Quantity.STATE_2);
                        }
                        //平台转入待验收
                    }else if(state==Quantity.STATE_9){
                        //部分退货
                        if (orderProductBack.getBacktype() == Quantity.STATE_2) {
                            for (OrderProduct op : list1) {
                                //找出部分退货相同的商品，删除这个部分退货，并加数量和总价到原来的商品
                                if (op.getPdid().longValue() == orderProductBack.getPdid().longValue() && op.getPdno().equals(orderProductBack.getPdno()) && op.getBackstate() == Quantity.STATE_0) {
                                    op.setNum(op.getNum().add(orderProduct.getNum()));
                                    op.setActualpayment(op.getNum().multiply(op.getPrice()).add(op.getFreight()));
                                    ordersService.updateOrderProduct(op);
                                    ordersService.deleteOrderProduct(orderProduct.getId());
                                    break;
                                    //如果部分退货全部退掉，找不到原来的商品，就更新状态
                                } else {
                                    orderProduct.setBackstate(Quantity.STATE_0);
                                }
                            }
                        } else {
                            orderProduct.setBackstate(Quantity.STATE_0);
                        }
                    }else if(state==Quantity.STATE_11){
                        orderProduct.setBackstate(Quantity.STATE_0);
                    } else{
                        //卖家不同意
                    }
                    ordersService.updateOrderProduct(orderProduct);
                    ordersService.updateOrderProductBack(orderProductBack);

                    List<OrderProduct> list = ordersService.getOrderProductByOrderId(orderProductBack.getOrderid(),orderProductBack.getOrderpdid());

                    boolean flag = true;
                    for(OrderProduct op:list){
                        if(op.getBackstate()!=Quantity.STATE_3){
                            flag =false;
                            break;
                        }
                    }
                    //判断订单中商品是否都退货完成，就结束订单
                    if(orderProduct.getBackstate()==Quantity.STATE_3&&flag){
                        //删除开票申请记录
                        ordersService.deleteBillRecord(orders.getOrderno());
                        orders.setOrderstatus(Quantity.STATE_7);
                        ordersService.updateSingleOrder(orders);
                    }
                }
            }else{
                basicRet.setMessage("没有退货申请记录");
                basicRet.setResult(BasicRet.ERR);
                return  basicRet;
            }

            //保存操作日志
            OperateLog operateLog = new OperateLog();
            //货状态0=待卖家处理1=卖家同意待收货2=卖家同意直接退款3=卖家收到货同意退款4=卖家不同意5=买家同意验货6=买家申请异议7=平台同意退货不扣违约金8=平台同意退货扣违约金9=平台转入待验收10=退货成功11=撤消
            operateLog.setOpid(member.getId());
            operateLog.setOpname(member.getUsername());
            operateLog.setOptime(new Date());
            operateLog.setOptype(Quantity.STATE_1);
            operateLog.setOrderid(orderProductBack.getOrderid());
            operateLog.setOrderno(orderProductBack.getOrderno());
            operateLog.setOrderpdid(orderProductBack.getOrderpdid());

//            if(orderProductBack.getState()==Quantity.STATE_1){
//                operateLog.setContent("卖家同意待收货");
//                ordersService.saveOperatelog(operateLog);
//            }
//            if(orderProductBack.getState()==Quantity.STATE_2){
//                operateLog.setContent("卖家同意直接退款");
//                ordersService.saveOperatelog(operateLog);
//            }
//            if(orderProductBack.getState()==Quantity.STATE_3){
//                operateLog.setContent("卖家收到货同意退款");
//                ordersService.saveOperatelog(operateLog);
//            }
//            if(orderProductBack.getState()==Quantity.STATE_4){
//                operateLog.setContent("卖家不同意");
//                ordersService.saveOperatelog(operateLog);
//            }
//            if(orderProductBack.getState()==Quantity.STATE_5){
//                operateLog.setContent("买家同意验货");
//                ordersService.saveOperatelog(operateLog);
//            }
//            if(orderProductBack.getState()==Quantity.STATE_6){
//                operateLog.setContent("买家申请异议");
//                ordersService.saveOperatelog(operateLog);
//            }
//            if(orderProductBack.getState()==Quantity.STATE_7){
//                operateLog.setContent("平台同意退货不扣违约金");
//                ordersService.saveOperatelog(operateLog);
//            }
//            if(orderProductBack.getState()==Quantity.STATE_8){
//                operateLog.setContent("平台同意退货扣违约金");
//                ordersService.saveOperatelog(operateLog);
//            }
//            if(orderProductBack.getState()==Quantity.STATE_9){
//                operateLog.setContent("平台转入待验收");
//                ordersService.saveOperatelog(operateLog);
//            }
//            if(orderProductBack.getState()==Quantity.STATE_10){
//                operateLog.setContent("退货成功");
//                ordersService.saveOperatelog(operateLog);
//            }
//            if(orderProductBack.getState()==Quantity.STATE_11){
//                operateLog.setContent("退货撤消");
//                ordersService.saveOperatelog(operateLog);
//            }

            operateLog.setContent(JinshangUtils.orderProductBackStateMess(orderProductBack.getState()));
            ordersService.saveOperatelog(operateLog);


            //保存用户日志
            memberLogOperator.saveMemberLog(null,member,"修改退货申请，退货申请id为："+productBackModel.getId(),"/rest/admin/orders/updateOrderProductBack",request,memberOperateLogService);
            basicRet.setMessage("修改成功");
            basicRet.setResult(BasicRet.SUCCESS);
            return  basicRet;
        }
    }

*/

    /**
     * 退款操作
     * @param operator 操作人
     * @param orderProductBack
     * @param orderProduct
     */
    /*
    public void handleBackGoods(Admin operator,OrderProductBack orderProductBack,OrderProduct orderProduct,Orders order) throws CashException {
        //退款申请人
        Member buyer = memberService.getMemberById(orderProductBack.getMemberid());
        Member seller = memberService.getMemberById(orderProductBack.getSellerid());
        

        Member oldBuyer = new Member();
        BeanUtils.copyProperties(buyer,oldBuyer);

        Member oldSeller = new Member();
        BeanUtils.copyProperties(seller,oldSeller);

        TransactionSetting transactionSetting = transactionSettingService.getTransactionSetting();
        BigDecimal getLiquidated = transactionSetting.getGetliquidated().multiply(new BigDecimal(0.01));
        BigDecimal forwardsalesmargin = transactionSetting.getForwardsalesmargin().multiply(new BigDecimal(0.01));

        if(orderProductBack!=null&&orderProduct!=null&&order!=null){
            BigDecimal backPay = new BigDecimal(0);
            BigDecimal penaltyPay = new BigDecimal(0);

            Date tranTime = new Date();
            List<BuyerCapital> buyerCapitals = new ArrayList<BuyerCapital>();
            List<SalerCapital> salerCapitals = new ArrayList<SalerCapital>();
            //买家退款资金明细
            BuyerCapital buyerCapital1 = null;
            //买家违约资金明细
            BuyerCapital buyerCapital2 = null;
            //卖家退款资金明细
            SalerCapital salerCapital1 = null;
            //卖家违约资金明细
            SalerCapital salerCapital2 = null;
            //立即发货,只有立即发货有部分退款
            if(orderProduct.getProtype()==Quantity.STATE_0){
                BigDecimal orderPay = orderProduct.getActualpayment().subtract(orderProduct.getFreight());
                //是否有部分退货的情况
                BigDecimal backPayMoney = orderProductBack.getBackmoney();
                //违约金
                penaltyPay = orderPay.multiply(getLiquidated);
                //退回的金额,异议扣违约金
                if(orderProductBack.getAdminstate()==Quantity.STATE_2){
                    backPay = backPayMoney.subtract(penaltyPay);
                }else{
                    backPay = backPayMoney;
                }
                //判断退回到余额还是授信
                BuyerCapital buyerCapital = ordersService.getBuyerCapitalByNoType(orderProduct.getOrderno(),Quantity.STATE_0);
                if(buyerCapital!=null){
                    //退回到余额
                    if(buyerCapital.getPaytype()==Quantity.STATE_3){
                        buyer.setBalance(buyer.getBalance().add(backPay));
                        buyerCapital1 = createBuyerBackPay(order,backPay,tranTime,Quantity.STATE_3);
                        salerCapital1 = createSalerBackPay(order,backPay,tranTime);
                        buyerCapitals.add(buyerCapital1);
                        salerCapitals.add(salerCapital1);
                    }
                    //退回到授信
                    if(buyerCapital.getPaytype()==Quantity.STATE_4){
                        buyer.setAvailablelimit(buyer.getAvailablelimit().add(backPay));
                        buyer.setUsedlimit(buyer.getUsedlimit().subtract(backPay));
                        buyerCapital1 = createBuyerBackPay(order,backPay,tranTime,Quantity.STATE_4);
                        salerCapital1 = createSalerBackPay(order,backPay,tranTime);
                        buyerCapitals.add(buyerCapital1);
                        salerCapitals.add(salerCapital1);
                    }
                    //退回到支付宝或微信
                    if(buyerCapital.getPaytype()==Quantity.STATE_0||buyerCapital.getPaytype()==Quantity.STATE_1){
                        String uuid = order.getUuid();
                        if(uuid!=null){
                            Refund refund = new Refund();
                            refund.setOutTradeNo(uuid);
                            if(order.getPaytype()==Quantity.STATE_0){
                                refund.setChannel("alipay");
                            }else {
                                refund.setChannel("wxpay");
                            }
                            refund.setRefundAmount((backPay.multiply(new BigDecimal(100))).longValue());
                            refund.setRefundReason("订单退款");
                            List<Orders> ordersList = ordersService.getOrdersByUUID(uuid);
                            BigDecimal totalAmout = new BigDecimal(0);
                            for(Orders od : ordersList){
                                totalAmout = totalAmout.add(od.getActualpayment());
                            }
                            refund.setTotalAmount((totalAmout.multiply(new BigDecimal(100))).longValue());
                            boolean result = false;
                            try {
                                if("alipay".equals(refund.getChannel())){
                                    result = alipayService.refund(refund);
                                }else if("wxpay".equals(refund.getChannel())){
                                    result = wxPayService.refund(refund);
                                }
                            }catch (Exception e){

                            }
                            if(result){
                                if(order.getPaytype()==Quantity.STATE_0){
                                    buyerCapital1 = createBuyerBackPay(order, backPay, tranTime, Quantity.STATE_0);
                                }else {
                                    buyerCapital1 = createBuyerBackPay(order, backPay, tranTime, Quantity.STATE_1);
                                }
                                salerCapital1 = createSalerBackPay(order, backPay, tranTime);
                                buyerCapitals.add(buyerCapital1);
                                salerCapitals.add(salerCapital1);
                            }

                        }
                    }
                    //异议扣违约金,记录资金明细
                    if(orderProductBack.getAdminstate()==Quantity.STATE_2){
                        seller.setSellerbanlance(seller.getSellerbanlance().add(penaltyPay.multiply(forwardsalesmargin)));
                        //违约金
                        buyerCapital2 = createBuyerPenaltyPay(order,penaltyPay,tranTime,Quantity.STATE_6,orderPay,Quantity.BUYER_BACK_REASON);
                        salerCapital2 = createSalerPenaltyPay(order,penaltyPay,tranTime,Quantity.STATE_6,orderPay,Quantity.BUYER_BACK_REASON);
                        buyerCapitals.add(buyerCapital2);
                        salerCapitals.add(salerCapital2);
                    }
                }
            }
            //远期全款
            if(orderProduct.getProtype()==Quantity.STATE_1){
                //计算扣除违约金
                BigDecimal orderPay = orderProduct.getAllpay();
                //违约金
                penaltyPay = orderPay.multiply(getLiquidated);
                //退回的金额,异议扣违约金
                if(orderProductBack.getAdminstate()==Quantity.STATE_2){
                    backPay = orderPay.subtract(penaltyPay);
                }else{
                    backPay = orderPay;
                }

                //判断退回到余额还是授信
                BuyerCapital buyerCapital = ordersService.getBuyerCapitalByNoType(orderProduct.getOrderno(),Quantity.STATE_9);
                if(buyerCapital!=null){
                    //退回到余额
                    if(buyerCapital.getPaytype()==Quantity.STATE_3){
                        buyer.setBalance(buyer.getBalance().add(backPay));
                        buyerCapital1 = createBuyerBackPay(order,backPay,tranTime,Quantity.STATE_3);
                        salerCapital1 = createSalerBackPay(order,backPay,tranTime);
                        buyerCapitals.add(buyerCapital1);
                        salerCapitals.add(salerCapital1);
                    }
                    //退回到授信
                    if(buyerCapital.getPaytype()==Quantity.STATE_4){
                        buyer.setAvailablelimit(buyer.getAvailablelimit().add(backPay));
                        buyer.setUsedlimit(buyer.getUsedlimit().subtract(backPay));
                        buyerCapital1 = createBuyerBackPay(order,backPay,tranTime,Quantity.STATE_4);
                        salerCapital1 = createSalerBackPay(order,backPay,tranTime);
                        buyerCapitals.add(buyerCapital1);
                        salerCapitals.add(salerCapital1);
                    }
                    //退回到支付宝或微信
                    if(buyerCapital.getPaytype()==Quantity.STATE_0||buyerCapital.getPaytype()==Quantity.STATE_1){
                        String uuid = order.getUuid();
                        if(uuid!=null){
                            Refund refund = new Refund();
                            refund.setOutTradeNo(uuid);
                            if(order.getPaytype()==Quantity.STATE_0){
                                refund.setChannel("alipay");
                            }else {
                                refund.setChannel("wxpay");
                            }
                            refund.setRefundAmount((backPay.multiply(new BigDecimal(100))).longValue());
                            refund.setRefundReason("订单退款");
                            List<Orders> ordersList = ordersService.getOrdersByUUID(uuid);
                            BigDecimal totalAmout = new BigDecimal(0);
                            for(Orders od : ordersList){
                                totalAmout = totalAmout.add(od.getActualpayment());
                            }
                            refund.setTotalAmount((totalAmout.multiply(new BigDecimal(100))).longValue());
                            boolean result = false;
                            try {
                                if("alipay".equals(refund.getChannel())){
                                    result = alipayService.refund(refund);
                                }else if("wxpay".equals(refund.getChannel())){
                                    result = wxPayService.refund(refund);
                                }
                            }catch (Exception e){

                            }
                            if(result){
                                if(order.getPaytype()==Quantity.STATE_0){
                                    buyerCapital1 = createBuyerBackPay(order, backPay, tranTime, Quantity.STATE_0);
                                }else {
                                    buyerCapital1 = createBuyerBackPay(order, backPay, tranTime, Quantity.STATE_1);
                                }
                                salerCapital1 = createSalerBackPay(order, backPay, tranTime);
                                buyerCapitals.add(buyerCapital1);
                                salerCapitals.add(salerCapital1);
                            }

                        }
                    }
                    //异议扣违约金,记录资金明细
                    if(orderProductBack.getAdminstate()==Quantity.STATE_2){
                        seller.setSellerbanlance(seller.getSellerbanlance().add(penaltyPay.multiply(forwardsalesmargin)));
                        //违约金
                        buyerCapital2 = createBuyerPenaltyPay(order,penaltyPay,tranTime,Quantity.STATE_6,orderPay,Quantity.BUYER_BACK_REASON);
                        salerCapital2 = createSalerPenaltyPay(order,penaltyPay,tranTime,Quantity.STATE_6,orderPay,Quantity.BUYER_BACK_REASON);
                        buyerCapitals.add(buyerCapital2);
                        salerCapitals.add(salerCapital2);
                    }
                }

            }
            //远期余款
            if(orderProduct.getProtype()==Quantity.STATE_2){
                //计算扣除违约金
                BigDecimal orderPay = orderProduct.getActualpayment().subtract(orderProduct.getFreight());
                BigDecimal partPay = orderProduct.getPartpay().subtract(orderProduct.getFreight());
                BigDecimal yuPay = orderProduct.getYupay();

                //违约金
                penaltyPay = orderPay.multiply(getLiquidated);
                //定金支付明细
                BuyerCapital depositBuyerCapital = ordersService.getBuyerCapitalByNoType(order.getOrderno(), Quantity.STATE_7);

                if(depositBuyerCapital!=null){
                    //异议扣违约金
                    //退回的金额,异议扣违约金
                    if (orderProductBack.getAdminstate() == Quantity.STATE_2) {
                        backPay = orderPay.subtract(penaltyPay);
                        yuPay = yuPay.subtract(penaltyPay);
                    } else {
                        backPay = orderPay;
                    }
                    //退回到余额
                    if (depositBuyerCapital.getPaytype() == Quantity.STATE_3) {
                        buyer.setBalance(buyer.getBalance().add(backPay));
                        buyerCapital1 = createBuyerBackPay(order, backPay, tranTime, Quantity.STATE_3);
                        salerCapital1 = createSalerBackPay(order, backPay, tranTime);
                        buyerCapitals.add(buyerCapital1);
                        salerCapitals.add(salerCapital1);
                    }
                    //退回到授信
                    if (depositBuyerCapital.getPaytype() == Quantity.STATE_4) {
                        buyer.setAvailablelimit(buyer.getAvailablelimit().add(backPay));
                        buyer.setUsedlimit(buyer.getUsedlimit().subtract(backPay));
                        buyerCapital1 = createBuyerBackPay(order, backPay, tranTime, Quantity.STATE_4);
                        salerCapital1 = createSalerBackPay(order, backPay, tranTime);
                        buyerCapitals.add(buyerCapital1);
                        salerCapitals.add(salerCapital1);
                    }
                    //退回到支付宝或微信
                    if(depositBuyerCapital.getPaytype()==Quantity.STATE_0||depositBuyerCapital.getPaytype()==Quantity.STATE_1){
                        String uuid = order.getUuid();
                        String yuuuid = order.getYuuuid();
                        if(uuid!=null&&yuuuid!=null){
                            //定金
                            Refund refund1 = new Refund();
                            //余款
                            Refund refund2 = new Refund();
                            refund1.setOutTradeNo(uuid);
                            refund2.setOutTradeNo(yuuuid);
                            if(order.getPaytype()==Quantity.STATE_0){
                                refund1.setChannel("alipay");
                                refund2.setChannel("alipay");
                            }else {
                                refund1.setChannel("wxpay");
                                refund2.setChannel("wxpay");
                            }
                            refund1.setRefundAmount((partPay.multiply(new BigDecimal(100))).longValue());
                            refund2.setRefundAmount((yuPay.multiply(new BigDecimal(100))).longValue());

                            refund1.setRefundReason("订单定金退款");
                            refund2.setRefundReason("订单余款退款");
                            List<Orders> ordersList = ordersService.getOrdersByUUID(uuid);
                            BigDecimal totalPartPay = new BigDecimal(0);
                            BigDecimal totalYuPay = new BigDecimal(0);
                            for(Orders od : ordersList){
                                totalPartPay = totalPartPay.add(od.getDeposit());
                                totalYuPay = totalYuPay.add(od.getBalance());
                            }
                            refund1.setTotalAmount((totalPartPay.multiply(new BigDecimal(100))).longValue());
                            refund2.setTotalAmount((totalYuPay.multiply(new BigDecimal(100))).longValue());

                            boolean result1 = false;
                            boolean result2 = false;
                            try {
                                if("alipay".equals(refund1.getChannel())){
                                    result1 = alipayService.refund(refund1);
                                    result2 = alipayService.refund(refund2);
                                }else if("wxpay".equals(refund1.getChannel())){
                                    result1 = wxPayService.refund(refund1);
                                    result2 = wxPayService.refund(refund2);
                                }
                            }catch (Exception e){

                            }
                            if(result1&&result2){
                                if(order.getPaytype()==Quantity.STATE_0){
                                    buyerCapital1 = createBuyerBackPay(order, backPay, tranTime, Quantity.STATE_0);
                                }else {
                                    buyerCapital1 = createBuyerBackPay(order, backPay, tranTime, Quantity.STATE_1);
                                }
                                salerCapital1 = createSalerBackPay(order, backPay, tranTime);
                                buyerCapitals.add(buyerCapital1);
                                salerCapitals.add(salerCapital1);
                            }

                        }
                    }
                    //异议扣违约金,记录资金明细
                    if(orderProductBack.getAdminstate()==Quantity.STATE_2){
                        seller.setSellerbanlance(seller.getSellerbanlance().add(penaltyPay.multiply(forwardsalesmargin)));
                        //违约金
                        buyerCapital2 = createBuyerPenaltyPay(order,penaltyPay,tranTime,Quantity.STATE_6,orderPay,Quantity.BUYER_BACK_REASON);
                        salerCapital2 = createSalerPenaltyPay(order,penaltyPay,tranTime,Quantity.STATE_6,orderPay,Quantity.BUYER_BACK_REASON);
                        buyerCapitals.add(buyerCapital2);
                        salerCapitals.add(salerCapital2);
                    }
                }
            }
            //保存用户余额和授信
            ordersService.saveMember(buyer,oldBuyer);
            ordersService.saveMember(seller,oldSeller);


            if(buyerCapitals.size()>0){
                ordersService.insertBuyerCapital(buyerCapitals);
                //保存操作日志
                OperateLog operateLog = new OperateLog();
                operateLog.setContent("退款成功");
                operateLog.setOpid(operator.getId());
                operateLog.setOpname(operator.getUsername());
                operateLog.setOptime(new Date());
                operateLog.setOptype(Quantity.STATE_1);
                operateLog.setOrderid(orderProductBack.getOrderid());
                operateLog.setOrderno(orderProductBack.getOrderno());
                operateLog.setOrderpdid(orderProductBack.getOrderpdid());
                ordersService.saveOperatelog(operateLog);
            }
            if(salerCapitals.size()>0){
                ordersService.insertSallerCapital(salerCapitals);
            }
        }

    }

    */


    /**
     * 退款操作
     *
     * @param orderProductBack
     * @param orderProduct
     * @param order
     * @param operatorId       操作者id
     * @param operatorUsername 操作者用户名
     * @throws CashException
     */
    private void handleBackGoods1(OrderProductBack orderProductBack, OrderProduct orderProduct, Orders order, Long operatorId, String operatorUsername) throws CashException {

        //卖家
        Member sellerMember = memberService.getMemberById(order.getSaleid());
        Member oldSellerMember = new Member();
        BeanUtils.copyProperties(sellerMember, oldSellerMember);

        //退款申请人
        Member buyer = memberService.getMemberById(order.getMemberid());
        Member oldBuyer = new Member();
        BeanUtils.copyProperties(buyer, oldBuyer);


        //违约金占比
        TransactionSetting transactionSetting = transactionSettingService.getTransactionSetting();
        BigDecimal getLiquidated = transactionSetting.getGetliquidated().multiply(new BigDecimal(0.01));
        BigDecimal forwardsalesmargin = transactionSetting.getForwardsalesmargin().multiply(new BigDecimal(0.01));

        if (orderProductBack != null && orderProduct != null && order != null) {
            BigDecimal backPay = new BigDecimal(0);
            BigDecimal penaltyPay = new BigDecimal(0);

            Date tranTime = new Date();
            List<BuyerCapital> buyerCapitals = new ArrayList<BuyerCapital>();
            List<SalerCapital> salerCapitals = new ArrayList<SalerCapital>();
            //买家退款资金明细
            BuyerCapital buyerCapital1 = null;
            //买家违约资金明细
            BuyerCapital buyerCapital2 = null;
            //卖家退款资金明细
            SalerCapital salerCapital1 = null;
            //卖家违约资金明细
            SalerCapital salerCapital2 = null;
            //立即发货,只有立即发货有部分退款
            if (orderProduct.getProtype() == Quantity.STATE_0) {
                BigDecimal orderPay = orderProduct.getActualpayment().subtract(orderProduct.getFreight());
                //是否有部分退货的情况
                BigDecimal backPayMoney = orderProductBack.getBackmoney();

                //退回的金额,异议扣违约金
                if (orderProductBack.getAdminstate() == Quantity.STATE_2) {
                    //违约金
                    penaltyPay = orderPay.multiply(getLiquidated).setScale(2, BigDecimal.ROUND_HALF_UP);
                }

                backPay = backPayMoney;

                //判断退回到余额还是授信
                BuyerCapital buyerCapital = ordersService.getBuyerCapitalByNoType(orderProduct.getOrderno(), Quantity.STATE_0);
                if (buyerCapital != null) {
                    //退回到余额
                    if (buyerCapital.getPaytype() == Quantity.STATE_3) {
                        buyer.setBalance(buyer.getBalance().add(backPay.subtract(penaltyPay)).setScale(2, BigDecimal.ROUND_HALF_UP));
                        buyerCapital1 = createBuyerBackPay(order, backPay.subtract(penaltyPay).setScale(2, BigDecimal.ROUND_HALF_UP), tranTime, Quantity.STATE_3);
                        salerCapital1 = createSalerBackPay(order, backPay, tranTime);
                        buyerCapitals.add(buyerCapital1);
                        salerCapitals.add(salerCapital1);
                    }


                    //退回到授信
                    if (buyerCapital.getPaytype() == Quantity.STATE_4) {
                        buyer.setAvailablelimit(buyer.getAvailablelimit().add(backPay.subtract(penaltyPay)).setScale(2, BigDecimal.ROUND_HALF_UP));
                        buyer.setUsedlimit(buyer.getUsedlimit().subtract(backPay.subtract(penaltyPay)));
                        buyerCapital1 = createBuyerBackPay(order, backPay.subtract(penaltyPay).setScale(2, BigDecimal.ROUND_HALF_UP), tranTime, Quantity.STATE_4);
                        salerCapital1 = createSalerBackPay(order, backPay, tranTime);
                        buyerCapitals.add(buyerCapital1);
                        salerCapitals.add(salerCapital1);
                    }


                    //退回到支付宝\微信\银行卡
                    if (buyerCapital.getPaytype() == Quantity.STATE_0 || buyerCapital.getPaytype() == Quantity.STATE_1 || buyerCapital.getPaytype() == Quantity.STATE_2) {
                        String uuid = order.getUuid();
                        Long ordertime = order.getOrdertime();
                        if (uuid != null) {
                            Refund refund = new Refund();
                            refund.setDatetime(ordertime);
                            refund.setOutTradeNo(uuid);
                            refund.setChannel(tradeService.getPayChannel(order.getPaytype()));

                            refund.setRefundAmount((backPay.subtract(penaltyPay).multiply(new BigDecimal(100))).setScale(2, BigDecimal.ROUND_HALF_UP).longValue());
                            refund.setRefundReason("订单退款");

                            BigDecimal totalAmout = tradeService.getTotalAmout(uuid);
                            refund.setTotalAmount((totalAmout.multiply(new BigDecimal(100))).setScale(2, BigDecimal.ROUND_HALF_UP).longValue());
                            boolean result = tradeService.backMoney(refund);

                            if (result) {
                                if (order.getPaytype() == Quantity.STATE_0) {
                                    buyerCapital1 = createBuyerBackPay(order, backPay.subtract(penaltyPay).setScale(2, BigDecimal.ROUND_HALF_UP), tranTime, Quantity.STATE_0);
                                } else if (order.getPaytype() == Quantity.STATE_1) {
                                    buyerCapital1 = createBuyerBackPay(order, backPay.subtract(penaltyPay).setScale(2, BigDecimal.ROUND_HALF_UP), tranTime, Quantity.STATE_1);
                                } else {
                                    buyerCapital1 = createBuyerBackPay(order, backPay.subtract(penaltyPay).setScale(2, BigDecimal.ROUND_HALF_UP), tranTime, Quantity.STATE_2);
                                }
                                salerCapital1 = createSalerBackPay(order, backPay, tranTime);
                                buyerCapitals.add(buyerCapital1);
                                salerCapitals.add(salerCapital1);
                            }
                        }
                    }


                    //异议扣违约金,记录资金明细
                    if (orderProductBack.getAdminstate() == Quantity.STATE_2) {
                        BigDecimal sellerPenaltyPay = penaltyPay.multiply(forwardsalesmargin).setScale(2, BigDecimal.ROUND_HALF_UP);
                        if (sellerPenaltyPay.compareTo(Quantity.BIG_DECIMAL_0) > 0) {
                            sellerMember.setSellerbanlance(sellerMember.getSellerbanlance().add(sellerPenaltyPay));
                            salerCapital2 = createSalerPenaltyPay(order, sellerPenaltyPay, tranTime, Quantity.STATE_6, orderPay, Quantity.BUYER_BACK_REASON);
                            salerCapitals.add(salerCapital2);
                        }

                        if (penaltyPay.compareTo(Quantity.BIG_DECIMAL_0) > 0) {
                            buyerCapital2 = createBuyerPenaltyPay(order, penaltyPay, tranTime, Quantity.STATE_6, orderPay, Quantity.BUYER_BACK_REASON);
                            buyerCapital2.setPaytype(order.getPaytype());
                            buyerCapitals.add(buyerCapital2);
                        }
                    }

                    //如果退货类型为全部退货   将运费加到卖家货款金额里
                    if (orderProductBack.getBacktype() == Quantity.STATE_1) {
                        sellerMember.setGoodsbanlance(sellerMember.getGoodsbanlance().add(orderProduct.getActualpayment().subtract(backPay)));
                    }
                }
            }


            //远期全款
            if (orderProduct.getProtype() == Quantity.STATE_1) {

                backPay = Quantity.BIG_DECIMAL_0;
                penaltyPay = Quantity.BIG_DECIMAL_0;

                //计算扣除违约金
                BigDecimal orderPay = orderProduct.getAllpay();

                //退回的金额,异议扣违约金
                if (orderProductBack.getAdminstate() == Quantity.STATE_2) {
                    //违约金
                    penaltyPay = orderPay.multiply(getLiquidated).setScale(2, BigDecimal.ROUND_HALF_UP);
                }

                backPay = orderPay;


                //判断退回到余额还是授信
                BuyerCapital buyerCapital = ordersService.getBuyerCapitalByNoType(orderProduct.getOrderno(), Quantity.STATE_9);
                if (buyerCapital != null) {
                    //退回到余额
                    if (buyerCapital.getPaytype() == Quantity.STATE_3) {
                        buyer.setBalance(buyer.getBalance().add(backPay).subtract(penaltyPay).setScale(2, BigDecimal.ROUND_HALF_UP));
                        buyerCapital1 = createBuyerBackPay(order, backPay.subtract(penaltyPay).setScale(2, BigDecimal.ROUND_HALF_UP), tranTime, Quantity.STATE_3);
                        salerCapital1 = createSalerBackPay(order, backPay, tranTime);
                        buyerCapitals.add(buyerCapital1);
                        salerCapitals.add(salerCapital1);
                    }
                    //退回到授信
                    if (buyerCapital.getPaytype() == Quantity.STATE_4) {
                        buyer.setAvailablelimit(buyer.getAvailablelimit().add(backPay.subtract(penaltyPay)).setScale(2, BigDecimal.ROUND_HALF_UP));
                        buyer.setUsedlimit(buyer.getUsedlimit().subtract(backPay.subtract(penaltyPay)).setScale(2, BigDecimal.ROUND_HALF_UP));
                        buyerCapital1 = createBuyerBackPay(order, backPay.subtract(penaltyPay).setScale(2, BigDecimal.ROUND_HALF_UP), tranTime, Quantity.STATE_4);
                        salerCapital1 = createSalerBackPay(order, backPay, tranTime);
                        buyerCapitals.add(buyerCapital1);
                        salerCapitals.add(salerCapital1);
                    }


                    //退回到支付宝或微信
                    if (buyerCapital.getPaytype() == Quantity.STATE_0 || buyerCapital.getPaytype() == Quantity.STATE_1 || buyerCapital.getPaytype() == Quantity.STATE_2) {
                        String uuid = order.getUuid();
                        Long ordertime = order.getOrdertime();
                        if (uuid != null) {
                            Refund refund = new Refund();
                            refund.setOutTradeNo(uuid);
                            refund.setDatetime(ordertime);
                            refund.setChannel(tradeService.getPayChannel(order.getPaytype()));
                            refund.setRefundAmount((backPay.subtract(penaltyPay).setScale(2, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100))).longValue());
                            refund.setRefundReason("订单退款");
                            BigDecimal totalAmout = tradeService.getTotalAmout(uuid);
                            refund.setTotalAmount((totalAmout.multiply(new BigDecimal(100))).longValue());
                            boolean result = tradeService.backMoney(refund);

                            if (result) {
                                if (order.getPaytype() == Quantity.STATE_0) {
                                    buyerCapital1 = createBuyerBackPay(order, backPay.subtract(penaltyPay).setScale(2, BigDecimal.ROUND_HALF_UP), tranTime, Quantity.STATE_0);
                                } else if (order.getPaytype() == Quantity.STATE_1) {
                                    buyerCapital1 = createBuyerBackPay(order, backPay.subtract(penaltyPay).setScale(2, BigDecimal.ROUND_HALF_UP), tranTime, Quantity.STATE_1);
                                } else {
                                    buyerCapital1 = createBuyerBackPay(order, backPay.subtract(penaltyPay).setScale(2, BigDecimal.ROUND_HALF_UP), tranTime, Quantity.STATE_2);
                                }
                                salerCapital1 = createSalerBackPay(order, backPay, tranTime);
                                buyerCapitals.add(buyerCapital1);
                                salerCapitals.add(salerCapital1);
                            }

                        }
                    }

                    //异议扣违约金,记录资金明细
                    if (orderProductBack.getAdminstate() == Quantity.STATE_2) {
                        BigDecimal sellerPenaltyPay = penaltyPay.multiply(forwardsalesmargin).setScale(2, BigDecimal.ROUND_HALF_UP);
                        if (sellerPenaltyPay.compareTo(Quantity.BIG_DECIMAL_0) > 0) {
                            sellerMember.setSellerbanlance(sellerMember.getSellerbanlance().add(sellerPenaltyPay));
                            salerCapital2 = createSalerPenaltyPay(order, sellerPenaltyPay, tranTime, Quantity.STATE_6, orderPay, Quantity.BUYER_BACK_REASON);
                            salerCapitals.add(salerCapital2);
                        }

                        if (penaltyPay.compareTo(Quantity.BIG_DECIMAL_0) > 0) {
                            buyerCapital2 = createBuyerPenaltyPay(order, penaltyPay, tranTime, Quantity.STATE_6, orderPay, Quantity.BUYER_BACK_REASON);
                            buyerCapital2.setPaytype(order.getPaytype());
                            buyerCapitals.add(buyerCapital2);
                        }
                    }


                    //如果退货类型为全部退货   将运费加到卖家货款金额里
                    if (orderProductBack.getBacktype() == Quantity.STATE_1) {
                        sellerMember.setGoodsbanlance(sellerMember.getGoodsbanlance().add(orderProduct.getActualpayment().subtract(backPay)));
                    }
                }
            }


            //远期余款
            if (orderProduct.getProtype() == Quantity.STATE_2) {
                //计算扣除违约金
                BigDecimal orderPay = orderProduct.getActualpayment().subtract(orderProduct.getFreight());


                BigDecimal partPay = orderProduct.getPartpay();
                BigDecimal yuPay = orderProduct.getYupay();


                //预付款违约金
                BigDecimal partPaypenal = Quantity.BIG_DECIMAL_0;
                //余款违约金
                BigDecimal yuPayPenal = Quantity.BIG_DECIMAL_0;

                //定金支付明细
                BuyerCapital depositBuyerCapital = ordersService.getBuyerCapitalByNoType(order.getOrderno(), Quantity.STATE_7);

                if (depositBuyerCapital != null) {

                    //异议扣违约金
                    //退回的金额,异议扣违约金
                    if (orderProductBack.getAdminstate() == Quantity.STATE_2) {
                        //违约金
                        partPaypenal = partPay.multiply(getLiquidated).setScale(2, BigDecimal.ROUND_HALF_UP);
                        yuPayPenal = yuPay.multiply(getLiquidated).setScale(2, BigDecimal.ROUND_HALF_UP);
                    }


                    //买家退款金额
                    BigDecimal buyerBackPay = partPay.subtract(partPaypenal).add(yuPay.subtract(yuPayPenal)).setScale(2, BigDecimal.ROUND_HALF_UP);

                    //卖家退款金额
                    BigDecimal salerBackPay = partPay.add(yuPay).setScale(2, BigDecimal.ROUND_HALF_UP);


                    //退回到余额
                    if (depositBuyerCapital.getPaytype() == Quantity.STATE_3) {
                        buyer.setBalance(buyer.getBalance().add(buyerBackPay));
                        buyerCapital1 = createBuyerBackPay(order, buyerBackPay, tranTime, Quantity.STATE_3);
                        salerCapital1 = createSalerBackPay(order, salerBackPay, tranTime);
                        buyerCapitals.add(buyerCapital1);
                        salerCapitals.add(salerCapital1);
                    }


                    //退回到授信
                    if (depositBuyerCapital.getPaytype() == Quantity.STATE_4) {
                        buyer.setAvailablelimit(buyer.getAvailablelimit().add(buyerBackPay));
                        buyer.setUsedlimit(buyer.getUsedlimit().subtract(buyerBackPay));
                        buyerCapital1 = createBuyerBackPay(order, buyerBackPay, tranTime, Quantity.STATE_4);
                        salerCapital1 = createSalerBackPay(order, salerBackPay, tranTime);
                        buyerCapitals.add(buyerCapital1);
                        salerCapitals.add(salerCapital1);
                    }
                    //退回到支付宝或微信
                    if (depositBuyerCapital.getPaytype() == Quantity.STATE_0 || depositBuyerCapital.getPaytype() == Quantity.STATE_1 || depositBuyerCapital.getPaytype() == Quantity.STATE_2) {
                        String uuid = order.getUuid();
                        String yuuuid = order.getYuuuid();
                        Long ordertime = order.getOrdertime();
                        Long yuordertime = order.getYuordertime();
                        if (uuid != null && yuuuid != null && ordertime != null && yuordertime != null) {
                            //定金
                            Refund refund1 = new Refund();
                            //余款
                            Refund refund2 = new Refund();
                            refund1.setOutTradeNo(uuid);
                            refund1.setDatetime(ordertime);
                            refund2.setOutTradeNo(yuuuid);
                            refund2.setDatetime(yuordertime);

                            if (order.getPaytype() == Quantity.STATE_0) {
                                refund1.setChannel("alipay");
                                refund2.setChannel("alipay");
                            } else if (order.getPaytype() == Quantity.STATE_1) {
                                refund1.setChannel("wxpay");
                                refund2.setChannel("wxpay");
                            } else {
                                refund1.setChannel("bank");
                                refund2.setChannel("bank");
                            }
                            refund1.setRefundAmount((partPay.subtract(partPaypenal).multiply(new BigDecimal(100))).longValue());
                            refund2.setRefundAmount((yuPay.subtract(yuPayPenal).multiply(new BigDecimal(100))).longValue());

                            refund1.setRefundReason("订单定金退款");
                            refund2.setRefundReason("订单余款退款");
                            List<Orders> ordersList = ordersService.getOrdersByUUID(uuid);
                            BigDecimal totalPartPay = new BigDecimal(0);
                            BigDecimal totalYuPay = new BigDecimal(0);
                            for (Orders od : ordersList) {
                                totalPartPay = totalPartPay.add(od.getDeposit());
                                totalYuPay = totalYuPay.add(od.getBalance());
                            }
                            refund1.setTotalAmount((totalPartPay.multiply(new BigDecimal(100))).longValue());
                            refund2.setTotalAmount((totalYuPay.multiply(new BigDecimal(100))).longValue());

                            boolean result1 = false;
                            boolean result2 = false;
                            try {
                                if ("alipay".equals(refund1.getChannel())) {
                                    result1 = alipayService.refund(refund1);
                                    result2 = alipayService.refund(refund2);
                                } else if ("wxpay".equals(refund1.getChannel())) {
                                    result1 = wxPayService.refund(refund1);
                                    result2 = wxPayService.refund(refund2);
                                } else {
                                    result1 = abcService.refund(refund1);
                                    result2 = abcService.refund(refund2);
                                }
                            } catch (Exception e) {

                            }
                            if (result1 && result2) {
                                if (order.getPaytype() == Quantity.STATE_0) {
                                    buyerCapital1 = createBuyerBackPay(order, buyerBackPay, tranTime, Quantity.STATE_0);
                                } else if (order.getPaytype() == Quantity.STATE_1) {
                                    buyerCapital1 = createBuyerBackPay(order, buyerBackPay, tranTime, Quantity.STATE_1);
                                } else {
                                    buyerCapital1 = createBuyerBackPay(order, buyerBackPay, tranTime, Quantity.STATE_2);
                                }
                                salerCapital1 = createSalerBackPay(order, salerBackPay, tranTime);
                                buyerCapitals.add(buyerCapital1);
                                salerCapitals.add(salerCapital1);
                            }

                        }
                    }

                    //异议扣违约金,记录资金明细
                    if (orderProductBack.getAdminstate() == Quantity.STATE_2) {
                        penaltyPay = partPaypenal.add(yuPayPenal);
                        BigDecimal sellerPenaltyPay = penaltyPay.multiply(forwardsalesmargin).setScale(2, BigDecimal.ROUND_HALF_UP);
                        if (sellerPenaltyPay.compareTo(Quantity.BIG_DECIMAL_0) > 0) {
                            sellerMember.setSellerbanlance(sellerMember.getSellerbanlance().add(sellerPenaltyPay));
                            salerCapital2 = createSalerPenaltyPay(order, sellerPenaltyPay, tranTime, Quantity.STATE_6, orderPay, Quantity.BUYER_BACK_REASON);
                            salerCapitals.add(salerCapital2);
                        }

                        if (penaltyPay.compareTo(Quantity.BIG_DECIMAL_0) > 0) {
                            buyerCapital2 = createBuyerPenaltyPay(order, penaltyPay, tranTime, Quantity.STATE_6, orderPay, Quantity.BUYER_BACK_REASON);
                            buyerCapital2.setPaytype(order.getPaytype());
                            buyerCapitals.add(buyerCapital2);
                        }
                    }

                    //如果退货类型为全部退货   将运费加到卖家货款金额里
                    if (orderProductBack.getBacktype() == Quantity.STATE_1) {
                        sellerMember.setGoodsbanlance(sellerMember.getGoodsbanlance().add(orderProduct.getActualpayment().subtract(partPay).subtract(yuPay)));
                    }
                }
            }


            //保存用户余额和授信
            ordersService.saveMember(buyer, oldBuyer);
            ordersService.saveMember(sellerMember, oldSellerMember);

            if (buyerCapitals.size() > 0) {

                ordersService.insertBuyerCapital(buyerCapitals);
                //保存操作日志
                OperateLog operateLog = new OperateLog();
                operateLog.setContent("退款成功");

                operateLog.setOpid(operatorId);

                operateLog.setOpname(operatorUsername);

                operateLog.setOptime(new Date());
                operateLog.setOptype(Quantity.STATE_1);
                operateLog.setOrderid(orderProductBack.getOrderid());
                operateLog.setOrderno(orderProductBack.getOrderno());
                operateLog.setOrderpdid(orderProduct.getId());
                ordersService.saveOperatelog(operateLog);
            }

            if (salerCapitals.size() > 0) {
                ordersService.insertSallerCapital(salerCapitals);
            }
        }
    }


    /**
     * 创建买家退款资金明细
     *
     * @param order
     * @param backPay
     * @param tranTime
     * @param type     3=退到余额4=退到授信
     * @return
     */
    private BuyerCapital createBuyerBackPay(Orders order, BigDecimal backPay, Date tranTime, Short type) {

        BuyerCapital buyerCapital = new BuyerCapital();
        buyerCapital.setOrderno(order.getOrderno());
        buyerCapital.setTradeno(order.getTransactionnumber());
        buyerCapital.setCapitaltype(Quantity.STATE_2);
        buyerCapital.setCapital(backPay);
        buyerCapital.setPaytype(type);
        buyerCapital.setMemberid(order.getMemberid());
        buyerCapital.setTradetime(tranTime);
        buyerCapital.setRemark("退款金额");
        return buyerCapital;
    }

    /**
     * 创建买家违约金资金明细
     *
     * @param order
     * @param penaltyPay
     * @param tranTime
     * @param type       6=买家违约10=卖家违约
     * @return
     */
    private BuyerCapital createBuyerPenaltyPay(Orders order, BigDecimal penaltyPay, Date tranTime, Short type, BigDecimal orderPay, String reason) {
        BuyerCapital buyerCapital = new BuyerCapital();
        buyerCapital.setOrderno(order.getOrderno());
        buyerCapital.setTradeno(order.getTransactionnumber());
        buyerCapital.setCapitaltype(type);
        buyerCapital.setCapital(penaltyPay);
        buyerCapital.setMemberid(order.getMemberid());
        buyerCapital.setTradetime(tranTime);
        buyerCapital.setAllpay(orderPay);
        buyerCapital.setRemark(reason);
        buyerCapital.setSellerid(order.getSaleid());
        if (type == Quantity.STATE_6) {
            buyerCapital.setRemark("买家违约金额");
        } else {
            buyerCapital.setRemark("卖家违约金额");
        }
        return buyerCapital;
    }

    /**
     * 创建卖家退款资金明细
     *
     * @param order
     * @param backPay
     * @param tranTime
     * @return
     */
    private SalerCapital createSalerBackPay(Orders order, BigDecimal backPay, Date tranTime) {
        SalerCapital salerCapital = new SalerCapital();
        salerCapital.setMemberid(order.getSaleid());
        salerCapital.setTradeno(order.getTransactionnumber());
        salerCapital.setOrderno(order.getOrderno());
        salerCapital.setTradetime(tranTime);
        salerCapital.setRefundamount(backPay);
        salerCapital.setCapitaltype(Quantity.STATE_3);
        salerCapital.setRemark("退款金额");

        //卖家从冻结金额中减去退回金额
//        Member seller = ordersService.getById(order.getSaleid());
//        seller.setSellerfreezebanlance(seller.getSellerfreezebanlance().subtract(backPay));
//        ordersService.saveMember(seller);

        //钱还未到卖家账号
        //memberService.updateSellerMemberBalanceInDb(order.getSaleid(), Quantity.BIG_DECIMAL_0, backPay.multiply(Quantity.BIG_DECIMAL_MINUS_1));


        return salerCapital;
    }

    /**
     * 创建卖家违约金资金明细
     *
     * @param order
     * @param penaltyPay
     * @param tranTime
     * @param type       6=买家违约7=卖家违约
     * @return
     */
    private SalerCapital createSalerPenaltyPay(Orders order, BigDecimal penaltyPay, Date tranTime, Short type, BigDecimal orderPay, String reason) {
        SalerCapital salerCapital = new SalerCapital();
        salerCapital.setMemberid(order.getSaleid());
        salerCapital.setTradeno(order.getTransactionnumber());
        salerCapital.setOrderno(order.getOrderno());
        salerCapital.setTradetime(tranTime);
        salerCapital.setPenalty(penaltyPay);
        salerCapital.setCapitaltype(type);
        salerCapital.setAllpay(orderPay);
        salerCapital.setRemark(reason);
        salerCapital.setBuyerid(order.getMemberid());
        if (type == Quantity.STATE_6) {
            salerCapital.setRemark("买家违约金额");
        } else {
            salerCapital.setRemark("卖家违约金额");
        }
        return salerCapital;
    }

    @RequestMapping(value = "/updateBillRecord", method = RequestMethod.POST)
    @ApiOperation(value = "更新开票申请记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "开票申请记录id", required = true, paramType = "query", dataType = "long"),
            @ApiImplicitParam(name = "state", value = "开票状态0=待开票1=未收到2=确认", required = true, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "expressno", value = "物流单号", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "expresscom", value = "物流公司", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "billtype", value = "开票类型", required = false, paramType = "query", dataType = "string"),
    })
    public BasicRet updateBillRecord(Model model, BillingRecord billingRecord, HttpServletRequest request) {
        BasicRet basicRet = new BasicRet();
        ordersService.updateBillRecord(billingRecord);
        if (billingRecord.getState()==Quantity.STATE_1){
            BuyerStatement buyerStatement=new BuyerStatement();
            BillingRecord billingRecord1=ordersService.getBillRecordByID(billingRecord.getId());
            buyerStatement.setMemberid(billingRecord1.getMemberid());
            buyerStatement.setContractno(billingRecord1.getBillno());
            buyerStatement.setType((short) StatementType.StType8.getTyep());
            buyerStatement.setBillrecoid(billingRecord1.getId());
            buyerStatement.setDeliveryamount(Quantity.BIG_DECIMAL_0);
            buyerStatement.setReceiptamount(Quantity.BIG_DECIMAL_0);
            buyerStatement.setInvoiceamount(billingRecord1.getBillcash());
            buyerStatement.setCreatetime(new Date());
            buyerStatement.setRemark(billingRecord1.getInvoiceheadup());
            buyerStatement.setPaytype((short)6);
            statementService.insertStatement(buyerStatement);
        }
        basicRet.setResult(BasicRet.SUCCESS);
        basicRet.setMessage("修改成功");
        Admin admin = (Admin) model.asMap().get(AppConstant.ADMIN_SESSION_NAME);
        //保存用户日志
        memberLogOperator.saveMemberLog(null, admin, "更新开票申请记录，开票申请记录id:" + billingRecord.getId(), "/rest/admin/orders/updateBillRecord", request, memberOperateLogService);
        return basicRet;
    }

    @RequestMapping(value = "/batchUpdateBillRecord", method = RequestMethod.POST)
    @ApiOperation(value = "批量更新开票申请记录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "开票申请记录ids", required = true, paramType = "query", dataType = "long"),
            @ApiImplicitParam(name = "state", value = "开票状态0=待开票1=未收到2=确认", required = true, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "expressno", value = "物流单号", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "expresscom", value = "物流公司", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "billtype", value = "开票类型", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "billno", value = "开票号", required = false, paramType = "query", dataType = "string"),
    })
    public BasicRet batchUpdateBillRecord(Model model, HttpServletRequest request, String ids, Short state, String expressno, String expresscom, String billtype, String billno) {
        BasicRet basicRet = new BasicRet();
        List<BuyerStatement> statementList=new ArrayList<>();
        BuyerStatement buyerStatement=null;
        Map<String,List<BillingRecord>> listMap = new LinkedHashMap<>();
        List<BillingRecord> billingRecords=null;
        List<BillingRecord> list = ordersService.getBillRecordList(ids);
        for (BillingRecord billingRecord : list) {
            billingRecord.setExpressno(expressno);
            billingRecord.setExpresscom(expresscom);
            billingRecord.setState(state);
            billingRecord.setBilltype(billtype);
            billingRecord.setBillno(billno);
            ordersService.updateBillRecord(billingRecord);
            if (listMap.containsKey(billingRecord.getInvoiceheadup())){
                billingRecords=listMap.get(billingRecord.getInvoiceheadup());
                billingRecords.add(billingRecord);
            }else{
                billingRecords=new ArrayList<>();
                billingRecords.add(billingRecord);
                listMap.put(billingRecord.getInvoiceheadup(),billingRecords);
            }
        }
        for (Map.Entry<String,List<BillingRecord>> entry:listMap.entrySet()){
            final BigDecimal[] billcash = {Quantity.BIG_DECIMAL_0};
            String invoiceHeadUp=entry.getKey();
            entry.getValue().stream().forEach(bg -> {
                billcash[0] = billcash[0].add(bg.getBillcash());
            });
            buyerStatement=new BuyerStatement();
            BillingRecord example=entry.getValue().get(0);
            buyerStatement.setMemberid(example.getMemberid());
            buyerStatement.setContractno(example.getBillno());
            buyerStatement.setType((short) StatementType.StType8.getTyep());
            buyerStatement.setBillrecoid(example.getId());
            buyerStatement.setDeliveryamount(Quantity.BIG_DECIMAL_0);
            buyerStatement.setReceiptamount(Quantity.BIG_DECIMAL_0);
            buyerStatement.setInvoiceamount(billcash[0]);
            buyerStatement.setCreatetime(new Date());
            buyerStatement.setRemark(invoiceHeadUp);
            buyerStatement.setPaytype((short)6);
            statementList.add(buyerStatement);
        }
        if (statementList.size()>0){
            statementService.insertStatementAll(statementList);
        }
        basicRet.setResult(BasicRet.SUCCESS);
        basicRet.setMessage("修改成功");
        Admin admin = (Admin) model.asMap().get(AppConstant.ADMIN_SESSION_NAME);
        //保存用户日志
        memberLogOperator.saveMemberLog(null, admin, "批量更新开票申请记录，开票申请记录id:" + ids, "/rest/admin/orders/batchUpdateBillRecord", request, memberOperateLogService);
        return basicRet;
    }

    /**
     * 获取开票列表
     *
     * @param model
     * @param billQueryParam
     * @param pageNo
     * @param pageSize
     * @return
     */
    @RequestMapping(value = "/getBillRecordList", method = RequestMethod.POST)
    @ApiOperation(value = "获取开票列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageNo", value = "页码", required = true, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "pageSize", value = "页数", required = true, paramType = "query", dataType = "int"),
    })
    public PageRet getBillRecordList(Model model, BillQueryParam billQueryParam, int pageNo, int pageSize) {
        PageRet pageRet = new PageRet();
        pageRet.setMessage("返回成功");
        pageRet.setResult(BasicRet.SUCCESS);
        Member member = (Member) model.asMap().get(AppConstant.MEMBER_SESSION_NAME);
        if (member != null) {
            billQueryParam.setMemberid(member.getId());
            pageRet.data.setPageInfo(ordersService.getBillRecordList(billQueryParam, pageNo, pageSize));
            return pageRet;
        } else {
            pageRet.data.setPageInfo(ordersService.getBillRecordList(billQueryParam, pageNo, pageSize));
            return pageRet;
        }
    }


    @RequestMapping(value = "/loadSellerBillRecordList", method = RequestMethod.POST)
    @ApiOperation("获取卖家开票列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "startTime", value = "开始时间", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "endTime", value = "结束时间", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "billno", value = "发票号", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "sellerid", value = "卖家编号", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "sellername", value = "卖家名称", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "diliveryno", value = "物流单号", required = false, paramType = "query", dataType = "string"),
    })
    public PageRet loadSellerBillRecordList(Model model, int pageNo, int pageSize, SellerBillRecordQueryParam param) {
        Member member = (Member) model.asMap().get(AppConstant.MEMBER_SESSION_NAME);
        PageRet pageRet = new PageRet();
        PageInfo pageInfo = ordersService.getSellerBillRecord(pageNo, pageSize, param, member);
        pageRet.data.setPageInfo(pageInfo);
        pageRet.setResult(BasicRet.SUCCESS);
        return pageRet;
    }

    @RequestMapping(value = "/updateSellerBillRecord", method = RequestMethod.POST)
    @ApiOperation("修改卖家开票")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "开票申请id", required = false, paramType = "query", dataType = "long"),
            @ApiImplicitParam(name = "state", value = "状态0=未确认1=确认", required = false, paramType = "query", dataType = "int"),

    })
    public BasicRet updateSellerBillRecord(Model model, SellerBillRecord sellerBillRecord, HttpServletRequest request) {
        BasicRet basicRet = new BasicRet();
        ordersService.updateSellerBillRecord(sellerBillRecord);
        basicRet.setResult(BasicRet.SUCCESS);
        basicRet.setMessage("修改成功");
        Admin admin = (Admin) model.asMap().get(AppConstant.ADMIN_SESSION_NAME);
        //保存用户日志
        memberLogOperator.saveMemberLog(null, admin, "修改卖家开票，开票申请记录id:" + sellerBillRecord.getId(), "/rest/admin/orders/updateSellerBillRecord", request, memberOperateLogService);
        return basicRet;
    }

    @RequestMapping(value = "/getOrdersByBillRecordId", method = RequestMethod.POST)
    @ApiOperation(value = "根据记录id获取开票信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "开票申请记录id", required = true, paramType = "query", dataType = "long"),
    })
    public OrderCarRet getOrdersByBillRecordId(Long id) {
        OrderCarRet orderCarRet = new OrderCarRet();
        orderCarRet.setResult(BasicRet.SUCCESS);
        orderCarRet.setMessage("返回成功");
        BillingRecord billingRecord = ordersService.getBillRecordByID(id);
        BillRecordComplex billRecordComplex = new BillRecordComplex();
        if (billingRecord != null) {
            if (billingRecord.getBillno() != null) {
                billRecordComplex.setBillNo(billingRecord.getBillno());
            }
            if (billingRecord.getBilltime() != null) {
                billRecordComplex.setBillTime(billingRecord.getCreatetime());
            }
            if (billingRecord.getBilltype() != null) {
                billRecordComplex.setBillType(billingRecord.getBilltype());
            }
            if (billingRecord.getExpressno() != null) {
                billRecordComplex.setExpressNo(billingRecord.getExpressno());
            }
            if (billingRecord.getBillingrecordtype() != null) {
                billRecordComplex.setBillingrecordtype(billingRecord.getBillingrecordtype());
            }
            if (billingRecord.getExpresscom() != null) {
                billRecordComplex.setExpressCompany(billingRecord.getExpresscom());
            }
            billRecordComplex.setMemberId(billingRecord.getMemberid());
            billRecordComplex.setMemberName(billingRecord.getMembername());

            billRecordComplex.setAddress(billingRecord.getAddress());
            billRecordComplex.setReceiveaddress(billingRecord.getReceiveaddress());


            String[] ordernoArray = billingRecord.getOrderno().split(",");
            List<Orders> list = new ArrayList<Orders>();
            for (String orderid : ordernoArray) {
                Orders orders = ordersService.getOrdersById(Long.parseLong(orderid));


                //查询是否有退货或退款的，如果有退货开票金额要减去退货的钱
                List<OrderProductBack> orderProductBackList = orderProductBackService.getByOrderNo(orders.getOrderno());
                BigDecimal subApply = new BigDecimal(0);
                for (OrderProductBack opb : orderProductBackList) {
                    if (opb.getState() == 10) {
                        subApply = subApply.add(opb.getBackmoney());
                    }
                }

                if (subApply.compareTo(new BigDecimal(0)) > 0) {
                    orders.setTotalprice(orders.getTotalprice().subtract(subApply).subtract(orders.getDiscountprice()));
                }


                if (orders != null) {
                    list.add(orders);
                }
            }
            billRecordComplex.setList(list);
        }
        orderCarRet.data.billRecordComplex = billRecordComplex;
        return orderCarRet;
    }

    @RequestMapping(value = "/getOrdersByBillRecordIds", method = RequestMethod.POST)
    @ApiOperation(value = "根据记录ids获取开票信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "批量开票申请记录id集合", required = true, paramType = "query", dataType = "string"),
    })
    public OrderCarRet getOrdersByBillRecordIds(String ids) {
        OrderCarRet orderCarRet = new OrderCarRet();
        orderCarRet.setResult(BasicRet.SUCCESS);
        orderCarRet.setMessage("返回成功");
        List<BillingRecord> billingRecords = ordersService.getBillRecordList(ids);
        List<BillRecordComplex> listComplex = new ArrayList<>();
        for (BillingRecord billingRecord : billingRecords) {
            BillRecordComplex billRecordComplex = new BillRecordComplex();

            if (billingRecord.getBillno() != null) {
                billRecordComplex.setBillNo(billingRecord.getBillno());
            }
            if (billingRecord.getBilltime() != null) {
                billRecordComplex.setBillTime(billingRecord.getCreatetime());
            }
            if (billingRecord.getBilltype() != null) {
                billRecordComplex.setBillType(billingRecord.getBilltype());
            }
            if (billingRecord.getExpressno() != null) {
                billRecordComplex.setExpressNo(billingRecord.getExpressno());
            }
            if (billingRecord.getBillingrecordtype() != null) {
                billRecordComplex.setBillingrecordtype(billingRecord.getBillingrecordtype());
            }
            if (billingRecord.getExpresscom() != null) {
                billRecordComplex.setExpressCompany(billingRecord.getExpresscom());
            }
            billRecordComplex.setMemberId(billingRecord.getMemberid());
            billRecordComplex.setMemberName(billingRecord.getMembername());
            String[] ordernoArray = billingRecord.getOrderno().split(",");
            List<Orders> list = new ArrayList<Orders>();
            for (String orderid : ordernoArray) {
                Orders orders = ordersService.getOrdersById(Long.parseLong(orderid));
                if (orders != null) {
                    list.add(orders);
                }

            }
            billRecordComplex.setList(list);
            listComplex.add(billRecordComplex);
        }

        orderCarRet.data.listComplex = listComplex;
        return orderCarRet;
    }


    @RequestMapping(value = "/getProductEvaList", method = RequestMethod.POST)
    @ApiOperation(value = "获取评价列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageNo", value = "页码", required = true, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "pageSize", value = "页数", required = true, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "startTime", value = "开始时间", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "endTime", value = "结束时间", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "orderNo", value = "订单编号", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "sellerName", value = "所属商家", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "memberName", value = "评价人", required = false, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "pdName", value = "商品名称", required = false, paramType = "query", dataType = "string"),

    })
    public PageRet getProductEvaList(OrderQueryParam param) {
        PageRet orderCarRet = new PageRet();

        PageInfo pageInfo = ordersService.getOrderProductEva(param);

        orderCarRet.data.setPageInfo(pageInfo);

        orderCarRet.setMessage("返回成功");
        orderCarRet.setResult(BasicRet.SUCCESS);
        return orderCarRet;
    }

    @RequestMapping(value = "/deleteProductEva", method = RequestMethod.POST)
    @ApiOperation(value = "删除评价")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "orderProductId", value = "评价id", required = true, paramType = "query", dataType = "long"),
    })
    public BasicRet deleteProductEva(Model model, Long orderProductId, HttpServletRequest request) {
        BasicRet orderCarRet = new BasicRet();
        ordersService.deleteMemberEva(orderProductId);
        orderCarRet.setMessage("删除成功");
        orderCarRet.setResult(BasicRet.SUCCESS);
        Admin admin = (Admin) model.asMap().get(AppConstant.ADMIN_SESSION_NAME);
        //保存用户日志
        memberLogOperator.saveMemberLog(null, admin, "删除评价,id为：" + orderProductId, "/rest/admin/orders/deleteProductEva", request, memberOperateLogService);
        return orderCarRet;
    }


    @RequestMapping(value = "/getOrderOperateLog", method = RequestMethod.POST)
    @ApiOperation(value = "获取操作日志")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "orderid", value = "订单id", required = true, paramType = "query", dataType = "long"),
            @ApiImplicitParam(name = "type", value = "操作类型0=订单操作1=退货操作", required = true, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "orderpdid", value = "订单商品id", required = false, paramType = "query", dataType = "long"),
    })
    public OrderCarRet getOrderOperateLog(Long orderid, Short type, Long orderpdid) {
        OrderCarRet orderCarRet = new OrderCarRet();

        List<OperateLog> list = ordersService.getOperatelog(orderid, type, orderpdid);


        List<OperateLog> olList = new ArrayList<>();

        List<OperateLog> operateLogfinalList = new ArrayList<>();

        for (OperateLog ol : list) {
            if (!ol.getContent().equals("订单已收货") && !ol.getContent().equals("订单已验货")) {
                olList.add(ol);
            } else {
                operateLogfinalList.add(ol);
            }
        }


        Orders orders = ordersService.getOrdersById(orderid);
        if (type == 0 && orders.getOrderstatus() != 0) {

            try {
                OrderStoreStateLog orderStoreStateLog = orderStoreStateLogService.getStoreState(orders);

                if (orderStoreStateLog != null && orderStoreStateLog.getList() != null) {
                    Collections.reverse(orderStoreStateLog.getList());
                    for(StoreState storeState : orderStoreStateLog.getList()){
                        OperateLog operateLog = new OperateLog();
                        operateLog.setOpname(storeState.getOperateUser());
                        operateLog.setOrderid(orderStoreStateLog.getOrderid());
                        operateLog.setContent(storeState.getPointName());
                        operateLog.setOptime(DateUtils.StrToDate(storeState.getCreateTime()));
                        olList.add(operateLog);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        olList.addAll(operateLogfinalList);

        orderCarRet.data.operateLogs = olList;
        orderCarRet.setMessage("返回成功");
        orderCarRet.setResult(BasicRet.SUCCESS);
        return orderCarRet;
    }


    @RequestMapping(value = "/saveBillType", method = RequestMethod.POST)
    @ApiOperation(value = "保存发票内容")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "name", value = "发票内容", required = true, paramType = "query", dataType = "string"),
    })
    public BasicRet saveBillType(Model model, BillType billType, HttpServletRequest request) {
        BasicRet basicRet = new BasicRet();

        ordersService.saveBillType(billType);

        basicRet.setResult(BasicRet.SUCCESS);
        basicRet.setMessage("保存成功");

        Admin admin = (Admin) model.asMap().get(AppConstant.ADMIN_SESSION_NAME);
        //保存用户日志
        memberLogOperator.saveMemberLog(null, admin, "保存发票内容", "/rest/admin/orders/saveBillType", request, memberOperateLogService);

        return basicRet;
    }

    @RequestMapping(value = "/getBillTypeList", method = RequestMethod.POST)
    @ApiOperation(value = "获取发票类型")
    public OrderCarRet getBillTypeList() {
        OrderCarRet orderCarRet = new OrderCarRet();
        List<BillType> list = ordersService.getBillTypeList();
        orderCarRet.data.billTypes = list;
        orderCarRet.setResult(BasicRet.SUCCESS);
        orderCarRet.setMessage("返回成功");
        return orderCarRet;
    }

    @RequestMapping(value = "/deleteBillType", method = RequestMethod.POST)
    @ApiOperation(value = "删除发票类型")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "id", required = true, paramType = "query", dataType = "int"),
    })
    public BasicRet deleteBillType(Model model, Integer id, HttpServletRequest request) {
        BasicRet basicRet = new BasicRet();
        ordersService.deleteBillTyep(id);
        basicRet.setResult(BasicRet.SUCCESS);
        basicRet.setMessage("删除成功");
        Admin admin = (Admin) model.asMap().get(AppConstant.ADMIN_SESSION_NAME);
        //保存用户日志
        memberLogOperator.saveMemberLog(null, admin, "删除发票内容", "/rest/admin/orders/deleteBillType", request, memberOperateLogService);
        return basicRet;
    }

    /**
     * 保存操作日志
     *
     * @param memberOperateLog
     * @return
     */
    @RequestMapping(value = "/saveMemberLog", method = RequestMethod.POST)
    @ApiOperation(value = "保存操作日志")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ipaddress", value = "ip地址", required = true, paramType = "query", dataType = "string"),
            @ApiImplicitParam(name = "content", value = "操作内容", required = true, paramType = "query", dataType = "string"),
    })
    public BasicRet saveMemberLog(Model model, MemberOperateLog memberOperateLog) {
        BasicRet basicRet = new BasicRet();
        Admin member = (Admin) model.asMap().get(AppConstant.ADMIN_SESSION_NAME);
        memberOperateLog.setMemberid(member.getId());
        memberOperateLog.setMembername(member.getUsername());
        memberOperateLog.setCreatetime(new Date());
        memberOperateLogService.saveMemberOperateLog(memberOperateLog);
        basicRet.setResult(BasicRet.SUCCESS);
        basicRet.setMessage("保存成功");
        return basicRet;
    }

    /**
     * 获取当前订单总的销售额
     *
     * @return
     */
    @RequestMapping(value = "/getCurrentOrdersSumPay", method = RequestMethod.POST)
    @ApiOperation(value = "获取当前订单总的销售额")
    public OrderCarRet getCurrentOrdersSumPay() {
        OrderCarRet orderCarRet = new OrderCarRet();
        orderCarRet.data.bigDecimal = ordersService.getCurrentOrdersSumPay();
        orderCarRet.setMessage("返回成功");
        orderCarRet.setResult(BasicRet.SUCCESS);
        return orderCarRet;
    }



    @RequestMapping(value = "/getOrderProductBackByOrderNo", method = RequestMethod.POST)
    @ApiOperation(value = "根据订单编号获取退货商品信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "orderno", value = "订单编号", required = true, paramType = "query", dataType = "string"),
    })
    @PreAuthorize("hasAuthority('" + AdminAuthorityConst.ORDERMANAGEMENT + "')")
    public OrderCarBackRet getOrderProductBackByOrderNo(String orderno) {
        OrderCarBackRet orderCarBackRet = new OrderCarBackRet();
        orderCarBackRet.setMessage("返回成功");
        orderCarBackRet.setResult(BasicRet.SUCCESS);
        List<OrderProductBackInfo> orderProductBackInfos = orderProductBackInfoService.getOrderProductBackByOrderNo(orderno);
        for (OrderProductBackInfo orderProductBackInfo : orderProductBackInfos) {
            List<OrderProduct> orderProducts = ordersService.getOrderProductByOrderNo(orderProductBackInfo.getOrderno());
            /*List<OrderProduct> list = new ArrayList<OrderProduct>();*/
            for (OrderProduct orderProduct : orderProducts) {
                if (orderProductBackInfo.getPdid() != null && orderProductBackInfo.getPdid().equals(orderProduct.getPdid())) {
                    /* list.add(orderProduct);*/
                    orderProductBackInfo.setOrderProduct(orderProduct);
                }
            }
        }
        orderCarBackRet.data.orderProductBackInfos = orderProductBackInfos;
        orderCarBackRet.data.orderProductBackInfos.forEach(orderProductBackInfo -> orderProductBackInfo.getExtend().put("productInfo", productInfoService.getById(orderProductBackInfo.getPdid())));
        return orderCarBackRet;
    }


    @RequestMapping(value = "/updateOrderProductPrice", method = RequestMethod.POST)
    @ApiOperation(value = "修改订单商品单价")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "orderid", value = "订单id", required = true, paramType = "query", dataType = "int"),
            //@ApiImplicitParam(name = "totalPrice",value = "订单总价",required = true,paramType = "query",dataType = "double"),
            @ApiImplicitParam(name="pricesJson",value = "订单商品单价json [{\"id\":1,\"price\":20}]", required = true,paramType = "query",dataType = "string")
    })
    public BasicRet updateOrderProductPrice(@RequestParam(required = true) long orderid,
                                            //@RequestParam(required = true) BigDecimal totalPrice,
                                            @RequestParam(required = true) String pricesJson,Model model) throws MyException {
        Admin admin = (Admin) model.asMap().get(AppConstant.ADMIN_SESSION_NAME);
        BasicRet basicRet = new BasicRet();

        Orders orders = ordersService.getOrdersById(orderid);
        if(orders == null){
            return  new BasicRet(BasicRet.ERR,"订单不存在");
        }

        if(orders.getOrderstatus() != Quantity.STATE_0){
            return  new BasicRet(BasicRet.ERR,"只有待付款订单可以改价");
        }
        Short isticket=orders.getIsticket();
        BigDecimal discountprice=Quantity.BIG_DECIMAL_0;
        List<ModifyOrderProductPrice> priceList = GsonUtils.toList(pricesJson,ModifyOrderProductPrice.class);

        for(ModifyOrderProductPrice price : priceList){
            if(price == null || price.getPrice() == null || price.getId()<=0 || price.getPrice().compareTo(Quantity.BIG_DECIMAL_0) <= 0){
               return  new BasicRet(BasicRet.ERR,"pricesJson数据不正确");
            }
        }

        StringBuilder log = new StringBuilder();

        List<OrderProduct> orderProductList = ordersService.getOrderProductByOrderId(orders.getId());
        int count= 0;
        if(orderProductList.size() != priceList.size()){
            return  new BasicRet(BasicRet.ERR,"订单商品数量与原订单不匹配");
        }else{
            for(OrderProduct orderProduct : orderProductList){
                for(ModifyOrderProductPrice price : priceList){
                    if(orderProduct.getId().equals(price.getId())){
                        if(orderProduct.getPrice().compareTo(price.getPrice()) != 0) {
                            log.append("订单商品" + orderProduct.getPdname()+"("+orderProduct.getId()+")" + "价格变动:" + orderProduct.getPrice() + "->" + price.getPrice() + ";\n");
                        }

                        orderProduct.setPrice(price.getPrice());
                        count++;
                    }
                }
            }

            if(orderProductList.size() != count){
                return  new BasicRet(BasicRet.ERR,"订单商品数量与原订单个数不匹配");
            }
        }

        BigDecimal orderTotalPrice = Quantity.BIG_DECIMAL_0; //订单总价
        BigDecimal deposit = Quantity.BIG_DECIMAL_0;  //远期定金
        BigDecimal balance = Quantity.BIG_DECIMAL_0;  //远期余款
        BigDecimal allPay = Quantity.BIG_DECIMAL_0;  //远期全款

        BigDecimal totalWeight = Quantity.BIG_DECIMAL_0; //总重量
        List<OrderProduct>  updateOrderProductList=new ArrayList<>();

        for(OrderProduct orderProduct : orderProductList){
            OrderProduct updateOrderProduct = new OrderProduct();
            updateOrderProduct.setId(orderProduct.getId());
            updateOrderProduct.setPrice(orderProduct.getPrice());

            BigDecimal productTotalPrice = new BigDecimal(orderProduct.getPrice().multiply(orderProduct.getNum()).toString()).setScale(2,BigDecimal.ROUND_HALF_UP);

            if(orderProduct.getProtype() == 0){  //远期类型0=不是远期1=全款2=定金
                updateOrderProduct.setActualpayment(productTotalPrice.setScale(2,BigDecimal.ROUND_HALF_UP));
            }else if(orderProduct.getProtype() ==1){
                updateOrderProduct.setAllpay(new BigDecimal(orderProduct.getPrice().multiply(orderProduct.getNum()).toString()));
                updateOrderProduct.setActualpayment(productTotalPrice.setScale(2,BigDecimal.ROUND_HALF_UP));
                allPay = allPay.add(productTotalPrice.setScale(2,BigDecimal.ROUND_HALF_UP));
            }else if(orderProduct.getProtype() == 2){
                updateOrderProduct.setActualpayment(productTotalPrice.setScale(2,BigDecimal.ROUND_HALF_UP));
                BigDecimal partpay = new BigDecimal(productTotalPrice.subtract(orderProduct.getFreight()).multiply(transactionSettingService.getTransactionSetting().getRemotepurchasingmargin()).multiply(new BigDecimal("0.01")).toString()).setScale(2,BigDecimal.ROUND_HALF_UP);
                BigDecimal yupay = productTotalPrice.subtract(partpay);
                updateOrderProduct.setPartpay(partpay);
                updateOrderProduct.setYupay(yupay);

                deposit = deposit.add(partpay);
                balance = balance.add(yupay);
            }else{
                throw new RuntimeException("商品远期类型不合法");
            }
            updateOrderProduct.setProtype(orderProduct.getProtype());
            orderTotalPrice = orderTotalPrice.add(updateOrderProduct.getActualpayment());
            updateOrderProductList.add(updateOrderProduct);
        }
        //重新进行优惠金额的计算
        if (isticket.compareTo((short) 1)==0){
            //优惠金额
            discountprice=orders.getDiscountprice();
            if (orderTotalPrice.compareTo(discountprice)==-1){
                return  new BasicRet(BasicRet.ERR,"减价后总金额小于优惠金额");
            }
            yhqUseService.useCouponForUpdate(updateOrderProductList,discountprice);

        }
        orderTotalPrice=Quantity.BIG_DECIMAL_0;
        for (OrderProduct orderProduct:updateOrderProductList){
            orderTotalPrice=orderTotalPrice.add(orderProduct.getActualpayment());
            BigDecimal actualpayment=orderProduct.getActualpayment();
            if(orderProduct.getProtype() == 2){
                BigDecimal partpay = new BigDecimal(actualpayment.subtract(orderProduct.getFreight()).multiply(transactionSettingService.getTransactionSetting().getRemotepurchasingmargin()).multiply(new BigDecimal("0.01")).toString()).setScale(2,BigDecimal.ROUND_HALF_UP);
                BigDecimal yupay = actualpayment.subtract(partpay);
                orderProduct.setPartpay(partpay);
                orderProduct.setYupay(yupay);

                deposit = deposit.add(partpay);
                balance = balance.add(yupay);
            }
            orderProductServices.updateByPrimaryKeySelective(orderProduct);
        }


        //orderTotalPrice = orderTotalPrice.add(orders.getFreight());  //加上运费
        //重新计算运费
        BigDecimal fright = Quantity.BIG_DECIMAL_0;
        if(orders.getOrderfright() != null&&orders.getOrderfright() != 1 && orders.getOrderfright() != 2 && orders.getOrderfright() != 3){
            OrderFrightDto orderFrightDto = null;
            if(StringUtils.hasText(orders.getFrighttemplate())){
                orderFrightDto = GsonUtils.toBean(orders.getFrighttemplate(),OrderFrightDto.class);

                BigDecimal totalProdMoney = Quantity.BIG_DECIMAL_0;
                BigDecimal totalProdWeigth = Quantity.BIG_DECIMAL_0;
                //此处运费的计算存在问题，需要用更新之后的orderProductList
                orderProductList=ordersService.getOrderProductByOrderId(orders.getId());
                for(OrderProduct orderProduct : orderProductList){
                    totalProdMoney = totalProdMoney.add(orderProduct.getActualpayment());
                    ProductStore productStore = productStoreService.getByPdidAndPdno(orderProduct.getPdid(),orderProduct.getPdno());
                    totalProdWeigth = totalProdWeigth.add(productStore.getWeight().multiply(orderProduct.getNum()));
                }

                fright = freightService.getFreightByOrderFrightDto(orderFrightDto,totalProdMoney.add(discountprice),totalProdWeigth,orders.getProvince(),orders.getCity());
            }else{
                fright = orders.getFreight();
            }
        }


        //计算商品佣金
        ordersService.jisuanOrdersBreakPay(Collections.singletonList(orders.getOrderno()));

        //总金额 = 商品总金额 + 运费
        orderTotalPrice = orderTotalPrice.add(fright);

        Orders updateOrders = new Orders();
        updateOrders.setId(orders.getId());
        updateOrders.setActualpayment(orderTotalPrice);
        updateOrders.setTotalprice(orderTotalPrice.add(discountprice));
        updateOrders.setBalance(balance.add(fright));
        updateOrders.setDeposit(deposit);
        updateOrders.setAllpay(orderTotalPrice);
        updateOrders.setFreight(fright);
        ordersService.updateSingleOrder(updateOrders);


        //修改发票金额
        if (orders.getIsbilling() == Quantity.STATE_1) {
            billingRecordService.updateBillcashByMemberAndOrderid(orders.getId().toString(), orders.getMemberid(), orderTotalPrice);
        }


        //操作日志
        OperateLog operateLog = new OperateLog();
        operateLog.setContent("修改商品单价:" + log.toString());
        operateLog.setOpid(admin.getId());
        operateLog.setOpname(admin.getUsername());
        operateLog.setOptime(new Date());
        operateLog.setOptype(Quantity.STATE_0);
        operateLog.setOrderid(orders.getId());
        operateLog.setOrderno(orders.getOrderno());
        ordersService.saveOperatelog(operateLog);

        return  new BasicRet(BasicRet.SUCCESS,"改价成功");
    }



    private class ModifyOrderProductPrice{
        private Long id;
        private BigDecimal price;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }
    }
}
