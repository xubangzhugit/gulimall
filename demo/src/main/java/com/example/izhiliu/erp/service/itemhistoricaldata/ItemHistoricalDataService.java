package com.izhiliu.erp.service.itemhistoricaldata;

import com.izhiliu.erp.service.itemhistoricaldata.dto.ItemHistoricalDataDTO;
import com.izhiliu.erp.service.itemhistoricaldata.dto.ItemHistoricalDataStatusDTO;
import com.izhiliu.erp.web.rest.itemhistoricaldata.qo.ItemHistoricalDataQO;

import java.util.List;

public interface ItemHistoricalDataService  {

    /**
     * 保存数据
     * @param qos
     * @return
     */
    Boolean preserveData(List<ItemHistoricalDataQO> qos);

    /**
     * 将状态设置为下架或者删除
     * @param qos
     * @return
     */
    Boolean modifyDeletedOrUpdate(List<ItemHistoricalDataQO> qos);

    /**
     *  通过条件查询或者商品历史价格和销量
     * @param qo
     * @return
     */
    ItemHistoricalDataDTO getData(ItemHistoricalDataQO qo);

    List<ItemHistoricalDataStatusDTO> getUntreatedData(int size);

}
