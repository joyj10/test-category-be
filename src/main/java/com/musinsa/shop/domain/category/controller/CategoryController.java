package com.musinsa.shop.domain.category.controller;

import com.musinsa.shop.common.response.ResultResponse;
import com.musinsa.shop.domain.category.dto.CategoryRequest;
import com.musinsa.shop.domain.category.dto.CategoryResponse;
import com.musinsa.shop.domain.category.dto.CategoryTreeResponse;
import com.musinsa.shop.domain.category.dto.CategoryUpdateRequest;
import com.musinsa.shop.domain.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "카테고리 API", description = "카테고리 등록, 수정, 삭제, 조회 API")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "카테고리 등록")
    public ResultResponse<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest categoryRequest) {
        CategoryResponse result = categoryService.createCategory(categoryRequest);
        return ResultResponse.success(result);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "카테고리 수정")
    public ResultResponse<CategoryResponse> updateCategory(@PathVariable Long id,
                                                           @RequestBody CategoryUpdateRequest categoryRequest) {
        CategoryResponse result = categoryService.updateCategory(id, categoryRequest);
        return ResultResponse.success(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "카테고리 삭제")
    public ResultResponse<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResultResponse.success(null);
    }

    @GetMapping
    @Operation(summary = "카테고리 트리 조회")
    public ResultResponse<List<CategoryTreeResponse>> getCategories(@RequestParam(required = false) Long parentId) {
        List<CategoryTreeResponse> categories = categoryService.getCategories(parentId);
        return ResultResponse.success(categories);
    }
}
