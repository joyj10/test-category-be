package com.musinsa.shop.category.service;

import com.musinsa.shop.common.exception.DuplicateResourceException;
import com.musinsa.shop.common.exception.InvalidRequestException;
import com.musinsa.shop.common.exception.ResourceNotFoundException;
import com.musinsa.shop.domain.category.dto.CategoryRequest;
import com.musinsa.shop.domain.category.dto.CategoryResponse;
import com.musinsa.shop.domain.category.entity.Category;
import com.musinsa.shop.domain.category.repository.CategoryRepository;
import com.musinsa.shop.domain.category.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("단위 테스트: 카테고리 등록")
class CategoryCreateServiceTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCases {
        @Test
        @DisplayName("루트 카테고리 등록")
        void test_success_create_root_category() {
            // given
            CategoryRequest categoryRequest = new CategoryRequest("상의", null, 1, "/category/top", true);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0, Category.class));

            // when
            CategoryResponse categoryResponse = categoryService.createCategory(categoryRequest);

            // then
            verify(categoryRepository).save(captor.capture());
            Category saved = captor.getValue();

            assertEquals(categoryRequest.getTitle(), saved.getTitle());
            assertEquals(categoryRequest.getTitle(), categoryResponse.getTitle());
            assertNull(saved.getParent());
        }

        @DisplayName("상위 카테고리 있는 카테고리 등록")
        @Test
        void test_success_create_category_with_parent() {
            // given
            CategoryRequest categoryRequest = new CategoryRequest("셔츠", 1L, 1, "/category/top/shirt", true);

            // 상위 카테고리 mocking
            Category parentCategory = Category.create("상의", 1, "/category/top", true, null);
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategory));

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0, Category.class));

            // when
            CategoryResponse categoryResponse = categoryService.createCategory(categoryRequest);

            // then
            verify(categoryRepository).save(captor.capture());
            Category saved = captor.getValue();

            assertNotNull(categoryResponse);
            assertEquals(categoryRequest.getTitle(), saved.getTitle());
            assertEquals(categoryRequest.getTitle(), categoryResponse.getTitle());
            assertEquals(parentCategory.getTitle(), saved.getParent().getTitle());
            assertEquals(parentCategory, saved.getParent());
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailCases {
        @Test
        @DisplayName("부모 카테고리가 존재하지 않는 경우 예외 발생")
        void test_fail_create_category_when_parent_not_found() {
            // given
            Long invalidParentId = 10L;
            CategoryRequest categoryRequest = new CategoryRequest("상의", invalidParentId, 1, "/category/top", true);

            when(categoryRepository.findById(invalidParentId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(ResourceNotFoundException.class, () -> {
                categoryService.createCategory(categoryRequest);
            });

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("이름이 null이면 예외 발생")
        void test_fail_create_category_when_title_is_null() {
            // given
            CategoryRequest categoryRequest = new CategoryRequest(null, null, 1, "/category/top", true);

            // when & then
            assertThrows(InvalidRequestException.class, () -> {
                categoryService.createCategory(categoryRequest);
            });

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("같은 상위 카테고리 하위에 이름 중복 시 예외 발생")
        void test_fail_create_category_when_title_duplicate() {
            // given
            String title = "상의";
            Long parentId = null;
            CategoryRequest categoryRequest = new CategoryRequest(title, parentId, 1, "/category/top", true);

            // mocking: 같은 title 존재하는 경우
            when(categoryRepository.existsByParentIdAndTitle(parentId, title)).thenReturn(true);


            // when & then
            assertThrows(DuplicateResourceException.class, () -> {
                categoryService.createCategory(categoryRequest);
            });

            verify(categoryRepository, never()).save(any());
        }
    }
}
