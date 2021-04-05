package com.izhiliu.erp.service.item;

import com.izhiliu.erp.service.item.dto.ProductMetaDataDTO;
import com.izhiliu.erp.service.module.metadata.basic.MetaDataObject;
import com.izhiliu.erp.service.module.metadata.convert.*;
import com.izhiliu.erp.web.rest.item.vm.ProductListVM;
import com.izhiliu.erp.web.rest.item.param.ClaimParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing ProductMetaData.
 */
public interface ProductMetaDataService {

    String CACHE_ONE = "product-meta-data-one";
    String CACHE_PAGE = "product-meta-data-page";

    /**
     * Save a productMetaData.
     *
     * @param productMetaDataDTO the entity to save
     * @return the persisted entity
     */
    ProductMetaDataDTO save(ProductMetaDataDTO productMetaDataDTO);

    /**
     * Get all the productMetaData.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    Page<ProductMetaDataDTO> findAll(Pageable pageable);


    /**
     * Get the "id" productMetaData.
     *
     * @param id the id of the entity
     * @return the entity
     */
    Optional<ProductMetaDataDTO> findOne(String id);

    /**
     * Delete the "id" productMetaData.
     *
     * @param ids the id of the entity
     */
    void delete(List<String> ids);

    Page<ProductListVM> pageByCurrentUser(String name, String loginId, Pageable pageable);

    void toShopee(ShopeeMetaDataConvert.ShopeeMetaData collected, String loginId, MetaDataObject.CollectController collectController);
    void alibabaToShopee(AlibabaMetaDataConvert.AlibabaMetaData collected, String loginId,MetaDataObject.CollectController collectController);
    void pingduoduoToShopee(PingduoduoMetaDataConvert.PingduoduoMeteData collected, String loginId,MetaDataObject.CollectController collectController);
    void alibabaToShopee(TaobaoMetaDataConvert.TaoBaoMetaData collected, String loginId,MetaDataObject.CollectController collectController);
    void alibabaToShopee(TianmaoMetaDataConvert.TianmaoMetaData alibabaMetaData, String loginId, MetaDataObject.CollectController collectController);
    void expressToShopee(ExpressMetaDataConvert.ExpressMetaData collected, String loginId,MetaDataObject.CollectController collectController);
    void lazadaToShopee(LazadaMetaDataConvert.LazadaMetaData collected, String loginId, MetaDataObject.CollectController collectController);
    void e7ToShopee(E7MetaDataConvert.E7MetaData collected, String loginId, MetaDataObject.CollectController collectController);

    /**
     * 采集
     */
    void collectToShopee(List<String> urls);

    /**
     * 认领
     */
    void claim(List<ClaimParam> claims);

    /**
     * 生成SkuCode编码
     */
    Long getSkuCode();

}
