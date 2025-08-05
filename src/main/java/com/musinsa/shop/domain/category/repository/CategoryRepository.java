package com.musinsa.shop.domain.category.repository;

import com.musinsa.shop.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, CategoryRepositoryCustom {
    boolean existsByParentIdAndTitle(Long parentId, String title);

    @Modifying
    @Query("UPDATE Category c " +
            "SET c.path = CONCAT(:newPath, SUBSTRING(c.path, LENGTH(:oldPath) + 1)) " +
            "WHERE c.path LIKE CONCAT(:oldPath, '%') " +
            "AND c.id <> :selfId " +
            "AND c.deleted = false")
    void bulkUpdatePath(@Param("oldPath") String oldPath, @Param("newPath") String newPath, @Param("selfId") Long selfId);

    boolean existsByParentIdAndDeletedFalse(Long parentId);
}
