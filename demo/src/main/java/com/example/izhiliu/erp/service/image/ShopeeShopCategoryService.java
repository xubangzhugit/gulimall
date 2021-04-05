package com.izhiliu.erp.service.image;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.izhiliu.erp.domain.item.ShopeeShopCategory;
import com.izhiliu.erp.service.image.dto.ShopeeShopCategoryDTO;
import com.izhiliu.erp.service.image.result.ShopCategorySelect;
import com.izhiliu.erp.web.rest.item.vm.ProductListVM_V21;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;


public interface ShopeeShopCategoryService extends CustomizeBaseService<ShopeeShopCategory, ShopeeShopCategoryDTO> {

    ShopeeShopCategoryDTO selectById(Long id);

    void batchEnabled(Integer status, ArrayList<Long> categoryIds);

    IPage<ProductListVM_V21> queryProductByProductId(Long cateagoryId, Long productItemId, Page page);

    @Getter
    enum Status {
        NORMAL(1, "NORMAL", "NORMAL"),
        INACTIVE(2, "INACTIVE", "INACTIVE"),
        DELETED(3, "DELETED", "DELETED");

        private int id;
        private String status;
        private String globalization;

        Status(int id, String status, String globalization) {
            this.id = id;
            this.status = status;
            this.globalization = globalization;
        }

        public static Status getStatus(String status) {
            if (Objects.isNull(status)) {
                return null;
            }
            return code(value -> value.getStatus().equals(status));
        }

        public static Status getStatus(int id) {
            if (Objects.isNull(id)) {
                return null;
            }
            return code(value -> value.getId() == id);
        }

        public static Status code(Predicate<Status> statusPredicate) {
            for (Status value : Status.values()) {
                if (statusPredicate.test(value)) {
                    return value;
                }
            }
            return null;
        }
    }

    IPage<ShopeeShopCategoryDTO> searchProductByCurrentUser(ShopCategorySelect productSearchStrategy);

    ResponseEntity<Object> insert(ShopeeShopCategoryDTO aDto);

    ResponseEntity<Object> put(ShopeeShopCategoryDTO aDto, String parentLoginId);

    ResponseEntity handleItem(ShopeeShopCategoryDTO aDto, boolean isCheck);

    void syncCategory(List<Long> categoreyIds);


    void remove(List<Long> deleteId);

    void sync(List<Long> shopIds);

    Long checkeV2(Long shopId);

    List<Long> checkeV2(List<Long> shopIds);
}
