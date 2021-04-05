package com.izhiliu.erp.service.item;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.core.common.ValidList;
import com.izhiliu.core.domain.common.IBaseService;
import com.izhiliu.erp.domain.enums.LocalProductStatus;
import com.izhiliu.erp.service.item.cache.Publish;
import com.izhiliu.erp.domain.item.ShopeeProduct;
import com.izhiliu.erp.service.item.dto.*;
import com.izhiliu.erp.web.rest.item.param.*;
import com.izhiliu.erp.web.rest.item.vm.*;
import com.izhiliu.erp.web.rest.provider.dto.HomeTodoVO;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service Interface for managing ShopeeProduct.
 */
public interface ShopeeProductService extends IBaseService<ShopeeProduct, ShopeeProductDTO> {

    boolean superUpdate(ShopeeProductDTO dto);

    Optional<ShopeeProduct> selectShopProductByParentIdAndShopId(Long parentId, Long shopId);

    Optional<ShopeeProduct> nodeProduct(Long parentId, Long platformNodeId);

    List<ProductListVM_V2> listShopProductByParentId(Long parentId, List<Long> publishShops, List<Long> unpublishShops);

    List<Long> shopIds(long productId);

    int updateByCategoryId(long categoryId, ShopeeProductDTO dto);

    boolean deleteLocal(Long id);

    int deleteByItemId(long itemId);

    ResponseEntity<Object> checkUpdate(ShopeeProductDTO dto);

    ResponseEntity<Object> checkUpdateOrSave(ShopeeProductDTO dto);


    void checkParam(ShopeeProductDTO dto);

    void checkParamTwo(ShopeeProductDTO dto);


    IPage<ProductListVM_V21> getProductListVM_v21IPage(IPage<ShopeeProduct> search);

     void settingRequiredInfo(ProductSearchStrategyDTO productSearchStrategy, String loginId, IPage<ProductListVM_V21> productListVMV21IPage);

    void settingSku(IPage<ProductListVM_V21> productListVMV21IPage, List<Long> productIds);

     void settingMediaInfo(IPage<ProductListVM_V21> productListVMV21IPage, List<Long> productIds);

     void settingShopName(String loginId, IPage<ProductListVM_V21> productListVMV21IPage);

      void settingBoost(IPage<ProductListVM_V21> productListVMV21IPage, List<Long> productIds);
    /**
     * 找不到会抛异常
     */
    Optional<ShopeeProductDTO> findThrow(long id);

    Optional<ShopeeProductDTO> findShopeeProductDTO21(long id);

    Optional<ShopeeProductDTO> findByItemIdAndShopId(long itemId, long shopId);

    /**
     * 查询所有子商品
     */
    List<ShopeeProductDTO> childs(long parentId, int childType);

    List<ShopeeProduct> childs$(long parentId, int childType);

    /**
     * 根据源数据ID查询
     */
    List<ShopeeProductDTO> listByMetaData(String metaDataId);
    /**
     * 拷贝到站点
     */
    Long copyToPlatformNode(long productId, long platformNodeId);


    /**
     *
     * @param productId 商品id
     * @param platformNodeId 节点ID
     * @return
     */
    Map copyToPlatformNodeTwo(long productId, long platformNodeId);

    /**
     * 拷贝一个商品到店铺
     *
     * @param productId
     * @param shopId
     * @return
     */
    Long copyToShop(long productId, long shopId,String loginId);

    /**
     * 拷贝到店铺
     */
    List<Long> publishToShop(SaveOrPublishToShopParam param);

    /**
     * 中间商品优化
     */
    List<Long> publishToShopTwo(SaveOrPublishToShopParam param);

    List<Long> saveToShop(SaveOrPublishToShopParam param);

    /**
     * 发布到店铺
     */
    void publishToShop(List<ShopProductParam> params);

    /**
     * 拉取到本地
     */
    void pullToLocal(List<ShopProductParam> params);

    /**
     * 拉取到本地
     */
    void pullToLocalAll(List<Long> shopIds);

    /**
     * 拉取到本地
     */
    ShopSycnResult pullToLocalAllTwo(List<Long> shopIds, List<Long> itmeIds, String key);

    void remove(String key);

    /**
     * 推送到店铺
     */
    void pushToShop(List<ShopProductParam> params);

    /**
     * 推送店铺 new
     * @param qo
     * @return
     */
    Boolean pushToShopCommon(PushToShopeeTaskQO qo);

