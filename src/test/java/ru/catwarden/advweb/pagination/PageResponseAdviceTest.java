package ru.catwarden.advweb.pagination;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.catwarden.advweb.pagination.dto.PageResponse;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageResponseAdviceTest {

    private final PageResponseAdvice advice = new PageResponseAdvice();

    @Test
    void supportsPageReturnType() throws NoSuchMethodException {
        Method method = TestController.class.getMethod("pageEndpoint");
        MethodParameter methodParameter = new MethodParameter(method, -1);

        assertTrue(advice.supports(methodParameter, null));
    }

    @Test
    void convertsPageToStableResponseWithTotalPages() {
        Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 5);

        Object result = advice.beforeBodyWrite(page, null, null, null, null, null);

        assertTrue(result instanceof PageResponse<?>);
        PageResponse<?> response = (PageResponse<?>) result;
        assertEquals(List.of("a", "b"), response.getContent());
        assertEquals(1, response.getNumber());
        assertEquals(2, response.getSize());
        assertEquals(5, response.getTotalElements());
        assertEquals(3, response.getTotalPages());
        assertEquals(2, response.getNumberOfElements());
        assertFalse(response.isFirst());
        assertFalse(response.isLast());
        assertFalse(response.isEmpty());
    }

    static class TestController {
        public Page<String> pageEndpoint() {
            return Page.empty();
        }
    }
}
