package com.musinsa.shop.domain.category.service;

import com.musinsa.shop.common.exception.DuplicateResourceException;
import com.musinsa.shop.common.exception.ResourceNotFoundException;
import com.musinsa.shop.domain.category.dto.CategoryRequest;
import com.musinsa.shop.domain.category.dto.CategoryResponse;
import com.musinsa.shop.domain.category.entity.Category;
import com.musinsa.shop.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * 카테고리 등록
     */
    @Transactional
    public CategoryResponse createCategory(CategoryRequest categoryRequest) {
        validateDuplicateTitle(categoryRequest.getParentId(), categoryRequest.getTitle());

        Category parent = getParentOrNull(categoryRequest.getParentId());

        Category category = Category.create(
                categoryRequest.getTitle(),
                resolveDisplayOrder(categoryRequest.getDisplayOrder()),
                categoryRequest.getLink(),
                resolveActive(categoryRequest.getActive()),
                parent
        );

        // 카테고리 저장 후 path, depth 세팅
        category = categoryRepository.save(category);
        category.updatePath();

        return CategoryResponse.of(category);
    }

    // 유효성 체크: 이름 중복 (동일 상위 카테고리 내 이름 동일 시 중복)
    private void validateDuplicateTitle(Long parentId, String title) {
        boolean exists = categoryRepository.existsByParentIdAndTitle(parentId, title);
        if (exists) {
            throw new DuplicateResourceException("이미 존재하는 카테고리명입니다.");
        }
    }

    // parentId가 존재할 경우 상위 카테고리 조회, 없으면 null 반환
    private Category getParentOrNull(Long parentId) {
        return Optional.ofNullable(parentId)
                .map(id -> categoryRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("상위 카테고리를 찾을 수 없습니다."))
                )
                .orElse(null);
    }

    // displayOrder 값이 null인 경우 기본값(9999) 처리
    private int resolveDisplayOrder(Integer displayOrder) {
        return Optional.ofNullable(displayOrder).orElse(9999);
    }

    // active 값이 null인 경우 기본값(true) 처리
    private boolean resolveActive(Boolean active) {
        return Optional.ofNullable(active).orElse(true);
    }
}
