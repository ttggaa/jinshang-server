package project.jinshang.mod_product.service;

import com.hankcs.hanlp.dictionary.CustomDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import project.jinshang.common.constant.Quantity;
import project.jinshang.common.utils.NlpUtils;
import project.jinshang.common.utils.StringUtils;
import project.jinshang.mod_admin.mod_synonym.Synonym;
import project.jinshang.mod_admin.mod_synonym.SynonymMapper;
import project.jinshang.mod_product.ProductInfoMapper;
import project.jinshang.mod_product.bean.ProductAttr;
import project.jinshang.mod_product.bean.ProductInfo;
import project.jinshang.mod_product.bean.ProductInfoExample;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductSearchServiceImpl implements ProductSearchService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private ProductSearchMapper productSearchMapper;
    @Autowired
    private NlpUtils nlpUtils;

    @Autowired
    private ProductAttrService productAttrService;

    private ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

    private void saveIndex(long productId, Set<String> params){
        String sindex = nlpUtils.tranlatePGVector(nlpUtils.seqForPgVector(params));
        Map<String,Object> map = productSearchMapper.findById(productId);
        if(map!=null){
            if(!sindex.equals(map.get("sindex"))) productSearchMapper.updateIndex(productId,sindex);
        }else{
            productSearchMapper.saveIndex(productId,sindex);
        }
    }
    public void saveIndex(ProductInfo productInfo){
        List<ProductAttr> list = productInfo.getAttrList();
        Set<String> params  = new HashSet<>();

        if(list != null) {
            for (int i = 0; i < list.size(); i++) {
                ProductAttr attr = list.get(i);
                params.add(StringUtils.nvl(attr.getValue()).toLowerCase());
            }
        }

        params.add(StringUtils.nvl(productInfo.getProductname()).toLowerCase());
        params.add(StringUtils.nvl(productInfo.getMark()).toLowerCase());
        params.add(StringUtils.nvl(productInfo.getBrand()).toLowerCase());
        params.add(StringUtils.nvl(productInfo.getLevel1()).toLowerCase());
        params.add(StringUtils.nvl(productInfo.getLevel2()).toLowerCase());
        params.add(StringUtils.nvl(productInfo.getLevel3()).toLowerCase());
        params.add(StringUtils.nvl(productInfo.getStand()).toLowerCase());
        params.add(StringUtils.nvl(productInfo.getMaterial()).toLowerCase());
        params.add(StringUtils.nvl(productInfo.getCardnum()).toLowerCase());
        if(productInfo.getAttrList() != null && productInfo.getAttrList().size()>0){
            params.add(productInfo.getAttrList().get(0).getPdno());
        }


        params.add(StringUtils.nvl(productInfo.getProductalias().toLowerCase()));
//        params.add(StringUtils.nvl(productInfo.getPddes().toLowerCase()));

        String[] paramArr =  new String[params.size()];

        saveIndex(productInfo.getId(),params);
    }
    public  void  delIndex(long productId){
        productSearchMapper.deleteIndex(productId);
    }

    @Override
    public Map<String,Object> search(String search_str,
                                     String level1,String level2,String level3,
                                     String productname,String brand,String cardnum,String material,String surfacetreatment,Map<String,Object> attrs,
                                     int start,int max,String sorttype,Integer selfsupport,Integer havestore,Integer forwardtime,String store,Integer type,String productType) {
        return null;
    }

    //    public List<Map> search(String search_str,int start,int max){
