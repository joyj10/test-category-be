package com.musinsa.shop.domain.category.entity;

import com.musinsa.shop.common.exception.InvalidRequestException;
import com.musinsa.shop.domain.category.dto.CategoryUpdateRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "category",
        indexes = {
                @Index(name = "idx_category_tree_query", columnList = "path, deleted, active")
        }
)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Category parent;

    @OneToMany(mappedBy = "parent")
    private List<Category> children = new ArrayList<>();

    @Column(name = "path", length = 512)
    private String path;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "link", length = 512)
    private String link;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "deleted")
    private Boolean deleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== 연관 관계 메서드 ===== //
    public void addChildCategory(Category child) {
        this.children.add(child);
        child.parent = this;
    }

    public void removeChildCategory(Category child) {
        this.children.remove(child);
        child.parent = null;
    }

    // ===== 생성 메서드 ===== //
    public static Category create(String title, Integer displayOrder, String link, Boolean active, Category parent) {
        validateTitle(title);

        Category category = new Category();
        category.title = title;
        category.displayOrder = (displayOrder != null) ? displayOrder : 9999;
        category.link = link;
        category.active = (active != null) ? active : true;
        category.parent = parent;
        category.deleted = false;

        // 연관 관계 설정
        if (parent != null) {
            parent.addChildCategory(category);
        }

        return category;
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidRequestException("카테고리 이름은 필수입니다.");
        }
    }

    // ===== 필드 수정 메서드 ===== //
    // 카테고리의 필드 + 부모 변경을 함께 처리하는 통합 메서드
    public void updateFieldAndParent(CategoryUpdateRequest updateRequest, Category newParent, boolean parentChanged) {
        update(updateRequest.getTitle(), updateRequest.getDisplayOrder(), updateRequest.getLink(), updateRequest.getActive());

        // 부모 카테고리 변경된 경우 변경 처리
        if (parentChanged) {
            changeParent(newParent);
        }
    }

    private void update(String title, Integer displayOrder, String link, Boolean active) {
        if (title != null) this.title = title;
        if (displayOrder != null) this.displayOrder = displayOrder;
        if (link != null) this.link = link;
        if (active != null) this.active = active;
    }

    // ===== 부모 변경 메서드 ===== //
    private void changeParent(Category newParent) {
        // 기존 부모를 children 제거
        if (this.parent != null) {
            this.parent.removeChildCategory(this);
        }

        // 새로운 부모 설정
        this.parent = newParent;
        if (newParent != null) {
            newParent.addChildCategory(this);
        }

        updatePath();
    }

    // ===== path 변경 메서드  ===== //
    public void updatePath() {
        this.path = (parent != null)
                ? parent.getPath() + this.id + "/"
                : "/" + this.id + "/";
    }

    // ===== 삭제 처리 메서드 ===== //
    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();

        // 연관관계 정리
        if (this.parent != null) {
            this.parent.removeChildCategory(this);
        }
    }
}
