package project.jinshang.mod_member.bean.dto;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * create : wyh
 * date : 2017/12/9
 */
public class MemberAdminQueryDto implements Serializable{

    @ApiModelProperty(notes = "会员编号")
    private Long id;

    @ApiModelProperty(notes = "用户名")
    private String username;


    @ApiModelProperty(notes = "手机号")
    private String mobile;

    @ApiModelProperty(notes = "客服人员")
    private String clerkname;

    @ApiModelProperty(notes = "业务员")
    private String waysalesman;


    @ApiModelProperty(notes = "注册时间-开始")
    private Date registDateStart;

    @ApiModelProperty(notes = "注册时间-结束")
    private  Date registDateEnd;

    @ApiModelProperty(notes = "会员标签id")
    private Long labelid;


    @ApiModelProperty(notes = "会员等级id")
    private Long gradleid;



    @ApiModelProperty(notes = "最后登录时间-开始")
    private Date loginDateStart;

    @ApiModelProperty(notes = "最后登录时间-结束")
    private  Date loginDateEnd;


    @ApiModelProperty(notes = "是否是公司 0-全部，1-是，2-否")
    private  short companyType;



    @ApiModelProperty(notes = "会员状态 是否停用")
    private  Boolean disabled;

    @ApiModelProperty(notes = "有没消费")
    private Short isbuy;

    @ApiModelProperty(notes = "注册来源标识")
    private String registersourcelabel;

    @ApiModelProperty(notes = "注册类型标识")
    private String registertypelabel;

    @ApiModelProperty(notes = "注册渠道标识")
    private String registerchannellabel;

    @ApiModelProperty(notes = "标签名称")
    private String labelname;



    public String getWaysalesman() {
        return waysalesman;
    }

    public void setWaysalesman(String waysalesman) {
        this.waysalesman = waysalesman;
    }

    public Short getIsbuy() {
        return isbuy;
    }

    public void setIsbuy(Short isbuy) {
        this.isbuy = isbuy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getClerkname() {
        return clerkname;
    }

    public void setClerkname(String clerkname) {
        this.clerkname = clerkname;
    }

    public Date getRegistDateStart() {
        return registDateStart;
    }

    public void setRegistDateStart(Date registDateStart) {
        this.registDateStart = registDateStart;
    }

    public Date getRegistDateEnd() {
        return registDateEnd;
    }

    public void setRegistDateEnd(Date registDateEnd) {
        this.registDateEnd = registDateEnd;
    }

    public Long getLabelid() {
        return labelid;
    }

    public void setLabelid(Long labelid) {
        this.labelid = labelid;
    }

    public Long getGradleid() {
        return gradleid;
    }

    public void setGradleid(Long gradleid) {
        this.gradleid = gradleid;
    }

    public Date getLoginDateStart() {
        return loginDateStart;
    }

    public void setLoginDateStart(Date loginDateStart) {
        this.loginDateStart = loginDateStart;
    }

    public Date getLoginDateEnd() {
        return loginDateEnd;
    }

    public void setLoginDateEnd(Date loginDateEnd) {
        this.loginDateEnd = loginDateEnd;
    }

    public short getCompanyType() {
        return companyType;
    }

    public void setCompanyType(short companyType) {
        this.companyType = companyType;
    }

    public String getRegistersourcelabel() {
        return registersourcelabel;
    }

    public void setRegistersourcelabel(String registersourcelabel) {
        this.registersourcelabel = registersourcelabel == null ? null : registersourcelabel.trim();
    }

    public String getRegistertypelabel() {
        return registertypelabel;
    }

    public void setRegistertypelabel(String registertypelabel) {
        this.registertypelabel = registertypelabel == null ? null : registertypelabel.trim();
    }

    public String getRegisterchannellabel() {
        return registerchannellabel;
    }

    public void setRegisterchannellabel(String registerchannellabel) {
        this.registerchannellabel = registerchannellabel == null ? null : registerchannellabel.trim();
    }

    public String getLabelname() {
        return labelname;
    }

    public void setLabelname(String labelname) {
        this.labelname = labelname == null ? null : labelname.trim();
    }
}
