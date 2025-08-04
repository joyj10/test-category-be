package com.musinsa.shop.domain.category.service;

import com.musinsa.shop.common.exception.DuplicateResourceException;
import com.musinsa.shop.common.exception.InvalidRequestException;
import com.musinsa.shop.common.exception.ResourceNotFoundException;
import com.musinsa.shop.domain.category.dto.CategoryRequest;
import com.musinsa.shop.domain.category.dto.CategoryResponse;
import com.musinsa.shop.domain.category.dto.CategoryUpdateRequest;
import com.musinsa.shop.domain.category.entity.Category;
import com.musinsa.shop.domain.category.repository.CategoryRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
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


    /**
     * 카테고리 수정
     * - 필드 값 수정
     * - 부모 카테고리 변경 시 유효성 체크 및 path, 하위 path 변경
     */
    @Transactional
    public CategoryResponse updateCategory(Long categoryId, @Valid CategoryUpdateRequest updateRequest) {
        Category category = getCategory(categoryId);
        String oldPath = category.getPath();

        Category newParent = getValidatedParent(updateRequest.getParentId(), categoryId);

        // 카테고리 필드 업데이트
        category.update(
                updateRequest.getTitle(),
                resolveDisplayOrder(updateRequest.getDisplayOrder()),
                updateRequest.getLink(),
                resolveActive(updateRequest.getActive())
        );

        // 부모가 변경된 경우에만 path 갱신 및 하위 카테고리 path 일괄 변경
        if (isParentChanged(category, newParent)) {
            updateParentAndPath(category, newParent, oldPath);
        }

        return CategoryResponse.of(category);
    }

    // 카테고리 단건 조회 : ID 기준
    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("카테고리를 찾을 수 없습니다."));
    }

    // 부모 카테고리 유효성: 자기 자신 지정 체크 부모 지정 X & 존재 유효성 & 순환 참조 여부
    private Category getValidatedParent(Long parentId, Long selfId) {
        if (parentId == null) return null;

        if (parentId.equals(selfId)) {
            throw new InvalidRequestException("자기 자신을 부모로 지정할 수 없습니다.");
        }

        Category newParent = categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("부모 카테고리가 존재하지 않습니다."));

        if (isDescendantPath(newParent, selfId)) {
            throw new InvalidRequestException("하위 카테고리를 부모로 지정할 수 없습니다.");
        }

        return newParent;
    }

    // 순환 참조 여부: 변경 parent가 자신의 하위 카테고리인 경우 true
    private boolean isDescendantPath(Category newParent, Long selfId) {
        return newParent.getPath() != null &&
                newParent.getPath().contains("/" + selfId + "/");
    }

    // 부모 카테고리 변경 여부
    private boolean isParentChanged(Category category, Category newParent) {
        Long oldParentId = category.getParent() != null ? category.getParent().getId() : null;
        Long newParentId = newParent != null ? newParent.getId() : null;
        return !Objects.equals(oldParentId, newParentId);
    }

    // 부모 카테고리 변경 및 path 갱신 처리 : 하위 카테고리 path는 bulk update로 일괄 갱신
    private void updateParentAndPath(Category category, Category newParent, String oldPath) {
        category.changeParent(newParent);
        String newPath = category.getPath();

        categoryRepository.bulkUpdatePath(oldPath, newPath, category.getId());
    }
}
