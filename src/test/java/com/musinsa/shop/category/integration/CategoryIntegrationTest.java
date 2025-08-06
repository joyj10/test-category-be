package com.musinsa.shop.category.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musinsa.shop.common.response.ResultResponse;
import com.musinsa.shop.domain.category.dto.CategoryRequest;
import com.musinsa.shop.domain.category.dto.CategoryResponse;
import com.musinsa.shop.domain.category.dto.CategoryUpdateRequest;
import com.musinsa.shop.domain.category.entity.Category;
import com.musinsa.shop.domain.category.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@DisplayName("통합 테스트: 카테고리")
class CategoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // 공통 메서드
    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    private CategoryResponse createCategory(String title, Long parentId) throws Exception {
        CategoryRequest request = new CategoryRequest(title, parentId, 1, "/dummy", true);
        String response = mockMvc.perform(post("/categories")
                        .contentType(APPLICATION_JSON)
                        .content(toJson(request)))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(response, new TypeReference<ResultResponse<CategoryResponse>>() {}).getData();
    }

    @Nested
    @DisplayName("카테고리 등록")
    class CreateCategoryTest {
        private Long rootId;

        @BeforeEach
        void setUp() throws Exception {
            rootId = createCategory("상의", null).getId();
        }

        @Test
        @DisplayName("성공: 루트 카테고리 등록")
        void test_success_create() throws Exception {
            // given
            CategoryRequest request = new CategoryRequest("남성의류", null, 1, "/category/men", true);

            // when & then
            mockMvc.perform(post("/categories")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.title").value(request.getTitle()));
        }

        @Test
        @DisplayName("실패: 부모 카테고리 없는 하위 카테고리 등록 시 404 예외")
        void test_fail_create_without_parent() throws Exception {
            CategoryRequest request = new CategoryRequest("티셔츠", 9999L, 1, "/category/tshirt", true);
            mockMvc.perform(post("/categories")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패: 동일 부모 카테고리 내 이름 중복 예외")
        void registerDuplicateTitle() throws Exception {
            // given
            CategoryRequest request = new CategoryRequest("중복", rootId, 1, "/category/dup", true);

            mockMvc.perform(post("/categories")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // when & then
            // 중복 등록
            mockMvc.perform(post("/categories")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("실패: 이름 빈값인 경우 예외")
        void test_fail_create_when_title_is_null() throws Exception {
            // given
            CategoryRequest request = new CategoryRequest(" ", null, 1, "/category/new", true);

            // when & then
            mockMvc.perform(post("/categories")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

    }


    @Nested
    @DisplayName("카테고리 수정")
    class UpdateCategory {
        private Long rootId;
        private Long childId;

        @BeforeEach
        void setUp() throws Exception {
            rootId = createCategory("상의", null).getId();
            childId = createCategory("반팔티", rootId).getId();
        }

        @Test
        @DisplayName("성공: 부모 없는 카테고리 필드 수정")
        void test_success_update_root_category_fields() throws Exception {
            // given
            Long categoryId = createCategory("신규", null).getId();

            CategoryUpdateRequest request = new CategoryUpdateRequest("신규수정", null, 3, "/updated", true);

            // when
            mockMvc.perform(patch("/categories/{id}", categoryId)
                            .contentType(APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(categoryId))
                    .andExpect(jsonPath("$.data.title").value(request.getTitle()));

            // then - 업데이트 데이터 확인
            Category updated = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new AssertionError("업데이트된 카테고리를 찾을 수 없습니다."));

            assertEquals(request.getTitle(), updated.getTitle());
            assertEquals(request.getDisplayOrder(), updated.getDisplayOrder());
            assertEquals(request.getLink(), updated.getLink());
            assertNull(updated.getParent());

        }

        @Test
        @DisplayName("성공: 부모 변경 시 본인 및 하위 카테고리 path 일괄 변경")
        void test_success_update_parent_change() throws Exception {
            // given
            Long oldParentId = createCategory("여성의류", null).getId();    //  /3/
            Long categoryId = createCategory("상의", oldParentId).getId();         //  /3/4/
            Long targetChildId = createCategory("반팔티", categoryId).getId();            //  /3/4/5

            Long newParentId = createCategory("신상", null).getId();       //  /6/

            CategoryUpdateRequest updateRequest = new CategoryUpdateRequest("상의", newParentId, 1, "/category/top", true);

            // when
            mockMvc.perform(patch("/categories/{id}", categoryId)
                            .contentType(APPLICATION_JSON)
                            .content(toJson(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(categoryId))
                    .andExpect(jsonPath("$.data.title").value(updateRequest.getTitle()));

            // then - 본인 path 변경 확인
            Category updatedParent = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new AssertionError("부모 카테고리 조회 실패"));

            String expectedNewParentPath = categoryRepository.findById(newParentId)
                    .orElseThrow().getPath() + categoryId + "/";

            assertEquals(expectedNewParentPath, updatedParent.getPath());

            // then - 하위 카테고리 path 확인
            Category updatedChild = categoryRepository.findById(targetChildId)
                    .orElseThrow(() -> new AssertionError("자식 카테고리 조회 실패"));
            String expectedChildPath = expectedNewParentPath + targetChildId + "/";
            assertEquals(expectedChildPath, updatedChild.getPath());

            // 부모 연결도 바뀌었는지 확인
            assertEquals(updatedParent, updatedChild.getParent());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 카테고리 ID로 수정 시 404 예외")
        void test_fail_update_not_found_category() throws Exception {
            // given
            CategoryUpdateRequest request = new CategoryUpdateRequest("없는", null, 1, "/none", true);

            // when & then
            mockMvc.perform(patch("/categories/{id}", 99999L)
                            .contentType(APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 부모 지정 시 404 예외")
        void test_fail_update_without_parent() throws Exception {
            // given
            CategoryUpdateRequest request = new CategoryUpdateRequest("변경", 9999L, 1, "/fail", true);

            // when & then
            mockMvc.perform(patch("/categories/{id}", childId)
                            .contentType(APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패: 자기 자신을 부모로 지정 시 400 예외")
        void test_fail_update_self_as_parent() throws Exception {
            // given
            CategoryUpdateRequest request = new CategoryUpdateRequest("자기참조", childId, 1, "/fail", true);

            // when & then
            mockMvc.perform(patch("/categories/{id}", childId)
                            .contentType(APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 자식을 부모로 지정하여 순환 참조 시 400 예외")
        void test_fail_update_with_circular_reference() throws Exception {
            // given
            Long grandChildId = createCategory("손자", childId).getId();

            CategoryUpdateRequest request = new CategoryUpdateRequest("루트", grandChildId, 1, "/fail", true);

            // when & then
            mockMvc.perform(patch("/categories/{id}", rootId)
                            .contentType(APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: 변경된 부모 내 동일한 이름 존재 시 409 예외")
        void test_fail_update_with_duplicate_title_in_new_parent() throws Exception {
            // given
            Long newParentId = createCategory("신상", null).getId();
            createCategory("반팔티", newParentId); // 같은 이름 미리 생성

            CategoryUpdateRequest request = new CategoryUpdateRequest("반팔티", newParentId, 1, "/dup", true);

            // when & then
            mockMvc.perform(patch("/categories/{id}", childId)
                            .contentType(APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("카테고리 삭제")
    class DeleteCategoryTest {

        @Test
        @DisplayName("성공: 하위 카테고리 없는 경우 삭제. deleted(true), deletedAt 설정")
        void test_success_delete_category_without_children() throws Exception {
            // given
            Long categoryId = createCategory("삭제대상", null).getId();

            // when
            mockMvc.perform(delete("/categories/{id}", categoryId))
                    .andExpect(status().isOk());

            // then
            Category deleted = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new AssertionError("카테고리 조회 실패"));

            assertTrue(deleted.getDeleted());
            assertNotNull(deleted.getDeletedAt());
        }

        @Test
        @DisplayName("실패: 하위 카테고리가 있는 경우 삭제 시 400 예외")
        void test_fail_delete_category_with_children() throws Exception {
            // given
            Long parentId = createCategory("부모", null).getId();
            createCategory("자식", parentId); // 하위 카테고리 존재

            // when & then
            mockMvc.perform(delete("/categories/{id}", parentId))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("카테고리 조회")
    class GetCategoryTest {

        private Long rootId;
        private Long childId;
        private Long grandChildId;

        @BeforeEach
        void setUp() throws Exception {
            rootId = createCategory("여성의류", null).getId();
            childId = createCategory("상의", rootId).getId();
            grandChildId = createCategory("반팔티", childId).getId();
        }

        @Test
        @DisplayName("성공: 전체 카테고리 트리 조회")
        void test_success_get_all_tree() throws Exception {
            mockMvc.perform(get("/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(rootId))
                    .andExpect(jsonPath("$.data[0].children[0].id").value(childId))
                    .andExpect(jsonPath("$.data[0].children[0].children[0].id").value(grandChildId));
        }

        @Test
        @DisplayName("성공: 특정 카테고리 ID 기준 트리 조회")
        void test_success_get_sub_tree() throws Exception {
            mockMvc.perform(get("/categories")
                            .param("parentId", childId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(childId))
                    .andExpect(jsonPath("$.data[0].children[0].id").value(grandChildId));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 카테고리 ID 조회 시 404 발생")
        void test_fail_get_not_found_category() throws Exception {
            mockMvc.perform(get("/categories")
                            .param("parentId", "999999"))
                    .andExpect(status().isNotFound());
        }
    }
}
