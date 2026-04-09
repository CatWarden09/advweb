package ru.catwarden.advweb.moderation.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.catwarden.advweb.ad.AdvertisementService;
import ru.catwarden.advweb.ad.dto.AdvertisementResponse;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvertisementModerationControllerTest {

    @Mock
    private AdvertisementService advertisementService;

    private AdvertisementModerationController controller;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @BeforeEach
    void setUp() {
        controller = new AdvertisementModerationController(advertisementService);
    }

    @Test
    void getAllPendingAdvertisementsPassesCorrectPageable() {
        Page<AdvertisementResponse> page = new PageImpl<>(List.of(AdvertisementResponse.builder().id(1L).build()));
        when(advertisementService.getAllPendingAdvertisements(any(Pageable.class))).thenReturn(page);

        Page<AdvertisementResponse> result = controller.getAllPendingAdvertisements(1, 5);

        assertEquals(1, result.getTotalElements());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(advertisementService).getAllPendingAdvertisements(captor.capture());
        assertEquals(1, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
    }

    @Test
    void getAllRejectedAdvertisementsPassesCorrectPageable() {
        Page<AdvertisementResponse> page = new PageImpl<>(List.of(AdvertisementResponse.builder().id(2L).build()));
        when(advertisementService.getAllRejectedAdvertisements(any(Pageable.class))).thenReturn(page);

        Page<AdvertisementResponse> result = controller.getAllRejectedAdvertisements(2, 7);

        assertEquals(1, result.getTotalElements());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(advertisementService).getAllRejectedAdvertisements(captor.capture());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(7, captor.getValue().getPageSize());
    }


    @Test
    void approveAdvertisementDelegatesToService() {
        controller.approveAdvertisement(3L);

        verify(advertisementService).approveAdvertisement(3L);
    }

    @Test
    void rejectAdvertisementDelegatesToService() {
        controller.rejectAdvertisement(4L, "Violation of rules");

        verify(advertisementService).rejectAdvertisement(4L, "Violation of rules");
    }
    

    @Test
    void rejectAdvertisementHasValidationForBlankReason() throws NoSuchMethodException {
        Method method = AdvertisementModerationController.class.getMethod(
                "rejectAdvertisement",
                Long.class,
                String.class
        );

        Set<ConstraintViolation<AdvertisementModerationController>> violations =
                validator.forExecutables().validateParameters(controller, method, new Object[]{1L, ""});

        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v ->
                v.getConstraintDescriptor().getAnnotation().annotationType().equals(NotBlank.class)));
    }

    @Test
    void rejectAdvertisementHasValidationForTooLongReason() throws NoSuchMethodException {
        Method method = AdvertisementModerationController.class.getMethod(
                "rejectAdvertisement",
                Long.class,
                String.class
        );
        String longReason = "a".repeat(256);

        Set<ConstraintViolation<AdvertisementModerationController>> violations =
                validator.forExecutables().validateParameters(controller, method, new Object[]{1L, longReason});

        assertEquals(1, violations.size());
        assertTrue(violations.stream().anyMatch(v ->
                v.getConstraintDescriptor().getAnnotation().annotationType().equals(Size.class)));
    }
}
