package ru.catwarden.advweb.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByAdId(Long id);

    Page<Comment> findAllByAdIdAndIsModeratedTrue(Long id, Pageable pageable);

    Page<Comment> findAllByIsModeratedFalse(Pageable pageable);

    void deleteAllByAdId(Long id);
}
