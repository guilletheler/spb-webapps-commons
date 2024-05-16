package com.gt.toolbox.spb.webapps.payload;

import java.util.Collection;
import org.springframework.data.domain.Page;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageResponse<T> {

    public static <T> PageResponse<T> fromPage(Page<?> page, Collection<T> content) {
        var ret = new PageResponse<T>();
        ret.setContent(content);
        ret.setNumberOfElements(page.getNumberOfElements());
        ret.setPageIndex(page.getNumber());
        ret.setPageSize(page.getSize());
        ret.setTotalElements(page.getTotalElements());
        return ret;
    }

    long totalElements;

    Integer pageIndex;

    Integer pageSize;

    Integer numberOfElements;

    Collection<T> content;
}
