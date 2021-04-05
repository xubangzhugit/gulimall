package com.izhiliu.erp.config.mq.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/3/7 20:40
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShopeeActionVO implements Serializable {
    private static final long serialVersionUID = 2944646005819511882L;
    private Integer action;

    private String loginId;
    private Long shopId;
    private Long itemId;
    private Long productId;

    private Integer batchCount;

    private String key;

    private Integer shopeeUpdateTime;

    private Integer dts;

    public enum Action {
        /**
         *
         */
        PULL(1, "拉取"),
        PUSH(2, "更新"),
        PUBLISH(3, "发布"),
        DISCOUNT(4, "折扣");
        ;

        private int code;
        private String info;

        Action(int code, String info) {
            this.code = code;
            this.info = info;
        }

        public int getCode() {
            return code;
        }

        public String getInfo() {
            return info;
        }
    }
}
