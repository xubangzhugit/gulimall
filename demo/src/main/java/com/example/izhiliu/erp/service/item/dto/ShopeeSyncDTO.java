package com.izhiliu.erp.service.item.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ShopeeSyncDTO implements Serializable {
    private static final long serialVersionUID = -7978306996967144374L;

    private String id;

    private int count;

    private int succeedCount;

    private int loserCount;

    private int ifSync;

    private int succeedRatio;

    private int shopCount;

    private String shopName;
    private String key;

    private int status;

    private String msg;

    private Map<String, Integer> repetition = new HashMap<>();
    private Map<Long, ShopeeSyncDTO> map = new HashMap<>();

    private List<ShopeeSyncDTO.LoserDetails> loserDetails = new ArrayList<>();

    private List<Long> shopIds;

    private List<Long> itmeIds;

    private String dateTime;

    @Data
    public static class LoserDetails {
        private Long itemId;

        private String cause;

        public LoserDetails() {

        }
        public LoserDetails(Long itemId, String cause) {
            this.itemId = itemId;
            this.cause = cause;
        }
    }
    public ShopeeSyncDTO() {
    }

    public ShopeeSyncDTO(int ifSync) {
        this.ifSync = ifSync;
    }

    public ShopeeSyncDTO(String id, int count, int succeedCount, int loserCount) {
        this.id = id;
        this.count = count;
        this.succeedCount = succeedCount;
        this.loserCount = loserCount;
    }

    public ShopeeSyncDTO(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "ShopeeSyncDTO{" +
                "id=" + id +
                ", count=" + count +
                ", succeedCount=" + succeedCount +
                ", loserCount=" + loserCount +
                '}';
    }
}
