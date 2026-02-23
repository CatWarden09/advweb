package ru.catwarden.advweb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.catwarden.advweb.dto.request.AdvertisementCategoryRequest;
import ru.catwarden.advweb.dto.request.AdvertisementCategoryUpdateRequest;
import ru.catwarden.advweb.dto.response.AdvertisementCategoryResponse;
import ru.catwarden.advweb.entity.AdvertisementCategory;
import ru.catwarden.advweb.mapper.AdvertisementCategoryMapper;
import ru.catwarden.advweb.repository.AdvertisementRepository;
import ru.catwarden.advweb.repository.CategoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final AdvertisementRepository advertisementRepository;
    private final AdvertisementCategoryMapper advertisementCategoryMapper;

    public AdvertisementCategoryResponse getCategory(Long id){
        AdvertisementCategory advertisementCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        return advertisementCategoryMapper.toResponse(advertisementCategory);
    }

    public List<AdvertisementCategoryResponse> getAllCategories(){
        return categoryRepository.findByParentIsNull()
                .stream()
                .map(advertisementCategoryMapper::toResponse)
                .toList();
    }

    public List<AdvertisementCategoryResponse> getSubcategories(Long id){
        AdvertisementCategory parent = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        return categoryRepository.findByParent(parent)
                .stream()
                .map(advertisementCategoryMapper::toResponse)
                .toList();
    }

    public void createCategory(AdvertisementCategoryRequest advertisementCategoryRequest){
        AdvertisementCategory advertisementCategory = advertisementCategoryMapper.toEntity(advertisementCategoryRequest, null);
        categoryRepository.save(advertisementCategory);
    }

    public void createSubcategories(Long id, List<AdvertisementCategoryRequest> subcategoryList){
        AdvertisementCategory parent = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        for(AdvertisementCategoryRequest advertisementCategoryRequest : subcategoryList){
            AdvertisementCategory advertisementSubcategory = advertisementCategoryMapper.toEntity(advertisementCategoryRequest, parent);

            categoryRepository.save(advertisementSubcategory);
        }
    }

    public void updateCategory(Long id, AdvertisementCategoryUpdateRequest advertisementCategoryUpdateRequest){
        AdvertisementCategory advertisementCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        advertisementCategory.setName(advertisementCategoryUpdateRequest.getName());

        categoryRepository.save(advertisementCategory);

    }

    public void deleteCategory(Long id){
        AdvertisementCategory advertisementCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        if(advertisementRepository.existsByCategory(advertisementCategory)){
            throw new RuntimeException("Cannot delete category with advertisements");

        }

        if (!categoryRepository.findByParent(advertisementCategory).isEmpty()) { // .isEmpty > != null (null = [])
            throw new RuntimeException("Cannot delete category with subcategories");
        }

        categoryRepository.delete(advertisementCategory);
    }
}
