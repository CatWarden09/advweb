package ru.catwarden.advweb.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.catwarden.advweb.dto.request.AdvertisementCategoryRequest;
import ru.catwarden.advweb.dto.request.AdvertisementCategoryUpdateRequest;
import ru.catwarden.advweb.dto.response.AdvertisementCategoryResponse;
import ru.catwarden.advweb.service.CategoryService;

import java.util.List;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
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
    public void createCategory(@RequestBody AdvertisementCategoryRequest advertisementCategoryRequest){
        categoryService.createCategory(advertisementCategoryRequest);
    }

    @PostMapping("/{id}/subcategories")
    public void createSubcategories(@PathVariable Long id, @RequestBody List<AdvertisementCategoryRequest> subcategoryList){
        categoryService.createSubcategories(id, subcategoryList);
    }

    @PatchMapping("/{id}")
    public void updateCategory(@PathVariable Long id, @RequestBody AdvertisementCategoryUpdateRequest advertisementCategoryUpdateRequest){
        categoryService.updateCategory(id, advertisementCategoryUpdateRequest);
    }

    @DeleteMapping("/{id}")
    public void deleteCategory(@PathVariable Long id){
        categoryService.deleteCategory(id);
    }
}
