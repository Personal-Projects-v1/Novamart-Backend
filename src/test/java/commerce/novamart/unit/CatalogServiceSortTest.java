package commerce.novamart.unit;

import commerce.novamart.catalog.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogServiceSortTest {

    @Test
    void priceAscSortUsesPriceThenId() {
        Sort sort = CatalogService.sortFor("price-asc");
        assertThat(sort.getOrderFor("price").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(sort.getOrderFor("id")).isNotNull();
    }

    @Test
    void unknownSortFallsBackToRecommended() {
        Sort sort = CatalogService.sortFor(null);
        assertThat(sort.getOrderFor("featured").getDirection()).isEqualTo(Sort.Direction.DESC);
    }
}
