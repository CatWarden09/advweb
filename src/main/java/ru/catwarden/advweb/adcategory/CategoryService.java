package ru.catwarden.advweb.adcategory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.catwarden.advweb.ad.AdvertisementService;
import ru.catwarden.advweb.ad.dto.AdvertisementResponse;
import ru.catwarden.advweb.ad.dto.AdvertisementSearchFilter;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryRequest;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryUpdateRequest;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryResponse;
import ru.catwarden.advweb.ad.AdvertisementRepository;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.exception.InvalidRelationException;
import ru.catwarden.advweb.exception.OperationNotAllowedException;
import ru.catwarden.advweb.security.SecurityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final AdvertisementService advertisementService;
    private final AdvertisementRepository advertisementRepository;
    private final AdvertisementCategoryMapper advertisementCategoryMapper;

    @Cacheable(value = "categories", key = "#id")
    public AdvertisementCategoryResponse getCategory(Long id){
        AdvertisementCategory advertisementCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(AdvertisementCategory.class, id));

        return advertisementCategoryMapper.toResponse(advertisementCategory);
    }

    @Cacheable(value = "categories", key = "'all'")
    public List<AdvertisementCategoryResponse> getAllCategories(){
        return categoryRepository.findByParentIsNull()
                .stream()
                .map(advertisementCategoryMapper::toResponse)
                .toList();
    }

    @Cacheable(value = "categories", key = "'sub-' + #id")
    public List<AdvertisementCategoryResponse> getSubcategories(Long id){
        AdvertisementCategory parent = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(AdvertisementCategory.class, id));

        return categoryRepository.findByParent(parent)
                .stream()
                .map(advertisementCategoryMapper::toResponse)
                .toList();
    }

    @Cacheable(
            value = "category-advertisements-list",
            key = "'cat-' + #id + '-p-' + #pageable.pageNumber + '-s-' + #pageable.pageSize"
    )
    public Page<AdvertisementResponse> getCategoryAds(Pageable pageable, Long id){
        AdvertisementSearchFilter filter = new AdvertisementSearchFilter();
        filter.setCategoryId(id);

        return advertisementService.getAdvertisementsByFilter(pageable, filter);

    }

    @Cacheable(
            value = "subcategory-advertisements-list",
            key = "'cat-' + #parentId + '-sub-' + #subId + '-p-' + #pageable.pageNumber + '-s-' + #pageable.pageSize"
    )
    public Page<AdvertisementResponse> getSubcategoryAds(Pageable pageable, Long parentId, Long subId){
        AdvertisementSearchFilter filter = new AdvertisementSearchFilter();
        filter.setCategoryId(parentId);
        filter.setSubcategoryId(subId);

        return advertisementService.getAdvertisementsByFilter(pageable, filter);
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void createCategory(AdvertisementCategoryRequest advertisementCategoryRequest){
        AdvertisementCategory advertisementCategory = advertisementCategoryMapper.toEntity(advertisementCategoryRequest);
        categoryRepository.save(advertisementCategory);

        log.info(
                "AUDIT category created: categoryId={}, name={}, parentId={}, actorId={}",
                advertisementCategory.getId(),
                advertisementCategory.getName(),
                getParentId(advertisementCategory),
                SecurityUtils.getCurrentUserKeycloakId()
        );
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void createSubcategories(Long id, List<AdvertisementCategoryRequest> subcategoryList){
        AdvertisementCategory parent = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(AdvertisementCategory.class, id));

        if (parent.getParent() != null){
            throw new InvalidRelationException("Cannot create subcategory for a subcategory",
                    Map.of("Parent id:", parent.getId()));
        }

        // DONE add batch updating
        List<AdvertisementCategory> advertisementSubcategories = new ArrayList<>();
        for(AdvertisementCategoryRequest advertisementCategoryRequest : subcategoryList){
            AdvertisementCategory advertisementSubcategory = advertisementCategoryMapper.toEntity(advertisementCategoryRequest);
            advertisementSubcategory.setParent(parent);

            advertisementSubcategories.add(advertisementSubcategory);
        }
        categoryRepository.saveAll(advertisementSubcategories);

        log.info(
                "AUDIT category subcategories created: parentId={}, createdCount={}, actorId={}",
                parent.getId(),
                advertisementSubcategories.size(),
                SecurityUtils.getCurrentUserKeycloakId()
        );
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void updateCategory(Long id, AdvertisementCategoryUpdateRequest advertisementCategoryUpdateRequest){
        AdvertisementCategory advertisementCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(AdvertisementCategory.class, id));

        if(advertisementCategory.getName().equals(advertisementCategoryUpdateRequest.getName())) {
            throw new OperationNotAllowedException("New category name is the same as the previous one",
                    Map.of("Category id", advertisementCategory.getId(), "Current name:", advertisementCategory.getName(), "Passed name:", advertisementCategoryUpdateRequest.getName()));
        }
        String previousName = advertisementCategory.getName();
        advertisementCategory.setName(advertisementCategoryUpdateRequest.getName());
        categoryRepository.save(advertisementCategory);

        log.info(
                "AUDIT category updated: categoryId={}, previousName={}, newName={}, actorId={}",
                advertisementCategory.getId(),
                previousName,
                advertisementCategory.getName(),
                SecurityUtils.getCurrentUserKeycloakId()
        );

    }

    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id){
        AdvertisementCategory advertisementCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(AdvertisementCategory.class, id));

        if(advertisementRepository.existsByCategory(advertisementCategory)
        || advertisementRepository.existsBySubcategory(advertisementCategory)){
            throw new OperationNotAllowedException("Cannot delete category with advertisements",
                    Map.of("Category id:", id));
        }

        if (!categoryRepository.findByParent(advertisementCategory).isEmpty()) { // .isEmpty > != null (null = [])
            throw new OperationNotAllowedException("Cannot delete category with subcategories",
                    Map.of("Category id:", id));
        }

        categoryRepository.delete(advertisementCategory);

        log.info(
                "AUDIT category deleted: categoryId={}, name={}, parentId={}, actorId={}",
                advertisementCategory.getId(),
                advertisementCategory.getName(),
                getParentId(advertisementCategory),
                SecurityUtils.getCurrentUserKeycloakId()
        );
    }

    private Long getParentId(AdvertisementCategory category) {
        return category.getParent() != null ? category.getParent().getId() : null;
    }
}
