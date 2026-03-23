package ru.catwarden.advweb.adcategory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.catwarden.advweb.ad.AdvertisementRepository;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryRequest;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryResponse;
import ru.catwarden.advweb.adcategory.dto.AdvertisementCategoryUpdateRequest;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.exception.InvalidRelationException;
import ru.catwarden.advweb.exception.OperationNotAllowedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AdvertisementRepository advertisementRepository;

    @Mock
    private AdvertisementCategoryMapper advertisementCategoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void getCategoryReturnsMappedResponse() {
        AdvertisementCategory category = AdvertisementCategory.builder()
                .id(1L)
                .name("Transport")
                .build();
        AdvertisementCategoryResponse response = AdvertisementCategoryResponse.builder()
                .id(1L)
                .name("Transport")
                .parentId(null)
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(advertisementCategoryMapper.toResponse(category)).thenReturn(response);

        AdvertisementCategoryResponse result = categoryService.getCategory(1L);

        assertEquals(response, result);
    }

    @Test
    void getCategoryThrowsWhenNotFound() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> categoryService.getCategory(999L));
    }

    @Test
    void getAllCategoriesReturnsMappedList() {
        AdvertisementCategory c1 = AdvertisementCategory.builder().id(1L).name("Transport").build();
        AdvertisementCategory c2 = AdvertisementCategory.builder().id(2L).name("Electronics").build();
        AdvertisementCategoryResponse r1 = AdvertisementCategoryResponse.builder().id(1L).name("Transport").build();
        AdvertisementCategoryResponse r2 = AdvertisementCategoryResponse.builder().id(2L).name("Electronics").build();

        when(categoryRepository.findByParentIsNull()).thenReturn(List.of(c1, c2));
        when(advertisementCategoryMapper.toResponse(c1)).thenReturn(r1);
        when(advertisementCategoryMapper.toResponse(c2)).thenReturn(r2);

        List<AdvertisementCategoryResponse> result = categoryService.getAllCategories();

        assertEquals(2, result.size());
        assertEquals("Transport", result.get(0).getName());
        assertEquals("Electronics", result.get(1).getName());
    }

    @Test
    void getSubcategoriesReturnsMappedList() {
        AdvertisementCategory parent = AdvertisementCategory.builder().id(1L).name("Transport").build();
        AdvertisementCategory sub1 = AdvertisementCategory.builder().id(3L).name("Cars").parent(parent).build();
        AdvertisementCategory sub2 = AdvertisementCategory.builder().id(4L).name("Bikes").parent(parent).build();
        AdvertisementCategoryResponse r1 = AdvertisementCategoryResponse.builder().id(3L).name("Cars").parentId(1L).build();
        AdvertisementCategoryResponse r2 = AdvertisementCategoryResponse.builder().id(4L).name("Bikes").parentId(1L).build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(categoryRepository.findByParent(parent)).thenReturn(List.of(sub1, sub2));
        when(advertisementCategoryMapper.toResponse(sub1)).thenReturn(r1);
        when(advertisementCategoryMapper.toResponse(sub2)).thenReturn(r2);

        List<AdvertisementCategoryResponse> result = categoryService.getSubcategories(1L);

        assertEquals(2, result.size());
        assertEquals("Cars", result.get(0).getName());
        assertEquals("Bikes", result.get(1).getName());
    }

    @Test
    void createCategorySavesMappedEntity() {
        AdvertisementCategoryRequest request = AdvertisementCategoryRequest.builder()
                .name("Books")
                .build();
        AdvertisementCategory entity = AdvertisementCategory.builder()
                .name("Books")
                .build();

        when(advertisementCategoryMapper.toEntity(request)).thenReturn(entity);

        categoryService.createCategory(request);

        verify(categoryRepository).save(entity);
    }

    @Test
    void createSubcategoriesCreatesChildrenForRootCategory() {
        AdvertisementCategory parent = AdvertisementCategory.builder()
                .id(1L)
                .name("Transport")
                .parent(null)
                .build();
        AdvertisementCategoryRequest req1 = AdvertisementCategoryRequest.builder().name("Cars").build();
        AdvertisementCategoryRequest req2 = AdvertisementCategoryRequest.builder().name("Bikes").build();
        AdvertisementCategory sub1 = AdvertisementCategory.builder().name("Cars").build();
        AdvertisementCategory sub2 = AdvertisementCategory.builder().name("Bikes").build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(advertisementCategoryMapper.toEntity(req1)).thenReturn(sub1);
        when(advertisementCategoryMapper.toEntity(req2)).thenReturn(sub2);

        categoryService.createSubcategories(1L, List.of(req1, req2));

        ArgumentCaptor<List<AdvertisementCategory>> captor = ArgumentCaptor.forClass(List.class);
        verify(categoryRepository).saveAll(captor.capture());

        List<AdvertisementCategory> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertEquals(parent, saved.get(0).getParent());
        assertEquals(parent, saved.get(1).getParent());
    }

    @Test
    void createSubcategoriesThrowsForSubcategoryParent() {
        AdvertisementCategory top = AdvertisementCategory.builder().id(1L).name("Transport").build();
        AdvertisementCategory subcategoryParent = AdvertisementCategory.builder()
                .id(2L)
                .name("Cars")
                .parent(top)
                .build();

        when(categoryRepository.findById(2L)).thenReturn(Optional.of(subcategoryParent));

        InvalidRelationException exception = assertThrows(InvalidRelationException.class, () ->
                categoryService.createSubcategories(2L, List.of(AdvertisementCategoryRequest.builder().name("Sedan").build())));
        assertEquals("Cannot create subcategory for a subcategory", exception.getMessage());
        assertEquals(Map.of("Parent id:", 2L), exception.getDetails());
        verify(categoryRepository, never()).saveAll(any());
    }

    @Test
    void updateCategoryChangesNameWhenDifferent() {
        AdvertisementCategory category = AdvertisementCategory.builder()
                .id(5L)
                .name("Old name")
                .build();
        AdvertisementCategoryUpdateRequest request = AdvertisementCategoryUpdateRequest.builder()
                .name("New name")
                .build();

        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

        categoryService.updateCategory(5L, request);

        assertEquals("New name", category.getName());
        verify(categoryRepository).save(category);
    }

    @Test
    void updateCategoryThrowsWhenNameUnchanged() {
        AdvertisementCategory category = AdvertisementCategory.builder()
                .id(5L)
                .name("Same")
                .build();
        AdvertisementCategoryUpdateRequest request = AdvertisementCategoryUpdateRequest.builder()
                .name("Same")
                .build();

        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));

        assertThrows(OperationNotAllowedException.class, () -> categoryService.updateCategory(5L, request));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteCategoryDeletesWhenNoAdsAndNoSubcategories() {
        AdvertisementCategory category = AdvertisementCategory.builder()
                .id(7L)
                .name("Books")
                .build();

        when(categoryRepository.findById(7L)).thenReturn(Optional.of(category));
        when(advertisementRepository.existsByCategory(category)).thenReturn(false);
        when(categoryRepository.findByParent(category)).thenReturn(List.of());

        categoryService.deleteCategory(7L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategoryThrowsWhenCategoryHasAdvertisements() {
        AdvertisementCategory category = AdvertisementCategory.builder()
                .id(8L)
                .name("Cars")
                .build();

        when(categoryRepository.findById(8L)).thenReturn(Optional.of(category));
        when(advertisementRepository.existsByCategory(category)).thenReturn(true);

        assertThrows(OperationNotAllowedException.class, () -> categoryService.deleteCategory(8L));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteCategoryThrowsWhenCategoryHasSubcategories() {
        AdvertisementCategory category = AdvertisementCategory.builder()
                .id(9L)
                .name("Transport")
                .build();
        AdvertisementCategory child = AdvertisementCategory.builder()
                .id(10L)
                .name("Cars")
                .parent(category)
                .build();

        when(categoryRepository.findById(9L)).thenReturn(Optional.of(category));
        when(advertisementRepository.existsByCategory(category)).thenReturn(false);
        when(categoryRepository.findByParent(category)).thenReturn(List.of(child));

        assertThrows(OperationNotAllowedException.class, () -> categoryService.deleteCategory(9L));
        verify(categoryRepository, never()).delete(any());
    }
}

