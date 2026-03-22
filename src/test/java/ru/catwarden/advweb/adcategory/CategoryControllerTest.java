package ru.catwarden.advweb.adcategory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryRequest;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryResponse;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryUpdateRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    private CategoryController categoryController;

    @BeforeEach
    void setUp() {
        categoryController = new CategoryController(categoryService);
    }

    @Test
    void getCategoryReturnsServiceResult() {
        AdvertisementCategoryResponse response = AdvertisementCategoryResponse.builder()
                .id(10L)
                .name("Transport")
                .parentId(null)
                .build();
        when(categoryService.getCategory(10L)).thenReturn(response);

        AdvertisementCategoryResponse result = categoryController.getCategory(10L);

        assertEquals(response, result);
    }

    @Test
    void getAllCategoriesReturnsServiceResult() {
        List<AdvertisementCategoryResponse> responseList = List.of(
                AdvertisementCategoryResponse.builder().id(1L).name("Transport").build(),
                AdvertisementCategoryResponse.builder().id(2L).name("Electronics").build()
        );
        when(categoryService.getAllCategories()).thenReturn(responseList);

        List<AdvertisementCategoryResponse> result = categoryController.getAllCategories();

        assertEquals(responseList, result);
    }

    @Test
    void getSubcategoriesReturnsServiceResult() {
        List<AdvertisementCategoryResponse> responseList = List.of(
                AdvertisementCategoryResponse.builder().id(3L).name("Cars").parentId(1L).build()
        );
        when(categoryService.getSubcategories(1L)).thenReturn(responseList);

        List<AdvertisementCategoryResponse> result = categoryController.getSubcategories(1L);

        assertEquals(responseList, result);
    }

    @Test
    void createCategoryDelegatesToService() {
        AdvertisementCategoryRequest request = AdvertisementCategoryRequest.builder()
                .name("Books")
                .build();

        categoryController.createCategory(request);

        verify(categoryService).createCategory(request);
    }

    @Test
    void createSubcategoriesDelegatesToService() {
        List<AdvertisementCategoryRequest> subcategories = List.of(
                AdvertisementCategoryRequest.builder().name("Sedans").build(),
                AdvertisementCategoryRequest.builder().name("SUV").build()
        );

        categoryController.createSubcategories(1L, subcategories);

        verify(categoryService).createSubcategories(1L, subcategories);
    }

    @Test
    void updateCategoryDelegatesToService() {
        AdvertisementCategoryUpdateRequest request = AdvertisementCategoryUpdateRequest.builder()
                .name("Updated name")
                .build();

        categoryController.updateCategory(5L, request);

        verify(categoryService).updateCategory(5L, request);
    }

    @Test
    void deleteCategoryDelegatesToService() {
        categoryController.deleteCategory(8L);

        verify(categoryService).deleteCategory(8L);
    }
}

