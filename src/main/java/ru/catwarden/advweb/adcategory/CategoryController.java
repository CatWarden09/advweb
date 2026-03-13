package ru.catwarden.advweb.adcategory;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryRequest;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryUpdateRequest;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryResponse;

import java.util.List;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Validated
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping("/{id}")
    public AdvertisementCategoryResponse getCategory(@PathVariable Long id){
        return categoryService.getCategory(id);
    }

    @GetMapping
    public List<AdvertisementCategoryResponse> getAllCategories(){
        return categoryService.getAllCategories();
    }

    @GetMapping("/{id}/subcategories")
    public List<AdvertisementCategoryResponse> getSubcategories(@PathVariable Long id){
        return categoryService.getSubcategories(id);
    }

    @PostMapping
    public void createCategory(@Valid @RequestBody AdvertisementCategoryRequest advertisementCategoryRequest){
        categoryService.createCategory(advertisementCategoryRequest);
    }

    @PostMapping("/{id}/subcategories")
    public void createSubcategories(@PathVariable Long id,
                                    @Valid @RequestBody List<AdvertisementCategoryRequest> subcategoryList){
        categoryService.createSubcategories(id, subcategoryList);
    }

    @PatchMapping("/{id}")
    public void updateCategory(@PathVariable Long id,
                               @Valid @RequestBody AdvertisementCategoryUpdateRequest advertisementCategoryUpdateRequest){
        categoryService.updateCategory(id, advertisementCategoryUpdateRequest);
    }

    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Long id){
        categoryService.deleteCategory(id);
    }
}
