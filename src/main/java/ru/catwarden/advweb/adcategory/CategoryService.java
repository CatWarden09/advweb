package ru.catwarden.advweb.adcategory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.catwarden.advweb.ad.Advertisement;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryRequest;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryUpdateRequest;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryResponse;
import ru.catwarden.advweb.ad.AdvertisementRepository;
import ru.catwarden.advweb.exception.EntityNotFoundException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final AdvertisementRepository advertisementRepository;
    private final AdvertisementCategoryMapper advertisementCategoryMapper;

    public AdvertisementCategoryResponse getCategory(Long id){
        AdvertisementCategory advertisementCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(AdvertisementCategory.class, id));

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
                .orElseThrow(() -> new EntityNotFoundException(AdvertisementCategory.class, id));

        return categoryRepository.findByParent(parent)
                .stream()
                .map(advertisementCategoryMapper::toResponse)
                .toList();
    }

    public void createCategory(AdvertisementCategoryRequest advertisementCategoryRequest){
        AdvertisementCategory advertisementCategory = advertisementCategoryMapper.toEntity(advertisementCategoryRequest);
        categoryRepository.save(advertisementCategory);
    }

    // FIXED - forbid to create more than 2 levels of hierarchy
    //  if the front passes the subcategory id, then we create a deeper hierarchy than 2 levels intended
    //  need to figure out a solution for deleting such deep tree (like recursively checking the subcategories and forbidding to delete those)
    public void createSubcategories(Long id, List<AdvertisementCategoryRequest> subcategoryList){
        AdvertisementCategory parent = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(AdvertisementCategory.class, id));

        if (parent.getParent() != null){
            throw new RuntimeException("Cannot create subcategory for a subcategory");
        }

        // DONE add batch updating
        List<AdvertisementCategory> advertisementSubcategories = new ArrayList<>();
        for(AdvertisementCategoryRequest advertisementCategoryRequest : subcategoryList){
            AdvertisementCategory advertisementSubcategory = advertisementCategoryMapper.toEntity(advertisementCategoryRequest);
            advertisementSubcategory.setParent(parent);

            advertisementSubcategories.add(advertisementSubcategory);
        }
        categoryRepository.saveAll(advertisementSubcategories);
    }

    public void updateCategory(Long id, AdvertisementCategoryUpdateRequest advertisementCategoryUpdateRequest){
        AdvertisementCategory advertisementCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(AdvertisementCategory.class, id));

        if(!advertisementCategory.getName().equals(advertisementCategoryUpdateRequest.getName())) {
            advertisementCategory.setName(advertisementCategoryUpdateRequest.getName());
        }
        categoryRepository.save(advertisementCategory);

    }

    public void deleteCategory(Long id){
        AdvertisementCategory advertisementCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(AdvertisementCategory.class, id));

        if(advertisementRepository.existsByCategory(advertisementCategory)){
            throw new RuntimeException("Cannot delete category with advertisements");
        }

        if (!categoryRepository.findByParent(advertisementCategory).isEmpty()) { // .isEmpty > != null (null = [])
            throw new RuntimeException("Cannot delete category with subcategories");
        }

        categoryRepository.delete(advertisementCategory);
    }
}