    /**
     * 图片换链
     * @param qo
     * @return
     */
    Boolean pushLocalImageToShopee(PushToShopeeTaskQO qo);

    /**
     * 商品上下架
     */
    void shopUnlist(ShopUnListParam param);

    /**
     * 追踪商品所在店铺
     */
    List<ShopVM> trackToTheShop(int type, long id);

    List<ShopVM> trackToTheShop(long id);

    /**
     * 根据 itemId 获取
     */
    Optional<ShopeeProductDTO> selectByItemId(long itemId);

    List<ScItemDTO> selectShopeeProductAllInfo(Map<Long, List<Long>> map);

    Float convertPrice(String from, String to, Float price);

    /**
     * 批量拷贝平台商品到站点
     */
    List<BatchEditProductVM> batchCopyToPlatformNode(BatchCopyToPlatformNodeParam param);

    /**
     * 批量查询数据
     */
    List<BatchEditProductVM> batchGetProduct(List<Long> productIds);

    /**
     * 批量保存
     */
    boolean batchSave(ValidList<BatchEditProductVM> products);

    /**
     * 批量保存到店铺
     */
    boolean batchSaveToShop(BatchEditToShopParam param);

    boolean batchSavePriceAndStock(BatchSavePriceAndStockParam param);

    /**
     * 查询商品来源列表
     */
    List<String> listBySource(String loginId);

    IPage<ProductListVM_V2> searchProductByCurrentUserV2(String loginId, ProductSearchStrategyDTO productSearchStrategy, Page page);

    /***
     * as  searchProductByCurrentUserV3
     * @param productSearchStrategy
     * @return
     */
    IPage<ProductListVM_V21> searchShopProductByCurrentUser(ProductSearchStrategyDTO productSearchStrategy ) ;

     void fill(String loginId, ProductSearchStrategyDTO productSearchStrategy);


    List<ProductListVM_V2> getNodeChilds(Long productId);

    List<ShopVM> getShops(Long productId);

    String getCurrencyByShopIdAndItemId(Long shopId, Long itemId);

    String getCurrencyById(Long id);

    String getLoginId(Long id);

    int childCount(Long productId);

    Long pending(String login);

    Long fail(String login);

     HomeTodoVO todo();

    /**
     *   生成一条平台源数据
     * @param dto
     * @return
     */
     ShopeeProductDTO saveSource(ShopeeProductInsertDTO dto);

    ShopeeProductDTO collectSave(ShopeeProductDTO product);

    Long  generate();

    /**
     *   查询是否是  {@code type} 站点商品 并且是{@code status}待发布的 才找出来
     * @param productIds
     * @param loginId
     * @param status
     * @param isShopItem
     * @return
     */
    List<ShopeeProduct> findList(List<Long> productIds, String loginId, Integer status, Boolean isShopItem);

    void batchPublish(List<Publish> publishDtos, String start, String end);

    void deleteBatchPublish(List<Long> productIds);

    Optional<List<ShopeeProductDTO>> selectShopeeProductBatch(List<Long> itemIdList);

    List<ShopeeProduct> selectBatchIds(List<Long> productIds);

    Long getProductIdByItemId(Long itemId);

    List<ShopeeProduct> findShopeeProductList(List<Long> itmeIds);


    /**
     * 推送执行成功后处理
     * @param product
     * @param pushSuccess
     */
    void pushSuccessHandle(ShopeeProductDTO product, LocalProductStatus pushSuccess);

    /**
     * 推送失败后处理
     * @param productId
     * @param errorMsg
     * @param pushFailure
     */
    void pushFailHandle(String productId, String errorMsg, LocalProductStatus pushFailure);

    /**
     * 修改状态
     * @param productId
     * @param pushFailure
     */
    void updateStatus(Long productId, LocalProductStatus pushFailure);

    /**
     *  校验是否Init variation
     * @param product
     * @return
     */
    Boolean checkNoInitTierVariation(ShopeeProductDTO product);

    /**
     * 同步店铺商品
     * @param qo
     * @return
     */
    TaskExecuteVO syncByShop(ItemSyncQO qo);

    /**
     * 批量同步商品
     * @param qo
     * @return
     */
    TaskExecuteVO syncBatch(ItemSyncQO qo);

    /**
     * 获取商品同步的进度
     * @param login
     * @param taskId
     * @return
     */
    ProductSyncVO getSyncProcess(String login, String taskId);


}
