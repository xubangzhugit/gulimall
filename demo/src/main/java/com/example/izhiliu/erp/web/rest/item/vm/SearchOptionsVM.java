package com.izhiliu.erp.web.rest.item.vm;

import com.izhiliu.core.domain.enums.ShopeeItemStatus;
import com.izhiliu.erp.domain.enums.LocalProductStatus;
import com.izhiliu.uaa.service.dto.ShopeeShopDTO;
import lombok.Data;
import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/2/19 11:33
 */
@Data
public class SearchOptionsVM implements Serializable {

    private static List<Entry> fields;

    private List<String> sources;
    private Map<String, List<ShopeeShopDTO>> shops;

    private List<LocalProductStatus> localStatus;
    private List<ShopeeItemStatus> remoteStatus;

    static {
        fields = new ArrayList<Entry>();
        fields.add(new Entry("name", "商品标题","search.options.name"));
        fields.add(new Entry("sku_code", "sku编码","search.options.sku.code"));
        fields.add(new Entry("shopee_item_id", "Shopee商品ID","search.options.shopee.item.id"));
        fields.add(new Entry("product_code", "商品编码","search.options.shopee.item.code"));
    }

    public List<Entry> getFields() {
        return fields;
    }

    @Getter
    public static class Entry {
        private String name;
        private String displayName;
        private String country;

        Entry(String name, String displayName,String country ) {
            this.name = name;
            this.displayName = displayName;
            this.country = country;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public void setCountry(String country) {
            this.country = country;
        }
    }
}
