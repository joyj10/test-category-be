package com.musinsa.shop.service;

import com.musinsa.shop.common.exception.ResourceNotFoundException;
import com.musinsa.shop.domain.category.dto.CategoryDto;
import com.musinsa.shop.domain.category.dto.CategoryTreeResponse;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@DisplayName("카테고리 조회 기능")
public class CategorySelectServiceTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCases {
        @Test
        @DisplayName("전체 트리 조회")
        void test_success_get_tree_all() {
            // given
            List<CategoryDto> flatList = List.of(
                    new CategoryDto(1L, "패션", null, "/1/", "/category/fashion", 1, true, null, null, null),
                    new CategoryDto(2L, "남성의류", 1L, "/1/2/", "/category/men", 1, true, null, null, null),
                    new CategoryDto(3L, "셔츠", 2L, "/1/2/3/", "/category/shirt", 1, true, null, null, null)
            );

            when(categoryRepository.findAllTree()).thenReturn(flatList);

            // when
            List<CategoryTreeResponse> result = categoryService.getCategories(null);

            // then
            assertEquals(1, result.size());

            CategoryTreeResponse root = result.get(0);
            assertEquals("패션", root.getTitle());
            assertEquals(1, root.getChildren().size());

            CategoryTreeResponse men = root.getChildren().get(0);
            assertEquals("남성의류", men.getTitle());
            assertEquals(1, men.getChildren().size());
            assertEquals("셔츠", men.getChildren().get(0).getTitle());
        }

        @Test
        @DisplayName("특정 카테고리 트리 조회")
        void test_success_get_tree_by_parent_id() {
            // given
            Long parentId = 1L;
            String parentPath = "/1/";
            Category parent = Category.create("상의", 1, "/category/top", true, null);
            ReflectionTestUtils.setField(parent, "id", parentId);
            parent.updatePath();

            List<CategoryDto> flatList = List.of(
                    new CategoryDto(1L, "상의", null, "/1/", "/category/top", 1, true, null, null, null), // ✅ 루트 추가
                    new CategoryDto(2L, "셔츠", 1L, "/1/2/", "/category/shirt", 1, true, null, null, null),
                    new CategoryDto(3L, "맨투맨", 1L, "/1/3/", "/category/hoodie", 2, true, null, null, null)
            );

            when(categoryRepository.findById(parentId)).thenReturn(Optional.of(parent));
            when(categoryRepository.findTreeByPath(parentPath)).thenReturn(flatList);

            // when
            List<CategoryTreeResponse> result = categoryService.getCategories(parentId);

            // then
            assertEquals(1, result.size());

            CategoryTreeResponse root = result.get(0);
            assertEquals("상의", root.getTitle());
            assertEquals(2, root.getChildren().size());
            assertTrue(root.getChildren().stream().anyMatch(c -> c.getTitle().equals("셔츠")));
            assertTrue(root.getChildren().stream().anyMatch(c -> c.getTitle().equals("맨투맨")));
        }
    }

    @Nested
    @DisplayName("실패 케이스")
    class FailCases {
        @Test
        @DisplayName("조회하고자 하는 특정 카테고리 존재하지 않는 경우 예외 발생")
        void test_fail_get_tree_when_parent_not_found() {
            // given
            Long invalidParentId = 999L;

            when(categoryRepository.findById(invalidParentId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThrows(ResourceNotFoundException.class, () -> {
                categoryService.getCategories(invalidParentId);
            });
        }
    }
}
