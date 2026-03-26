package ru.catwarden.advweb.adcategory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryRequest;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryUpdateRequest;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryResponse;
import ru.catwarden.advweb.ad.AdvertisementRepository;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.exception.InvalidRelationException;
import ru.catwarden.advweb.exception.OperationNotAllowedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {
    private final CategoryRepository categoryRepository;
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

    public List<AdvertisementCategoryResponse> getSubcategories(Long id){
        AdvertisementCategory parent = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(AdvertisementCategory.class, id));

        return categoryRepository.findByParent(parent)
                .stream()
                .map(advertisementCategoryMapper::toResponse)
                .toList();
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void createCategory(AdvertisementCategoryRequest advertisementCategoryRequest){
        AdvertisementCategory advertisementCategory = advertisementCategoryMapper.toEntity(advertisementCategoryRequest);
        categoryRepository.save(advertisementCategory);

        log.info(
                "AUDIT category created: categoryId={}, name={}, parentId={}",
                advertisementCategory.getId(),
                advertisementCategory.getName(),
                getParentId(advertisementCategory)
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
                "AUDIT category subcategories created: parentId={}, createdCount={}",
                parent.getId(),
                advertisementSubcategories.size()
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
                "AUDIT category updated: categoryId={}, previousName={}, newName={}",
                advertisementCategory.getId(),
                previousName,
                advertisementCategory.getName()
        );

    }

    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id){
        AdvertisementCategory advertisementCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(AdvertisementCategory.class, id));

        if(advertisementRepository.existsByCategory(advertisementCategory)){
            throw new OperationNotAllowedException("Cannot delete category with advertisements",
                    Map.of("Category id:", id));
        }

        if (!categoryRepository.findByParent(advertisementCategory).isEmpty()) { // .isEmpty > != null (null = [])
            throw new OperationNotAllowedException("Cannot delete category with subcategories",
                    Map.of("Category id:", id));
        }

        categoryRepository.delete(advertisementCategory);

        log.info(
                "AUDIT category deleted: categoryId={}, name={}, parentId={}",
                advertisementCategory.getId(),
                advertisementCategory.getName(),
                getParentId(advertisementCategory)
        );
    }

    private Long getParentId(AdvertisementCategory category) {
        return category.getParent() != null ? category.getParent().getId() : null;
    }
}
