package ru.catwarden.advweb.ad;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.catwarden.advweb.ad.dto.AdvertisementRequest;
import ru.catwarden.advweb.ad.dto.AdvertisementResponse;
import ru.catwarden.advweb.ad.dto.AdvertisementSearchFilter;
import ru.catwarden.advweb.ad.dto.AdvertisementUpdateRequest;
import ru.catwarden.advweb.comment.CommentService;
import ru.catwarden.advweb.comment.dto.CommentResponse;



@RestController
@RequestMapping("/advertisements")
@RequiredArgsConstructor
@Validated
@Tag(name = "Объявления", description = "Операции с объявлениями")
public class AdvertisementController {
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final AdvertisementService advertisementService;
    private final CommentService commentService;

    @Operation(summary = "Получить объявление по id")
    @GetMapping("/{id}")
    public AdvertisementResponse getAdvertisement(@Parameter(description = "ID объявления") @PathVariable Long id){
        AdvertisementResponse response = advertisementService.getAdvertisement(id);

        return response;
    }

    @Operation(summary = "Получить список опубликованных объявлений")
    @GetMapping
    public Page<AdvertisementResponse> getAllApprovedAdvertisements(
            @Parameter(description = "Номер страницы (с 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size){
        Pageable pageable = buildPageable(page, size);
        return advertisementService.getAllApprovedAdvertisements(pageable);

    }


    @Operation(summary = "Получить комментарии к объявлению")
    @GetMapping("/{id}/comments")
    public Page<CommentResponse> getAdvertisementComments(
            @Parameter(description = "ID объявления") @PathVariable Long id,
            @Parameter(description = "Номер страницы (с 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size){
        Pageable pageable = buildPageable(page, size);
        return commentService.getAdvertisementModeratedComments(id, pageable);
    }

    @Operation(summary = "Поиск объявлений по фильтру")
    @PostMapping("/search")
    public Page<AdvertisementResponse> getAdvertisementsByFilter(
            @Parameter(description = "Номер страницы (с 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Фильтр поиска объявлений")
            @Valid @RequestBody AdvertisementSearchFilter filter){
        Pageable pageable = buildPageable(page, size);
        return advertisementService.getAdvertisementsByFilter(pageable, filter);
    }

    @Operation(summary = "Создать объявление")
    @PostMapping
    public Long createAdvertisement(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Тело запроса для создания объявления")
            @Valid @RequestBody AdvertisementRequest advertisementRequest){
        return advertisementService.createAdvertisement(advertisementRequest);
    }

    @Operation(summary = "Обновить объявление")
    @PutMapping("/{id}/update")
    public void updateAdvertisement(
            @Parameter(description = "ID объявления") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Тело запроса для обновления объявления")
            @Valid @RequestBody AdvertisementUpdateRequest advertisementUpdateRequest){
        advertisementService.updateAdvertisement(id, advertisementUpdateRequest);
    }

    @Operation(summary = "Завершить объявление")
    @PutMapping("/{id}/finish")
    public void finishAdvertisement(@Parameter(description = "ID объявления") @PathVariable Long id){
        advertisementService.finishAdvertisement(id);
    }

    @Operation(summary = "Удалить объявление")
    @DeleteMapping("/{id}")
    public void deleteAdvertisement(@Parameter(description = "ID объявления") @PathVariable Long id){
        advertisementService.deleteAdvertisement(id);
    }

    private Pageable buildPageable(int page, int size) {
        return PageRequest.of(page, size, DEFAULT_SORT);
    }

}


