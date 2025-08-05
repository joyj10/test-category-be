package com.musinsa.shop.domain.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "카테고리 트리 구조 응답")
public class CategoryTreeResponse {

    private Long id;
    private String title;
    private Long parentId;
    private String link;
    private int displayOrder;
    private boolean active;
    private List<CategoryTreeResponse> children = new ArrayList<>();

    public static CategoryTreeResponse of(CategoryDto category) {
        CategoryTreeResponse dto = new CategoryTreeResponse();
        dto.id = category.getId();
        dto.title = category.getTitle();
        dto.parentId = category.getParentId();
        dto.link = category.getLink();
        dto.displayOrder = category.getDisplayOrder();
        dto.active = category.isActive();
        return dto;
    }
}
