package com.izhiliu.erp.web.rest.item.param;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @author Twilight
 * @date 2021/2/20 10:09
 */
@Data
public class BatchDownloadImageQO {

    private String login;

    private String fileName;

    private String taskId;

    @NotEmpty
    private List<String> url;
}
