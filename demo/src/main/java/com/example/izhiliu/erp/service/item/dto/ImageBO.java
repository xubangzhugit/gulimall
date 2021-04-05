package com.izhiliu.erp.service.item.dto;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@ToString
@Accessors(chain = true)
public class ImageBO implements Comparable<ImageBO>{
    private int position;
    private String url;
    private boolean isError;
    private String errorInfo;

    @Override
    public int compareTo(ImageBO o) {
        return this.getPosition() - o.getPosition();
    }
}
