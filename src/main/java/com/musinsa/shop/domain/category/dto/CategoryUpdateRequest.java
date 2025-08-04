package com.musinsa.shop.domain.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "카테고리 수정 요청 Model")
public class CategoryUpdateRequest {

    @Schema(description = "카테고리 제목", example = "상의")
    private String title;

    @Schema(description = "부모 카테고리 ID (root는 null)", example = "1")
    private Long parentId;

    @Schema(description = "카테고리 정렬 순서", example = "0")
    private Integer displayOrder;

    @Schema(description = "카테고리 링크", example = "/category/top")
    private String link;

    @Schema(description = "카테고리 활성 여부", example = "true")
    private Boolean active;
}
