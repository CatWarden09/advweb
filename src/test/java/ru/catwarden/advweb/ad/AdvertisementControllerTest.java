package ru.catwarden.advweb.ad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.catwarden.advweb.ad.dto.AdvertisementRequest;
import ru.catwarden.advweb.ad.dto.AdvertisementResponse;
import ru.catwarden.advweb.ad.dto.AdvertisementSearchFilter;
import ru.catwarden.advweb.ad.dto.AdvertisementUpdateRequest;
import ru.catwarden.advweb.comment.CommentService;
import ru.catwarden.advweb.comment.dto.CommentResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvertisementControllerTest {

    @Mock
    private AdvertisementService advertisementService;
    @Mock
    private CommentService commentService;

    private AdvertisementController controller;

    @BeforeEach
    void setUp() {
        controller = new AdvertisementController(advertisementService, commentService);
    }

    @Test
    void getAdvertisementReturnsServiceResult() {
        AdvertisementResponse response = AdvertisementResponse.builder().id(1L).build();
        when(advertisementService.getAdvertisement(1L)).thenReturn(response);

        AdvertisementResponse result = controller.getAdvertisement(1L);

        assertEquals(response, result);
        verify(advertisementService).incrementAdvertisementViewCount(1L);
    }

    @Test
    void getAllApprovedAdvertisementsPassesCorrectPageable() {
        Page<AdvertisementResponse> page = new PageImpl<>(List.of(AdvertisementResponse.builder().id(1L).build()));
        when(advertisementService.getAllApprovedAdvertisements(any(Pageable.class))).thenReturn(page);

        Page<AdvertisementResponse> result = controller.getAllApprovedAdvertisements(2, 5);

        assertEquals(1, result.getTotalElements());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(advertisementService).getAllApprovedAdvertisements(pageableCaptor.capture());
        assertEquals(2, pageableCaptor.getValue().getPageNumber());
        assertEquals(5, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void getAdvertisementCommentsPassesCorrectPageable() {
        Page<CommentResponse> page = new PageImpl<>(List.of(CommentResponse.builder().id(3L).build()));
        when(commentService.getAdvertisementModeratedComments(any(Long.class), any(Pageable.class))).thenReturn(page);

        Page<CommentResponse> result = controller.getAdvertisementComments(10L, 1, 20);

        assertEquals(1, result.getTotalElements());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(commentService).getAdvertisementModeratedComments(org.mockito.ArgumentMatchers.eq(10L), pageableCaptor.capture());
        assertEquals(1, pageableCaptor.getValue().getPageNumber());
        assertEquals(20, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void getAdvertisementsByFilterPassesCorrectArguments() {
        AdvertisementSearchFilter filter = AdvertisementSearchFilter.builder().name("phone").build();
        Page<AdvertisementResponse> page = new PageImpl<>(List.of());
        when(advertisementService.getAdvertisementsByFilter(any(Pageable.class), any(AdvertisementSearchFilter.class)))
                .thenReturn(page);

        Page<AdvertisementResponse> result = controller.getAdvertisementsByFilter(0, 10, filter);

        assertEquals(0, result.getTotalElements());
        verify(advertisementService).getAdvertisementsByFilter(any(Pageable.class), org.mockito.ArgumentMatchers.eq(filter));
    }

    @Test
    void createAdvertisementReturnsCreatedId() {
        AdvertisementRequest request = AdvertisementRequest.builder().build();
        when(advertisementService.createAdvertisement(request)).thenReturn(77L);

        Long result = controller.createAdvertisement(request);

        assertEquals(77L, result);
    }

    @Test
    void updateAdvertisementDelegatesToService() {
        AdvertisementUpdateRequest request = AdvertisementUpdateRequest.builder().build();

        controller.updateAdvertisement(8L, request);

        verify(advertisementService).updateAdvertisement(8L, request);
    }

    @Test
    void deleteAdvertisementDelegatesToService() {
        controller.deleteAdvertisement(9L);

        verify(advertisementService).deleteAdvertisement(9L);
    }
}

