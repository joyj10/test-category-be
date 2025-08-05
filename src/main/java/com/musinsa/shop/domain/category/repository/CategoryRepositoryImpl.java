package com.musinsa.shop.domain.category.repository;

import com.musinsa.shop.domain.category.dto.CategoryDto;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.musinsa.shop.domain.category.entity.QCategory.category;

@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepositoryCustom {
    private final JPAQueryFactory queryFactory;


    @Override
    public List<CategoryDto> findAllTree() {
        return treeBaseQuery()
                .where(category.deleted.isFalse(), category.active.isTrue())
                .fetch();
    }

    @Override
    public List<CategoryDto> findTreeByPath(String path) {
        return treeBaseQuery()
                .where(
                        category.path.startsWith(path),
                        category.deleted.isFalse(),
                        category.active.isTrue()
                )
                .fetch();
    }

    // 트리 기본 쿼리
    private JPQLQuery<CategoryDto> treeBaseQuery() {
        return queryFactory
                .select(Projections.fields(CategoryDto.class,
                        category.id,
                        category.title,
                        category.parent.id.as("parentId"),
                        category.path,
                        category.depth,
                        category.link,
                        category.displayOrder,
                        category.active,
                        category.deletedAt,
                        category.createdAt,
                        category.updatedAt
                ))
                .from(category)
                .orderBy(category.path.asc());
    }
}
