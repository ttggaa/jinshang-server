package project.jinshang.mod_admin.mod_article;


import com.github.pagehelper.PageInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import mizuki.project.core.restserver.config.BasicRet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import project.jinshang.common.bean.MemberLogOperator;
import project.jinshang.common.bean.PageRet;
import project.jinshang.common.constant.AppConstant;
import project.jinshang.common.constant.Quantity;
import project.jinshang.common.utils.GsonUtils;
import project.jinshang.common.utils.ProductCategoryUtils;
import project.jinshang.common.utils.StringUtils;
import project.jinshang.mod_admin.mod_article.bean.*;
import project.jinshang.mod_admin.mod_article.service.ArticleCategoryService;
import project.jinshang.mod_admin.mod_article.service.ArticleService;
import project.jinshang.mod_common.bean.BasicExtRet;
import project.jinshang.mod_member.bean.Admin;
import project.jinshang.mod_member.bean.Member;
import project.jinshang.mod_product.service.MemberOperateLogService;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

@RestController
@Api(tags = "文章内容管理", description = "文章管理相关接口")
@RequestMapping(value = "/rest/admin/newArticle")
@SessionAttributes({AppConstant.ADMIN_SESSION_NAME})
public class NewArticeAction {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleCategoryService articleCategoryService;

    @Autowired
    MemberLogOperator memberLogOperator;

    @Autowired
    MemberOperateLogService memberOperateLogService;

