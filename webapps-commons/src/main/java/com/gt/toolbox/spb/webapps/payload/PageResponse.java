package com.gt.toolbox.spb.webapps.payload;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import com.gt.toolbox.spb.webapps.commons.infra.dto.EntityDetailLevel;
import com.gt.toolbox.spb.webapps.commons.infra.dto.IDtoConverter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageResponse<T> {

    public static <T> PageResponse<T> fromPage(Page<?> page) {
        return fromPage(page, (Collection<T>) null);
    }

    @SuppressWarnings("unchecked")
    public static <T> PageResponse<T> fromPage(Page<?> page, Collection<T> content) {
        var ret = new PageResponse<T>();
        ret.setContent(Optional.ofNullable(content).orElse((Collection<T>) page.getContent()));
        ret.setNumberOfElements(page.getNumberOfElements());
        ret.setPageIndex(page.getNumber());
        ret.setPageSize(page.getSize());
        ret.setTotalElements(page.getTotalElements());
        return ret;
    }

    public static <E, T> PageResponse<T> fromPage(Page<E> page, IDtoConverter<E, T> converter) {
        return fromPage(page, converter, EntityDetailLevel.LIST);
    }

    public static <E, T> PageResponse<T> fromPage(Page<E> page, IDtoConverter<E, T> converter,
            EntityDetailLevel level) {
        var ret = new PageResponse<T>();
        ret.setContent(page.getContent().stream().map(e -> converter.toDto(e, level)).toList());
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
