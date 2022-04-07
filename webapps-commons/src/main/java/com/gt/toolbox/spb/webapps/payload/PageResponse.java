package com.gt.toolbox.spb.webapps.payload;

import java.util.List;

import lombok.Data;

@Data
public class PageResponse<T> {
    
    long totalElements;

    Integer pageIndex;

    Integer pageSize;

    Integer numberOfElements;

    List<T> content;
}
