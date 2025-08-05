package com.musinsa.shop.domain.category.repository;

import com.musinsa.shop.domain.category.dto.CategoryDto;

import java.util.List;

public interface CategoryRepositoryCustom {
    List<CategoryDto> findAllTree();

    List<CategoryDto> findTreeByPath(String path);
}
