package com.izhiliu.erp.service.itemhistoricaldata.impl;

import com.baomidou.lock.annotation.Lock4j;
import com.izhiliu.erp.Enum.ItemHistoricalDataEnum;
import com.izhiliu.erp.common.CommonUtils;
import com.izhiliu.erp.domain.itemhistoricaldata.ItemHistoricalDataRecord;
import com.izhiliu.erp.domain.itemhistoricaldata.ItemHistoricalDataStatus;
import com.izhiliu.erp.service.itemhistoricaldata.ItemHistoricalDataService;
import com.izhiliu.erp.service.itemhistoricaldata.dto.ItemHistoricalDataDTO;
import com.izhiliu.erp.service.itemhistoricaldata.dto.ItemHistoricalDataStatusDTO;
import com.izhiliu.erp.web.rest.errors.LuxServerErrorException;
import com.izhiliu.erp.web.rest.itemhistoricaldata.qo.ItemHistoricalDataQO;
import com.izhiliu.erp.web.rest.itemhistoricaldata.vm.ItemHistoricalDataVM;
import com.izhiliu.erp.web.rest.itemhistoricaldata.vm.MakeItemHistoricalDataVM;
import org.bson.types.ObjectId;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ItemHistoricalDataServiceImpl implements ItemHistoricalDataService {
    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private RedissonClient redissonClient;

    private static ExecutorService selectExecutor = new ThreadPoolExecutor(10, 50, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.DiscardOldestPolicy());
    private static ExecutorService unpateStatusExecutor = new ThreadPoolExecutor(10, 50, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.DiscardOldestPolicy());
    private static ExecutorService deleteStatusExecutor = new ThreadPoolExecutor(5, 50, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.DiscardOldestPolicy());

    private static final Logger log = LoggerFactory.getLogger(ItemHistoricalDataServiceImpl.class);

    @Override
    public Boolean preserveData(List<ItemHistoricalDataQO> qos) {
        if (CommonUtils.isBlank(qos)) {
            return false;
        }
        qos.stream().filter(f -> CommonUtils.isNotBlank(f.getItemId()) && CommonUtils.isNotBlank(f.getShopId()) && CommonUtils.isNotBlank(f.getUrl()) && CommonUtils.isNotBlank(f.getMinPrice()) && CommonUtils.isNotBlank(f.getMaxPrice()))
                .forEach(m -> {
                    selectExecutor.execute(() -> {
                        final String itemId = m.getItemId();
                        final String shopId = m.getShopId();
                        RLock rLock = redissonClient.getLock(itemId + shopId);
                        if (rLock.tryLock()) {
                            try {
                                handleItemHistoricalDataRecord(convertDataToItemHistoricalDataRecord(m));
                            } catch (NullPointerException e) {
                                // todo 线上有部分商品报空指针错误，先放在线上观察几天，查找原因
                                log.error("传入的参数有null,参数：{}", m.toString(), e);
                                throw new NullPointerException();
                            } catch (Exception e) {
                                log.error("处理店铺id:{};商品id:{}，失败，失败链接：{}", shopId, itemId, m.getUrl());
                                throw new LuxServerErrorException("处理商品失败");
                            } finally {
                                rLock.unlock();
                            }
                        }
                    });
                });
        return true;
    }

    /**
     * 循环商品，并分条件存储
     *
     * @param itemHistoricalDataRecord
     */
    private void handleItemHistoricalDataRecord(ItemHistoricalDataRecord itemHistoricalDataRecord) {
        Query query = new Query();
        Criteria criteriaItemId = Criteria.where("itemId").is(itemHistoricalDataRecord.getItemId());
        Criteria criteriaShopId = Criteria.where("shopId").is(itemHistoricalDataRecord.getShopId());
        query.addCriteria(criteriaItemId);
        query.addCriteria(criteriaShopId);
        // 判断该商品id是否存在
        boolean exists = mongoTemplate.exists(query, ItemHistoricalDataRecord.class);
        if (!exists) {
            // 商品id不存在,增加到主表
            mongoTemplate.save(itemHistoricalDataRecord);
            // 增加到状态表
            mongoTemplate.save(convertItemHistoricalDataStatus(itemHistoricalDataRecord));
            return;
        }
        Query fileQuery = new Query(Criteria.where("data").elemMatch(Criteria.where("gmtDate").is(LocalDate.now().toString())));
        fileQuery.fields().include("data");
        fileQuery.addCriteria(criteriaItemId);
        fileQuery.addCriteria(criteriaShopId);

        ItemHistoricalDataRecord getItemHistoricalDataRecord = mongoTemplate.findOne(fileQuery, ItemHistoricalDataRecord.class);
        if (CommonUtils.isNotBlank(getItemHistoricalDataRecord)) {
            // 批量更新
            itemHistoricalDataRecord.setId(getItemHistoricalDataRecord.getId());
            updateOne(itemHistoricalDataRecord);
            // 更新状态表
            updateStatus(itemHistoricalDataRecord);
            return;
        }
        addDaily(itemHistoricalDataRecord);
        updateStatus(itemHistoricalDataRecord);
    }

    /**
     * 单个更新
     */
    private void updateOne(ItemHistoricalDataRecord itemHistoricalDataRecord) {
        Update update = new Update();
        Query fileQuery = new Query(Criteria.where("data").elemMatch(Criteria.where("gmtDate").is(LocalDate.now().toString())));
        fileQuery.fields().include("data");
        fileQuery.addCriteria(Criteria.where("_id").is(itemHistoricalDataRecord.getId()));
        update.set("allSales", itemHistoricalDataRecord.getAllSales());
        update.set("url", itemHistoricalDataRecord.getUrl());
        update.set("data.$.minPrice", itemHistoricalDataRecord.getData().get(0).getMinPrice());
        update.set("data.$.maxPrice", itemHistoricalDataRecord.getData().get(0).getMaxPrice());
        update.set("data.$.updateTime", itemHistoricalDataRecord.getData().get(0).getUpdateTime());
//        update.set("data.$.daySales", itemHistoricalDataRecord.getData().get(0).getDaySales());
        update.set("data.$.allSales", itemHistoricalDataRecord.getAllSales());
        mongoTemplate.updateMulti(fileQuery, update, ItemHistoricalDataRecord.class);
    }

    /**
     * 新增每日数据
     */
    private void addDaily(ItemHistoricalDataRecord itemHistoricalDataRecord) {
        Query query = new Query();
        query.addCriteria(Criteria.where("itemId").is(itemHistoricalDataRecord.getItemId()));
        query.addCriteria(Criteria.where("shopId").is(itemHistoricalDataRecord.getShopId()));
        Update update = new Update();
        update.set("allSales", itemHistoricalDataRecord.getAllSales());
        update.set("status", itemHistoricalDataRecord.getStatus());
        update.addToSet("data", itemHistoricalDataRecord.getData().get(0));
        mongoTemplate.upsert(query, update, ItemHistoricalDataRecord.class);
    }

    /**
     * 修改状态表
     */
    private void updateStatus(ItemHistoricalDataRecord itemHistoricalDataRecord) {
        Query query = new Query();
        query.addCriteria(Criteria.where("itemId").is(itemHistoricalDataRecord.getItemId()));
        query.addCriteria(Criteria.where("shopId").is(itemHistoricalDataRecord.getShopId()));
        Update update = new Update();
        update.set("status", itemHistoricalDataRecord.getStatus());
        update.set("allSales", itemHistoricalDataRecord.getAllSales());
        update.set("updateTime", itemHistoricalDataRecord.getData().get(0).getUpdateTime());
        mongoTemplate.updateMulti(query, update, ItemHistoricalDataStatus.class);
    }


    @Override
    public Boolean modifyDeletedOrUpdate(List<ItemHistoricalDataQO> qos) {
        deleteStatusExecutor.execute(() -> {
            BulkOperations operations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ItemHistoricalDataStatus.class);
            BulkOperations operation = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ItemHistoricalDataRecord.class);
            List<Query> queryList = new ArrayList<>();
            List<Pair<Query, Update>> pairs = qos.stream().filter(l -> l.getStatus().equals(ItemHistoricalDataEnum.DELETED.getCode()))
                    .map(f -> {
                        Pair<Query, Update> updatePair;
                        Query query = new Query();
                        queryList.add(query.addCriteria(Criteria.where("itemId").is(f.getItemId())));
                        queryList.add(query.addCriteria(Criteria.where("shopId").is(f.getShopId())));
                        Update update = new Update();
                        update.set("status", ItemHistoricalDataEnum.getCommonStauts(f.getStatus()));
                        updatePair = Pair.of(query, update);
                        return updatePair;
                    }).collect(Collectors.toList());
            // 在状态表中删除
            try {
                operations.remove(queryList);
                operations.execute();
                // 在详情表中记录下架或者删除标记
                operation.updateMulti(pairs);
                operation.execute();
            } catch (Exception e) {
                log.error("删除商品历史价格和销量失败原因:{},", e);
                throw new LuxServerErrorException("删除商品历史价格和销量失败：" + e);
            }
        });
        return true;
    }

    @Override
    public ItemHistoricalDataDTO getData(ItemHistoricalDataQO qo) {
        final String itemId = qo.getItemId();
        final String shopId = qo.getShopId();

        final LocalDateTime beginDateTime = qo.getBeginDateTime();

        LocalDate beginDate = beginDateTime.toLocalDate();
        Instant beginTime = beginDate.atStartOfDay().toInstant(ZoneOffset.ofHours(8));

        LocalDateTime endDateTime = CommonUtils.isBlank(qo.getEndDateTime()) ? LocalDateTime.now() : qo.getEndDateTime();
        LocalDate endDate = endDateTime.plus(Duration.ofDays(1)).toLocalDate();

        long day = 1;
        Double minPrice = 0d;
        Double maxPrice = 0d;
        Long allSales = 0L;
        Long avgDaySales = 0L;

        ItemHistoricalDataDTO itemHistoricalDataDTO = new ItemHistoricalDataDTO();
        if (CommonUtils.isBlank(qo.getBeginDateTime())) {
            throw new LuxServerErrorException("请选择开始时间");
        }

        if (endDate.minusDays(1).compareTo(LocalDate.now()) > 0) {
            throw new LuxServerErrorException("查询的结束时间不能大于当前时间");
        }


        // 查询需要的数据
        Query query = new Query();
        query.addCriteria(Criteria.where("itemId").is(itemId));
        query.addCriteria(Criteria.where("shopId").is(shopId));
        ItemHistoricalDataRecord itemHistoricalDataRecords = mongoTemplate.findOne(query, ItemHistoricalDataRecord.class);
        if (CommonUtils.isBlank(itemHistoricalDataRecords)) {
            return itemHistoricalDataDTO;
        }

        // 获取要查询时间的天数
        day = beginDate.until(endDate, ChronoUnit.DAYS);

        List<ItemHistoricalDataVM> itemHistoricalDataVMS = new ArrayList<>();
        List<ItemHistoricalDataVM> finalItemHistoricalDataVMList = new ArrayList();

        // 获取真实的数据
        itemHistoricalDataRecords.getData().stream()
                .filter(f -> stringToLocalDate(f.getGmtDate()).compareTo(beginDate) > -1 && stringToLocalDate(f.getGmtDate()).compareTo(endDate) < 1 && CommonUtils.isNotBlank(f.getGmtDate()))
                .forEach(m -> {
                    ItemHistoricalDataVM itemHistoricalDataVM = new ItemHistoricalDataVM();
                    BeanUtils.copyProperties(m, itemHistoricalDataVM);
                    finalItemHistoricalDataVMList.add(itemHistoricalDataVM);
                });

        // 如果查询的时间范围内真实数据存在并且只有一个，则当天的日销量=总销量
        if (finalItemHistoricalDataVMList.size() == 1) {
            itemHistoricalDataVMS = finalItemHistoricalDataVMList;
            itemHistoricalDataVMS.get(0).setDaySales(finalItemHistoricalDataVMList.get(0).getAllSales());
        }

        if (CommonUtils.isNotBlank(finalItemHistoricalDataVMList) && finalItemHistoricalDataVMList.size() > 1) {
            minPrice = getMinPriceByTimeLimit(finalItemHistoricalDataVMList);
            maxPrice = getMaxPriceByTimeLimit(finalItemHistoricalDataVMList);
            allSales = finalItemHistoricalDataVMList.get(finalItemHistoricalDataVMList.size() - 1).getAllSales();
            // 计算处理好的真实数据
            Map<String, Object> getdailySales = getDaySales(finalItemHistoricalDataVMList, endDate);
            avgDaySales = (Long) getdailySales.get("avgSales");
            itemHistoricalDataVMS = (List<ItemHistoricalDataVM>) getdailySales.get("realData");

        }


        // 制造空数据
        List<ItemHistoricalDataVM> data = new ArrayList<>();
        if (day != itemHistoricalDataVMS.size()) {
            MakeItemHistoricalDataVM makeItemHistoricalDataVM = coverMakeItemHistoricalDataVM(day, beginTime, avgDaySales, minPrice, maxPrice, allSales);
            data = makeData(makeItemHistoricalDataVM);
        }

        // 将真实数据和空数据合并
        List<ItemHistoricalDataVM> collect = mergeData(itemHistoricalDataVMS, data);

        // 封装数据返回给前端
        itemHistoricalDataRecords.setData(collect);
        BeanUtils.copyProperties(itemHistoricalDataRecords, itemHistoricalDataDTO);
        return itemHistoricalDataDTO;
    }

    @Override
    @Lock4j(expire = 60000, tryTimeout = 100)
    public List<ItemHistoricalDataStatusDTO> getUntreatedData(int size) {
        if (CommonUtils.isBlank(size)) {
            throw new LuxServerErrorException("指定条数能为空");
        }
        Query query = new Query();
        List<ItemHistoricalDataStatusDTO> itemHistoricalDataStatusDTOS = new ArrayList();
        query.addCriteria(Criteria.where("status").is("UNTREATED"));
        query.limit(size);
        BulkOperations operation = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, ItemHistoricalDataStatus.class);
        List<ItemHistoricalDataStatus> itemHistoricalDataStatus = mongoTemplate.find(query, ItemHistoricalDataStatus.class);
        if (CommonUtils.isBlank(itemHistoricalDataStatus)) {
            return itemHistoricalDataStatusDTOS;
        }
        itemHistoricalDataStatusDTOS = itemHistoricalDataStatus.stream().map(m -> {
            ItemHistoricalDataStatusDTO itemHistoricalDataStatusDTO = ItemHistoricalDataStatusDTO.builder()
                    .id(m.getId())
                    .itemId(m.getItemId())
                    .shopId(m.getShopId())
                    .url(m.getUrl())
                    .allSales(m.getAllSales().toString())
                    .build();
            return itemHistoricalDataStatusDTO;
        }).collect(Collectors.toList());
        List<ItemHistoricalDataStatusDTO> finalItemHistoricalDataStatusDTOS = itemHistoricalDataStatusDTOS;
        unpateStatusExecutor.execute(() -> {
            List<Pair<Query, Update>> pairs = finalItemHistoricalDataStatusDTOS.stream().map(m -> {
                Pair<Query, Update> updatePair;
                Query updateQuery = new Query();
                updateQuery.addCriteria(Criteria.where("_id").is(new ObjectId(m.getId())));
                Update update = new Update();
                update.set("status", ItemHistoricalDataEnum.PROCESSING.getStatus());
                updatePair = Pair.of(updateQuery, update);
                return updatePair;
            }).collect(Collectors.toList());
            operation.updateMulti(pairs);
            operation.execute();
        });
        return itemHistoricalDataStatusDTOS;
    }

    /**
     * 将前端传过来的数据转换为ItemHistoricalDataRecord
     *
     * @param qo
     * @return
     */
    private ItemHistoricalDataRecord convertDataToItemHistoricalDataRecord(ItemHistoricalDataQO qo) {
        final String minPrice = CommonUtils.isNotBlank(qo.getMinPrice()) ? qo.getMinPrice().toString() : String.valueOf(0);
        final String maxPrice = CommonUtils.isNotBlank(qo.getMinPrice()) ? qo.getMaxPrice().toString() : String.valueOf(0);
        final Long allSales = CommonUtils.isNotBlank(qo.getMinPrice()) ? qo.getAllSales() : Long.valueOf(-1L);
        ItemHistoricalDataVM itemHistoricalDataVM = ItemHistoricalDataVM.builder()
                .itemId(qo.getItemId())
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .gmtDate(LocalDate.now().toString())
                .updateTime(qo.getUpdateTime())
                .allSales(allSales)
                .build();
        ItemHistoricalDataRecord itemHistoricalDataRecord = ItemHistoricalDataRecord.builder()
                .id(qo.getId())
                .itemId(qo.getItemId())
                .shopId(qo.getShopId())
                .url(qo.getUrl())
                .allSales(allSales)
                .status(ItemHistoricalDataEnum.COMPLETE.getStatus())
                .data(Collections.singletonList(itemHistoricalDataVM))
                .build();
        return itemHistoricalDataRecord;
    }

    /**
     * 转换为状态表
     *
     * @param itemHistoricalDataRecord
     * @return
     */
    private ItemHistoricalDataStatus convertItemHistoricalDataStatus(ItemHistoricalDataRecord itemHistoricalDataRecord) {
        ItemHistoricalDataStatus itemHistoricalDataStatus = ItemHistoricalDataStatus.builder()
                .itemId(itemHistoricalDataRecord.getItemId())
                .shopId(itemHistoricalDataRecord.getShopId())
                .url(itemHistoricalDataRecord.getUrl())
                .allSales(itemHistoricalDataRecord.getAllSales())
                .status(itemHistoricalDataRecord.getStatus())
                .updateTime(Instant.now())
                .build();
        return itemHistoricalDataStatus;
    }

    /**
     * 秒 分 时 天 月 周  年（可选）
     * 0  0  0 *  *  ?
     */
    @Scheduled(cron = "0 5 1  * * ?")
    public void updateStatusTableByTimer() {
        RLock rLock = redissonClient.getLock("updateStatusTableByTimer");
        if (rLock.isLocked()) {
            return;
        }
        if (rLock.tryLock()) {
            try {
                Query query = new Query();
                query.addCriteria(Criteria.where("updateTime").lt(Instant.now()));
                Update update = new Update();
                update.set("status", ItemHistoricalDataEnum.UNTREATED.getStatus());
                update.set("updateTime", Instant.now());
                mongoTemplate.updateMulti(query, update, ItemHistoricalDataStatus.class);
            } catch (Exception e) {
                log.error("将所有商品状态更新为未处理出错：{}", e);
            } finally {
                rLock.unlock();
            }
        }

    }


    /**
     * 秒 分 时 天 月 周  年（可选）
     * 0  30  * *  *  ?
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void monitorUpdateStatusTable() {
        RLock rLock = redissonClient.getLock("monitorUpdateStatusTable");
        if (rLock.isLocked()) {
            return;
        }
        if (rLock.tryLock()) {
            try {
                // 通过时间小于当前时间,处理中的状态进行查询
                Query query = new Query();
                query.addCriteria(Criteria.where("status").is(ItemHistoricalDataEnum.PROCESSING.getStatus()));
                query.addCriteria(Criteria.where("updateTime").lt(Instant.now().minus(Duration.ofMinutes(30))));
                // 修改状态为未处理
                Update update = new Update();
                update.set("status", ItemHistoricalDataEnum.UNTREATED.getStatus());
                mongoTemplate.updateMulti(query, update, ItemHistoricalDataStatus.class);
            } catch (Exception e) {
                log.error("将商品处理中变为未处理状态失败：{}", e);
            } finally {
                rLock.unlock();
            }
        }
    }


    /**
     * instant转date
     *
     * @param instant
     * @return
     */
    private Date instantToDate(Instant instant) {
        return new Date(instant.toEpochMilli());
    }

    /**
     * date转字符串
     *
     * @param date
     * @return
     */
    private String dateToString(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }


    /**
     * String 转LocalDate
     *
     * @param str
     * @return
     */
    private LocalDate stringToLocalDate(String str) {
        return LocalDate.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }


    /**
     * 获取查询指定时间范围内的最低价格
     *
     * @param itemHistoricalDataData
     * @return
     */
    private double getMinPriceByTimeLimit(List<ItemHistoricalDataVM> itemHistoricalDataData) {
        double minPrice = itemHistoricalDataData.stream()
                .filter(f -> CommonUtils.isNotBlank(f.getMinPrice()))
                .mapToDouble(d -> Double.parseDouble(d.getMinPrice()))
                .min().orElse(0);
        return minPrice;
    }

    /**
     * 获取查询指定时间范围内的最高价格
     *
     * @param itemHistoricalDataData
     * @return
     */
    private double getMaxPriceByTimeLimit(List<ItemHistoricalDataVM> itemHistoricalDataData) {
        double maxPrice = itemHistoricalDataData.stream()
                .filter(f -> CommonUtils.isNotBlank(f.getMinPrice()))
                .mapToDouble(d -> Double.parseDouble(d.getMinPrice()))
                .max().orElse(0);
        return maxPrice;
    }

    /**
     * 将变量转换为自己制造的数据
     *
     * @param day
     * @param beginTime
     * @param avgDaySales
     * @param minPrice
     * @param maxPrice
     * @param allSales
     * @return
     */
    private MakeItemHistoricalDataVM coverMakeItemHistoricalDataVM(long day, Instant beginTime, Long avgDaySales, Double minPrice, Double maxPrice, Long allSales) {
        MakeItemHistoricalDataVM makeItemHistoricalDataVM = MakeItemHistoricalDataVM.builder()
                .day(day)
                .beginTime(beginTime)
                .avgDaySales(avgDaySales)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .allSales(allSales)
                .build();
        return makeItemHistoricalDataVM;
    }

    /**
     * 制造不存在的数据
     */
    private List<ItemHistoricalDataVM> makeData(MakeItemHistoricalDataVM makeItemHistoricalDataVM) {
        List<ItemHistoricalDataVM> data = new ArrayList<>();
        for (int i = 0; i < makeItemHistoricalDataVM.getDay(); i++) {
            Date tempDate = instantToDate(makeItemHistoricalDataVM.getBeginTime().plus(Duration.ofDays(i)));
            ItemHistoricalDataVM itemHistoricalDataVM = new ItemHistoricalDataVM();
            itemHistoricalDataVM.setGmtDate(dateToString(tempDate));
            itemHistoricalDataVM.setDaySales(makeItemHistoricalDataVM.getAvgDaySales());
            itemHistoricalDataVM.setAllSales(makeItemHistoricalDataVM.getAllSales());
            itemHistoricalDataVM.setMinPrice(makeItemHistoricalDataVM.getMinPrice().toString());
            itemHistoricalDataVM.setMaxPrice(makeItemHistoricalDataVM.getMaxPrice().toString());
            itemHistoricalDataVM.setUpdateTime(makeItemHistoricalDataVM.getBeginTime().plus(Duration.ofDays(i)));
            data.add(itemHistoricalDataVM);
        }
        return data;
    }

    /**
     * 获取已有数据的天销量
     *
     * @param itemHistoricalDataData
     * @return
     */
    private Map<String, Object> getDaySales(List<ItemHistoricalDataVM> itemHistoricalDataData, LocalDate endDate) {
        Long avgSales = 0L;
        int size = itemHistoricalDataData.size();
        for (int i = 0; i < size - 1; i++) {
            Long daySales = itemHistoricalDataData.get(i + 1).getAllSales() - itemHistoricalDataData.get(i).getAllSales();
            avgSales += daySales;
            itemHistoricalDataData.get(i).setDaySales(Math.abs(daySales));
        }

        // 如果查询的数据是今天的数据吗，则设置最后一天的销量为0
        if (endDate.minusDays(1).compareTo(LocalDate.now()) == 0) {
            itemHistoricalDataData.get(size - 1).setDaySales(0L);
        }

        // 如果查找的数据多余今天的，则去掉最后一天的数据
        if (stringToLocalDate(itemHistoricalDataData.get(size - 1).getGmtDate()).compareTo(endDate) >= 0) {
            itemHistoricalDataData.remove(size - 1);
        }
        if (CommonUtils.isNotBlank(itemHistoricalDataData)) {
            avgSales = avgSales / itemHistoricalDataData.size();
        }
        Map<String, Object> map = new HashMap();
        map.put("avgSales", avgSales);
        map.put("realData", itemHistoricalDataData);
        return map;
    }


    /**
     * 将真实数据和制造数据合并
     *
     * @param realItemHistoricalDataData
     * @param makeItemHistoricalDataData
     * @return
     */
    private List<ItemHistoricalDataVM> mergeData(List<ItemHistoricalDataVM> realItemHistoricalDataData, List<ItemHistoricalDataVM> makeItemHistoricalDataData) {
        List<ItemHistoricalDataVM> itemHistoricalDataVMList = Stream.of(realItemHistoricalDataData, makeItemHistoricalDataData)
                .flatMap(f -> f.stream())
                .filter(CommonUtils.distinctByKey(d -> d.getGmtDate()))
                .sorted(Comparator.comparing(ItemHistoricalDataVM::getGmtDate))
                .collect(Collectors.toList());
        return itemHistoricalDataVMList;
    }
}
