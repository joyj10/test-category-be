package com.musinsa.shop.domain.category.dto;

import com.musinsa.shop.domain.category.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "카테고리 응답")
public class CategoryResponse {

    @Schema(description = "카테고리 ID", example = "1")
    private Long id;

    @Schema(description = "카테고리 이름", example = "상의")
    private String title;

    public static CategoryResponse of(Category category) {
        return new CategoryResponse(category.getId(), category.getTitle());
    }
}
