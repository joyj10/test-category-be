package com.musinsa.shop.domain.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "카테고리 요청 Model")
public class CategoryRequest {

    @NotBlank(message = "카테고리 이름은 필수입니다.")
    @Schema(description = "카테고리 이름", example = "상의")
    private String title;

    @Schema(description = "부모 카테고리 ID (root는 null)", example = "1")
    private Long parentId;

    @Min(value = 0, message = "정렬 순서는 0보다 작을 수 없습니다.")
    @Schema(description = "카테고리 정렬 순서", example = "0")
    private Integer displayOrder;

    @Schema(description = "클릭 이동 URL", example = "/category/top")
    private String link;

    @Schema(description = "카테고리 표시 여부", example = "true")
    private Boolean active;
}