    @RequestMapping(value = "/addArticle", method = RequestMethod.POST)
    @ApiOperation("添加文章")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "docid", value = "文章内容ID", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "doctitle", value = "文章标题", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "doccontent", value = "文章内容", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "docorder", value = "文章排序", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "docaddress", value = "链接地址", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "docshow", value = "文章是否显{0:不显示;1:显示}", required = false, defaultValue = "1", dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "sketch", value = "文章简述", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "picjson", value = "文章图片json字符串", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "type", value = "资讯类型", required = true, dataType = "short", paramType = "query"),
    })
    public BasicRet addArticle(Article article, Model model, HttpServletRequest request) {
        Admin admin = (Admin) model.asMap().get(AppConstant.ADMIN_SESSION_NAME);
        BasicRet basicRet = new BasicRet();
//        List<Article> articles = articleService.getArticleListBySortId(article.getDocid(), article.getDocorder());
//        if (articles.size() != 0) {
//            Article defaultArticle = articles.get(0);
//            defaultArticle.setDocorder(articleService.maxSortIdByDocId(article.getDocid()) + 1);
//            articleService.updateArticle(defaultArticle);
//        }
        List<ArticlePic> articlePicList= GsonUtils.toList(article.getPicjson(),ArticlePic.class);
//        if (article.getType().compareTo(Quantity.STATE_1)==0){
            article.setPic(articlePicList.get(0).getPath());
//        }
        articleService.addArticle(article);
        if (article.getType().compareTo(Quantity.STATE_1)==0&&articlePicList!=null&&articlePicList.size()>0){
            for (ArticlePic articlePic:articlePicList){
//                if (StringUtils.isEmpty(articlePic.getDescription())){
//                    articlePic.setDescription(article.getDoctitle());
//                }
                articlePic.setArticleid(article.getId());
                articleService.addArticlePic(articlePic);
            }
        }
        basicRet.setResult(BasicRet.SUCCESS);
        basicRet.setMessage("添加成功");
        memberLogOperator.saveMemberLog(null, admin, "新增文章：" + article.getDoctitle(), "/addArticle", request, memberOperateLogService);
        return basicRet;
    }

    @RequestMapping(value = "/deleteArticle", method = RequestMethod.POST)
    @ApiOperation("删除文章")
    public BasicRet deleteArticle(@RequestParam(required = true) long id, Model model, HttpServletRequest request) {
        Admin admin = (Admin) model.asMap().get(AppConstant.ADMIN_SESSION_NAME);
        BasicRet basicRet = new BasicRet();
        Article article = articleService.getArticleById(id);
        if (article == null) {
            basicRet.setResult(BasicRet.ERR);
            basicRet.setMessage("没有该文章");
            return basicRet;
        }
        articleService.deleteArticle(id);
        articleService.deleteArticlePic(id);
        basicRet.setResult(BasicRet.SUCCESS);
        basicRet.setMessage("删除成功");
        memberLogOperator.saveMemberLog(null, admin, "删除文章：" + article.getDoctitle(), "/deleteArticle", request, memberOperateLogService);
        return basicRet;
    }

    @RequestMapping(value = "/updateArticle", method = RequestMethod.POST)
    @ApiOperation("修改文章")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "文章内容ID", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "docid", value = "文章分类ID", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "doctitle", value = "文章标题", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "doccontent", value = "文章内容", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "docorder", value = "文章排序", required = true, dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "docaddress", value = "链接地址", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "docshow", value = "文章是否显{0:不显示;1:显示}", required = false, defaultValue = "1", dataType = "int", paramType = "query"),
            @ApiImplicitParam(name = "sketch", value = "文章简述", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "picjson", value = "文章图片json字符串", required = false, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "type", value = "资讯类型", required = true, dataType = "short", paramType = "query"),
    })
    public BasicRet updateArticle(Article article, Model model, HttpServletRequest request) {
        Admin admin = (Admin) model.asMap().get(AppConstant.ADMIN_SESSION_NAME);
        BasicRet basicRet = new BasicRet();
//        Article dbArticle = articleService.getArticleById(article.getId());
//        List<Article> articles = articleService.getArticleListBySortId(article.getDocid(), article.getDocorder());
//        if (!dbArticle.getDocid().equals(article.getDocid())) {
//            if (articles.size() != 0) {
//                Article defaultArticle = articles.get(0);
//                defaultArticle.setDocorder(articleService.maxSortIdByDocId(article.getDocid()) + 1);
//                articleService.updateArticle(defaultArticle);
//            }
//        } else if (!dbArticle.getDocorder().equals(article.getDocorder())) {
//            if (articles.size() != 0) {
//                Article defaultArticle = articles.get(0);
//                defaultArticle.setDocorder(dbArticle.getDocorder());
//                articleService.updateArticle(defaultArticle);
//            }
//        }
        List<ArticlePic> articlePicList= GsonUtils.toList(article.getPicjson(),ArticlePic.class);
//        if (article.getType().compareTo(Quantity.STATE_1)!=0&&articlePicList!=null&&articlePicList.size()>0){
            article.setPic(articlePicList.get(0).getPath());
//        }
        articleService.updateArticleNew(article);
        if (article.getType().compareTo(Quantity.STATE_1)==0&&articlePicList!=null&&articlePicList.size()>0){
            articleService.deleteArticlePic(article.getId());
            for (ArticlePic articlePic:articlePicList){
//                if (StringUtils.isEmpty(articlePic.getDescription())){
//                    articlePic.setDescription(article.getDoctitle());
//                }
                articlePic.setArticleid(article.getId());
                articleService.addArticlePic(articlePic);
            }
        }
        basicRet.setResult(BasicRet.SUCCESS);
        basicRet.setMessage("修改成功");
        memberLogOperator.saveMemberLog(null, admin, "修改文章：" + article.getDoctitle(), "/updateArticle", request, memberOperateLogService);
        return basicRet;
    }

    @RequestMapping(value = "/article/list", method = RequestMethod.POST)
    @ApiOperation("后台文章管理列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageNo", value = "pageNo", required = true, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "pageSize", value = "pageSize", required = true, paramType = "query", dataType = "int"),
            @ApiImplicitParam(name = "titleName", value = "titleName", required = false, paramType = "query", dataType = "String"),
            @ApiImplicitParam(name = "articleCategoryId", value = "articleCategoryId", required = false, paramType = "query", dataType = "Long"),
    })
    public PageRet getArticleList(@RequestParam int pageNo,
                                  @RequestParam int pageSize,
                                  @RequestParam(required = false) String titleName,
                                  @RequestParam(required = false) Long articleCategoryId) {
        PageRet pageRet = new PageRet();
        PageInfo pageInfo = articleService.getNewArticleList(pageNo, pageSize, titleName, articleCategoryId);
        pageRet.data.setPageInfo(pageInfo);
        pageRet.setMessage("获取成功");
        pageRet.setResult(BasicRet.SUCCESS);
        return pageRet;
    }

    @RequestMapping(value = "/getArticleListCategoryList", method = RequestMethod.POST)
    @ApiOperation(value = "获取文章分类")
    @ResponseBody
    public BasicRet getArticleListCategoryList() {
        BasicExtRet basicRet = new BasicExtRet();

        List<ArticleCategory> list = articleCategoryService.getAll();
        List<ArticleListCategory> articleListCategoryList = ProductCategoryUtils.toArticleProdCate(list);
        articleListCategoryList = ProductCategoryUtils.getArticleChildsManyGroup(articleListCategoryList, 0);
        basicRet.setData(articleListCategoryList);
        basicRet.setResult(BasicRet.SUCCESS);
        return basicRet;
    }

    @PostMapping(value = "/addToCarousel")
    @ApiOperation(value = "取消、添加文章图片到轮播图")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "carouselstate",required =true,paramType = "short"),
            @ApiImplicitParam(name ="articleId",required = true,paramType = "long")
    })

    public BasicRet addToCarousel(Model model,Long articleId,short carouselstate){
        BasicRet basicRet=new BasicRet();
        basicRet.setResult(BasicRet.SUCCESS);
        basicRet.setMessage("更新成功");
        Article article=new Article();
        article.setId(articleId);
        article.setIscarousel(carouselstate);
        articleService.updateArticle(article);
        return basicRet;
    }




}
