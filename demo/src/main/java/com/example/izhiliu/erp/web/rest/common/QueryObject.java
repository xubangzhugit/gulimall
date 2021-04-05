package com.izhiliu.erp.web.rest.common;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * describe:
 * <p>
 *
 * @author cheng
 * @date 2019/4/28 14:59
 */
@Data
@Accessors(chain = true)
public class QueryObject {

    private static final long serialVersionUID = 2145241879912328923L;

    /**
     * ${field} LINK  '%${keyword}%'
     */
    private String field;
    private String keyword;
    private String type;
    private String warehouseId;
    private String name;
    /**
     * gmt_create BETWEEN ${startDate}, ${endDate}
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endDate;

    /**
     * gmt_modified BETWEEN ${startDate}, ${endDate}
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date ustartDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date uendDate;

    @NotNull
    private Long page;
    @NotNull
    private Long size;
}