//       return productSearchMapper.search(nlpUtils.seqForPgQuery(search_str),start,max);
//    }

    /**** 提取分类关键字*/
    public List<Map> fetchSearchKeys(String search_str,
                                     String level1,String level2,String level3,
                                     String productname,String brand,String cardnum,String material,String surfacetreatment,Map<String,Object> attrs,
                                     Integer selfsupport,Integer havestore,Integer forwardtime,String store){
        return productSearchMapper.searchKeys(nlpUtils.seqForPgQuery(search_str),
                level1,level2,level3,productname,brand,cardnum,material,surfacetreatment,attrs,selfsupport,havestore,forwardtime,store);
    }


    public List<Map> otherProdFetchSearchKeys(String search_str,
                                     String level1,String level2,String level3,
                                     String productname,String brand,Map<String,Object> attrs,int type){
        return productSearchMapper.otherProdSearchKeys(nlpUtils.seqForPgQuery(search_str),
                level1,level2,level3,productname,brand,attrs,type);
    }



    public List<Map> fetchSearchAttrKeys(String search_str,
                                     String level1,String level2,String level3,
                                     String productname,String brand,String cardnum,String material,String surfacetreatment,
                                         Map<String,Object> attrs,Integer selfsupport,Integer havestore,Integer forwardtime,String store){
        return productSearchMapper.searchAttrKeys(nlpUtils.seqForPgQuery(search_str),
                level1,level2,level3,productname,brand,cardnum,material,surfacetreatment,attrs,selfsupport,havestore,forwardtime,store);
    }



    public List<Map> fetchSearchAttrKeysHashAttr(String search_str,
                                         String level1,String level2,String level3,
                                         String productname,String brand,String cardnum,String material,String surfacetreatment,Map<String,Object> attrs,
                                                 Integer selfsupport,Integer havestore,Integer forwardtime,String store){
        return productSearchMapper.searchAttrKeysHashAttr(nlpUtils.seqForPgQuery(search_str),
                level1,level2,level3,productname,brand,cardnum,material,surfacetreatment,attrs,selfsupport,havestore,forwardtime,store);
    }


    public List<Map> otherProdFetchSearchAttrKeys(String search_str,
                                         String level1,String level2,String level3,
                                         String productname,String brand,Map<String,Object> attrs,int type){
        return productSearchMapper.otherProdsearchAttrKeys(nlpUtils.seqForPgQuery(search_str),
                level1,level2,level3,productname,brand,attrs,type);
    }

    /**
     * 搜索中 再分类显示结果
     * 分组分页
     * start 分页开始 （0 起始）
     * max 分页个数 -1则为全部
     */
    public List<Map> searchWithKeys(
            String search_str,
            String level1,String level2,String level3,
            String productname,String brand,String cardnum,String material,String surfacetreatment,Map<String,Object> attrs,
            int start,int max,String sorttype,Integer selfsupport,Integer havestore,Integer forwardtime,String store
    ){
        return productSearchMapper.searchWithKeys(nlpUtils.seqForPgQuery(search_str),
                level1,level2,level3,productname,brand,cardnum,material,surfacetreatment,attrs,start,max,sorttype,
               selfsupport,havestore,forwardtime,store);
    }




    /**
     * 搜索中 再分类显示结果
     * 分组分页
     * start 分页开始 （0 起始）
     * max 分页个数 -1则为全部
     */
    public List<Map> otherProdSearchWithKeys(
            String search_str,
            String level1,String level2,String level3,
            String productname,String brand,Map<String,Object> attrs,int pageNo,int pageSize,String sorttype,int type

    ){
       int start = (pageNo-1)*pageSize;
       List<Map> list =   productSearchMapper.otherProdSearchWithKeys(nlpUtils.seqForPgQuery(search_str),
                level1,level2,level3,productname,brand,attrs,start,pageSize,sorttype,type);

       return  list;
    }



    /**
     * 总数
     */
    public int countSearchWithKeys(String search_str,
                                   String level1,String level2,String level3,
                                   String productname,String brand,String cardnum,String material,String surfacetreatment,Map<String,Object> attrs,Integer selfsupport,Integer havestore,Integer forwardtime,String store){
        return productSearchMapper.countSearchWithKeys(nlpUtils.seqForPgQuery(search_str),
                level1,level2,level3,productname,brand,cardnum,material,surfacetreatment,attrs,selfsupport,havestore,forwardtime,store);
    }


    /**
     * 总数
     */
    public int otherProdCountSearchWithKeys(String search_str,
                                   String level1,String level2,String level3,
                                   String productname,String brand,Map<String,Object> attrs,int type){
        return productSearchMapper.otherProdCountSearchWithKeys(nlpUtils.seqForPgQuery(search_str),
                level1,level2,level3,productname,brand,attrs,type);
    }

    @Autowired
    private ProductInfoMapper productInfoMapper;

    @Autowired
    private SynonymMapper synonymMapper;

    public void rebuildIndex(){
        cachedThreadPool.execute(() -> {
            // 重新加载词典
            List<Synonym> synonyms = synonymMapper.listAll(null);
            synonyms.forEach(synonym -> synonym.getWords().forEach(CustomDictionary::add));

            ProductInfoExample example = new ProductInfoExample();
            example.createCriteria().andPdstateEqualTo(Quantity.STATE_4);
            List<ProductInfo> list = productInfoMapper.selectByExample(example);

            list.forEach(info->{
                List<ProductAttr> attrList = productAttrService.getByProductid(info.getId());
                info.setAttrList(attrList);
                saveIndex(info);
            });

        });
    }

    @Override
    public List<Map<String, Object>> searchByStoreidAndPdno(Long storeid, String pdno) {
        return null;
    }

    @Override
    public void bulkUpdateIndex(List<Map<String, Object>> data) {
        return;
    }


}
