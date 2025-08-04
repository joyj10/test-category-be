package com.musinsa.shop.service;

import com.musinsa.shop.common.exception.InvalidRequestException;
import com.musinsa.shop.common.exception.ResourceNotFoundException;
import com.musinsa.shop.domain.category.entity.Category;
import com.musinsa.shop.domain.category.repository.CategoryRepository;
import com.musinsa.shop.domain.category.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@DisplayName("카테고리 삭제 기능")
public class CategoryDeleteServiceTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCases {

        @Test
        @DisplayName("하위 카테고리가 없으면 삭제 처리")
        void test_success_delete_category() {
            // given
            Long categoryId = 20L;
            Category parent = Category.create("상의", 1, "/top", true, null);
            Category category = Category.create("니트", 1, "/top/knit", true, parent);

            ReflectionTestUtils.setField(category, "id", categoryId);

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(categoryRepository.existsByParentIdAndDeletedFalse(categoryId)).thenReturn(false);

            // when
            categoryService.deleteCategory(categoryId);

            // then
            assertTrue(category.getDeleted());
            assertNotNull(category.getDeletedAt());
            assertFalse(parent.getChildren().contains(category));
        }

    }

    @Nested
    @DisplayName("실패 케이스")
    class FailCases {

        @Test
        @DisplayName("삭제 요청 카테고리 존재하지 않으면 예외 발생")
        void test_fail_delete_category_not_found() {
            // given
            Long invalidId = 999L;

            when(categoryRepository.findById(invalidId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(ResourceNotFoundException.class, () ->
                categoryService.deleteCategory(invalidId)
            );
        }

        @Test
        @DisplayName("하위 카테고리가 있으면 삭제 예외 발생")
        void test_fail_delete_category_when_has_children() {
            // given
            Long categoryId = 20L;
            Category parent = Category.create("상의", 1, "/top", true, null);
            Category category = Category.create("니트", 1, "/top/knit", true, parent);

            ReflectionTestUtils.setField(category, "id", categoryId);

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(categoryRepository.existsByParentIdAndDeletedFalse(categoryId)).thenReturn(true);

            // when & then
            assertThrows(InvalidRequestException.class, () ->
                categoryService.deleteCategory(categoryId)
            );
        }

    }
}
