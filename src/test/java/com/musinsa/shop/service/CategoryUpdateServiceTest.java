package com.musinsa.shop.service;

import com.musinsa.shop.common.exception.DuplicateResourceException;
import com.musinsa.shop.common.exception.InvalidRequestException;
import com.musinsa.shop.common.exception.ResourceNotFoundException;
import com.musinsa.shop.domain.category.dto.CategoryResponse;
import com.musinsa.shop.domain.category.dto.CategoryUpdateRequest;
import com.musinsa.shop.domain.category.entity.Category;
import com.musinsa.shop.domain.category.repository.CategoryRepository;
import com.musinsa.shop.domain.category.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("카테고리 수정 기능")
class CategoryUpdateServiceTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCases {
        @Test
        @DisplayName("부모 카테고리 없는 카테고리 수정")
        void test_success_update_without_parent() {
            // given
            Long categoryId = 1L;

            // 기존 카테고리
            Category originCategory = Category.create("상의", 1, "/category/top", true, null);
            ReflectionTestUtils.setField(originCategory, "id", categoryId);
            originCategory.updatePath();

            // 수정 요청: 부모 카테고리 변경 없음
            CategoryUpdateRequest updateRequest = new CategoryUpdateRequest("상의-NEW", null, 2, "/category/top-new", true);

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(originCategory));

            // when
            CategoryResponse response = categoryService.updateCategory(categoryId, updateRequest);

            // then
            assertNotNull(response);
            assertEquals("상의-NEW", response.getTitle());
            assertNull(originCategory.getParent());
            assertEquals(2, originCategory.getDisplayOrder());
            assertEquals("/category/top-new", originCategory.getLink());
            assertEquals("/1/", originCategory.getPath());

