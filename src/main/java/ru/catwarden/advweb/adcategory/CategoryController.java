package ru.catwarden.advweb.adcategory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.catwarden.advweb.ad.dto.AdvertisementResponse;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryRequest;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryUpdateRequest;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryResponse;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Validated
@Tag(name = "Категории", description = "Операции с категориями объявлений")
public class CategoryController {
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final CategoryService categoryService;

    @Operation(summary = "Получить категорию по id")
    @GetMapping("/{id}")
    public AdvertisementCategoryResponse getCategory(@Parameter(description = "ID категории") @PathVariable Long id){
        return categoryService.getCategory(id);
    }

    @Operation(summary = "Получить все категории")
    @GetMapping
    public List<AdvertisementCategoryResponse> getAllCategories(){
        return categoryService.getAllCategories();
    }

    @Operation(summary = "Получить подкатегории категории")
    @GetMapping("/{id}/subcategories")
    public List<AdvertisementCategoryResponse> getSubcategories(
            @Parameter(description = "ID родительской категории") @PathVariable Long id){
        return categoryService.getSubcategories(id);
    }

    @Operation(summary = "Получить объявления категории")
    @GetMapping("/{id}/ads")
    public Page<AdvertisementResponse> getCategoryAds(
            @Parameter(description = "Номер страницы (с 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "ID категории") @PathVariable Long id){
        Pageable pageable = buildPageable(page, size);

        return categoryService.getCategoryAds(pageable, id);
    }

    @Operation(summary = "Получить объявления подкатегории")
    @GetMapping("/{parent_id}/{sub_id}/ads")
    public Page<AdvertisementResponse> getSubcategoryAds(
            @Parameter(description = "Номер страницы (с 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "ID родительской категории") @PathVariable Long parent_id,
            @Parameter(description = "ID подкатегории") @PathVariable Long sub_id){

        Pageable pageable = buildPageable(page, size);

        return categoryService.getSubcategoryAds(pageable, parent_id, sub_id);
    }

    @Operation(summary = "Создать категорию")
    @PostMapping
    public void createCategory(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Тело запроса для создания категории")
            @Valid @RequestBody AdvertisementCategoryRequest advertisementCategoryRequest){
        categoryService.createCategory(advertisementCategoryRequest);
    }

    @Operation(summary = "Создать подкатегории")
    @PostMapping("/{id}/subcategories")
    public void createSubcategories(
            @Parameter(description = "ID родительской категории") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Список подкатегорий для создания")
            @RequestBody @NotEmpty List<@Valid AdvertisementCategoryRequest> subcategoryList){
        categoryService.createSubcategories(id, subcategoryList);
    }

    @Operation(summary = "Обновить категорию")
    @PutMapping("/{id}")
    public void updateCategory(
            @Parameter(description = "ID категории") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Тело запроса для обновления категории")
            @Valid @RequestBody AdvertisementCategoryUpdateRequest advertisementCategoryUpdateRequest){
        categoryService.updateCategory(id, advertisementCategoryUpdateRequest);
    }

    @Operation(summary = "Удалить категорию")
    @DeleteMapping("/{id}")
    public void deleteCategory(@Parameter(description = "ID категории") @PathVariable Long id){
        categoryService.deleteCategory(id);
    }

    private Pageable buildPageable(int page, int size) {
        return PageRequest.of(page, size, DEFAULT_SORT);
    }
}


