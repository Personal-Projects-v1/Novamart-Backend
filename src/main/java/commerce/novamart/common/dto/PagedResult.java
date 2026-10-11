package commerce.novamart.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Page envelope matching the frontend's {@code PaginatedResult<T>} (1-based page numbers). */
public record PagedResult<T>(List<T> items, long total, int page, int pageSize, int totalPages) {

    public static <E, T> PagedResult<T> of(Page<E> page, Function<E, T> mapper) {
        return new PagedResult<>(
                page.getContent().stream().map(mapper).toList(),
                page.getTotalElements(),
                page.getNumber() + 1,
                page.getSize(),
                Math.max(1, page.getTotalPages()));
    }

    public static <T> PagedResult<T> of(Page<T> page) {
        return of(page, Function.identity());
    }
}