            // 부모 카테고리 변경 없는 로직 검증
            verify(categoryRepository, never()).bulkUpdatePath(any(), any(), any());
        }

        @Test
        @DisplayName("부모 카테고리 있는 상태에서 다른 부모 카테고리로 변경하며 카테고리 수정")
        void test_success_update_category_with_parent() {
            // given
            Long categoryId = 2L;
            Long oldParentId = 1L;
            Long newParentId = 3L;

            // 기존 부모 카테고리
            Category oldParent = Category.create("상의", 0, "/category/top", true, null);
            ReflectionTestUtils.setField(oldParent, "id", oldParentId);
            oldParent.updatePath();

            // 새 부모 카테고리
            Category newParent = Category.create("신상", 1, "/category/new", true, null);
            ReflectionTestUtils.setField(newParent, "id", newParentId);
            newParent.updatePath();

            // 기존 카테고리
            Category originCategory = Category.create("반팔티", 1, "/category/tshirt", true, oldParent);
            ReflectionTestUtils.setField(originCategory, "id", categoryId);
            originCategory.updatePath();

            CategoryUpdateRequest updateRequest = new CategoryUpdateRequest("반팔티-NEW", newParentId, 5, "/category/tshirt-new", true);

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(originCategory));
            when(categoryRepository.findById(newParentId)).thenReturn(Optional.of(newParent));

            // when
            CategoryResponse response = categoryService.updateCategory(categoryId, updateRequest);

            // then
            assertNotNull(response);
            assertEquals(updateRequest.getTitle(), response.getTitle());
            assertEquals(updateRequest.getDisplayOrder(), originCategory.getDisplayOrder());
            assertEquals(updateRequest.getLink(), originCategory.getLink());

            assertEquals(newParent, originCategory.getParent());
            assertEquals("/3/2/", originCategory.getPath());

            verify(categoryRepository).bulkUpdatePath(categoryId, "/1/2/", "/3/2/");
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailCases {
        @Test
        @DisplayName("수정 대상 카테고리 데이터 없는 경우 예외 발생")
        void test_update_fail_when_category_not_found() {
            // given
            Long invalidId = 999L;
            CategoryUpdateRequest request = new CategoryUpdateRequest("상의", null, 1, "/top", true);

            when(categoryRepository.findById(invalidId)).thenReturn(Optional.empty());

            // when & then
            assertThrows(ResourceNotFoundException.class, () ->
                    categoryService.updateCategory(invalidId, request)
            );
        }

        @Test
        @DisplayName("존재하지 않는 부모 카테고리로 수정하고자 하는 경우 예외 발생")
        void test_update_fail_when_parent_not_found() {
            // given
            Long categoryId = 1L;
            Long invalidParentId = 999L;

            Category category = Category.create("상의", 1, "/category/top", true, null);
            ReflectionTestUtils.setField(category, "id", categoryId);
            category.updatePath();

            CategoryUpdateRequest request = new CategoryUpdateRequest("상의", invalidParentId, 1, "/category/top", true);

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
            when(categoryRepository.findById(invalidParentId)).thenReturn(Optional.empty());

            // when & then
            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                    categoryService.updateCategory(categoryId, request)
            );

            assertEquals("부모 카테고리가 존재하지 않습니다.", exception.getMessage());

            // 벌크 쿼리 실행 안됨 체크
            verify(categoryRepository, never()).bulkUpdatePath(any(), any(), any());
        }

        @Test
        @DisplayName("자기 자신을 부모로 지정하면 예외 발생")
        void test_update_fail_when_parent_is_self() {
            // given
            Long categoryId = 1L;

            Category category = Category.create("상의", 1, "/category/top", true, null);
            ReflectionTestUtils.setField(category, "id", categoryId);
            category.updatePath();

            CategoryUpdateRequest request = new CategoryUpdateRequest("상의", categoryId, 1, "/category/top", true);

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    categoryService.updateCategory(categoryId, request)
            );

            assertEquals("자기 자신을 부모로 지정할 수 없습니다.", exception.getMessage());

            // 벌크 쿼리 실행 안됨 체크
            verify(categoryRepository, never()).bulkUpdatePath(any(), any(), any());
        }

        @Test
        @DisplayName("하위 카테고리를 부모로 지정하는 경우 순환참조로 예외 발생")
        void test_update_fail_when_circular_reference() {
            // given
            Long parentId = 1L;
            Long childId = 2L;

            Category parent = Category.create("상의", 1, "/category/top", true, null);
            ReflectionTestUtils.setField(parent, "id", parentId);

            Category child = Category.create("셔츠", 2, "/category/top/shirt", true, parent);
            ReflectionTestUtils.setField(child, "id", childId);

            parent.addChildCategory(child);
            parent.updatePath();
            child.updatePath();

            // 부모 카테고리를 자식으로 바꾸는 잘못된 요청
            CategoryUpdateRequest request = new CategoryUpdateRequest("상의", childId, 1, "/category/top", true);

            when(categoryRepository.findById(parentId)).thenReturn(Optional.of(parent));
            when(categoryRepository.findById(childId)).thenReturn(Optional.of(child));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    categoryService.updateCategory(parentId, request)
            );

            assertEquals("하위 카테고리를 부모로 지정할 수 없습니다.", exception.getMessage());

            // 벌크 쿼리 실행 안됨 체크
            verify(categoryRepository, never()).bulkUpdatePath(any(), any(), any());
        }

        @Test
        @DisplayName("변경 부모 카테고리에 동일한 이름이 있는 경우 예외 발생")
        void test_update_fail_when_title_duplicate_in_new_parent() {
            // given
            Long categoryId = 2L;
            Long newParentId = 3L;
            String duplicatedTitle = "반팔티";

            Category originCategory = Category.create(duplicatedTitle, 1, "/category/tshirt", true, null);
            ReflectionTestUtils.setField(originCategory, "id", categoryId);
            originCategory.updatePath();

            Category newParent = Category.create("신상", 0, "/category/new", true, null);
            ReflectionTestUtils.setField(newParent, "id", newParentId);
            newParent.updatePath();

            // 수정 요청: 부모를 바꾸면서 동일한 title 유지
            CategoryUpdateRequest updateRequest = new CategoryUpdateRequest(duplicatedTitle, newParentId, 5, "/category/tshirt-new", true);

            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(originCategory));
            when(categoryRepository.findById(newParentId)).thenReturn(Optional.of(newParent));
            // 중복 title 존재함
            when(categoryRepository.existsByParentIdAndTitleAndIdNot(newParentId, duplicatedTitle, categoryId)).thenReturn(true);

            // when & then
            DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () ->
                    categoryService.updateCategory(categoryId, updateRequest));

                    assertEquals("동일 상위 카테고리 내 이미 존재하는 카테고리명입니다.", exception.getMessage());
        }
    }
}
