package com.izhiliu.erp.service.item.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class ImageResult {
   private List<String> imageUrl;
   private boolean isError;
   private String errorInfo;
}
