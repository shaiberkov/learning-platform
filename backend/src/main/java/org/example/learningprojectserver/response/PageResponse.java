package org.example.learningprojectserver.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** One page of a server-paginated list, carried in BasicResponse.data. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    public static final int MAX_PAGE_SIZE = 100;

    private List<T> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public static int clampSize(Integer size, int fallback) {
        if (size == null || size <= 0) return fallback;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    public static int clampPage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
