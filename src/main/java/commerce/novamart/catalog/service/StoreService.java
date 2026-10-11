package commerce.novamart.catalog.service;

import commerce.novamart.catalog.dto.CatalogDtos.StoreResponse;
import commerce.novamart.catalog.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository repository;

    public List<StoreResponse> list() {
        return repository.findAll(Sort.by("createdAt")).stream()
                .map(s -> new StoreResponse(s.getId().toString(), s.getName(), s.getCity(), s.getCountry(),
                        s.getAddress(), s.getHours(), s.getServices(), s.getLat(), s.getLng()))
                .toList();
    }
}
