package ru.catwarden.advweb.comment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.catwarden.advweb.ad.Advertisement;
import ru.catwarden.advweb.ad.AdvertisementRepository;
import ru.catwarden.advweb.comment.dto.CommentRequest;
import ru.catwarden.advweb.comment.dto.CommentResponse;
import ru.catwarden.advweb.comment.dto.CommentUpdateRequest;
import ru.catwarden.advweb.exception.DetailedAccessDeniedException;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.security.SecurityUtils;
import ru.catwarden.advweb.user.User;
import ru.catwarden.advweb.user.UserResponseAssembler;
import ru.catwarden.advweb.user.UserRepository;
import ru.catwarden.advweb.user.dto.ShortUserInfoResponse;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    private final CommentMapper commentMapper;
    private final UserResponseAssembler userResponseAssembler;

    private final CommentRepository commentRepository;
    private final AdvertisementRepository advertisementRepository;
    private final UserRepository userRepository;

    public Page<CommentResponse> getAllUnmoderatedComments(Pageable pageable){
        return commentRepository.findAllByIsModeratedFalse(pageable).
                map(comment -> {
                    CommentResponse response = commentMapper.toResponse(comment);
                    ShortUserInfoResponse authorInfo = userResponseAssembler.toShortUserInfoResponse(comment.getAuthor());
                    response.setAuthorInfo(authorInfo);
                    return response;
                });
    }

    @Cacheable(
            value = "comments-list",
            key = "'ad-' + #advertisementId + '-p-' + #pageable.pageNumber + '-s-' + #pageable.pageSize",
            condition = "#pageable.pageNumber == 0"
    )
    public Page<CommentResponse> getAdvertisementModeratedComments(Long advertisementId, Pageable pageable){
        return commentRepository.findAllByAdIdAndIsModeratedTrue(advertisementId, pageable)
                .map(comment -> {
                    CommentResponse response = commentMapper.toResponse(comment);

                    ShortUserInfoResponse authorInfo = userResponseAssembler.toShortUserInfoResponse(comment.getAuthor());
                    response.setAuthorInfo(authorInfo);

                    return response;
                });
    }

    @CacheEvict(value = "comments-list", allEntries = true)
    public void createComment(CommentRequest commentRequest){
        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();
        User currentUser = userRepository.findByKeycloakId(currentKeycloakId)
                .orElseThrow(() -> new EntityNotFoundException(User.class, currentKeycloakId));

        Advertisement advertisement = advertisementRepository.findById(commentRequest.getAdvertisementId())
            .orElseThrow(() -> new EntityNotFoundException(Advertisement.class, commentRequest.getAdvertisementId()));

        Comment comment = commentMapper.toEntity(commentRequest);
        comment.setAuthor(currentUser);
        comment.setAd(advertisement);

        commentRepository.save(comment);

        log.info(
                "AUDIT comment created: commentId={}, adId={}, authorId={}, isModerated={}",
                comment.getId(),
                getAdvertisementId(comment),
                getAuthorId(comment),
                comment.getIsModerated()
        );
    }

    @CacheEvict(value = "comments-list", allEntries = true)
    public void updateComment(Long id, CommentUpdateRequest commentUpdateRequest){
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Comment.class, id));

        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        if (!isAdmin && !comment.getAuthor().getKeycloakId().equals(currentKeycloakId)) {
            throw new DetailedAccessDeniedException("You are not allowed to update this comment",
                    Map.of(
                            "Comment id:", comment.getId(),
                            "Comment author keycloak id:", String.valueOf(comment.getAuthor().getKeycloakId()),
                            "Current user keycloak id:", String.valueOf(currentKeycloakId)
                    ));
        }

        comment.setText(commentUpdateRequest.getText());
        comment.setIsModerated(false);

        commentRepository.save(comment);

        log.info(
                "AUDIT comment updated: commentId={}, adId={}, authorId={}, isModerated={}",
                comment.getId(),
                getAdvertisementId(comment),
                getAuthorId(comment),
                comment.getIsModerated()
        );
    }

    @CacheEvict(value = "comments-list", allEntries = true)
    public void updateCommentOnModeration(Long id, CommentRequest commentRequest){
        Comment comment = commentRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(Comment.class, id));

        comment.setText(commentRequest.getText());
        comment.setIsModerated(true);

        commentRepository.save(comment);

        log.info(
                "AUDIT comment on moderation updated: commentId={}, adId={}, authorId={}, isModerated={}",
                comment.getId(),
                getAdvertisementId(comment),
                getAuthorId(comment),
                comment.getIsModerated()
        );
    }

    @CacheEvict(value = "comments-list", allEntries = true)
    public void deleteComment(Long id){
        if(!commentRepository.existsById(id)){
            throw new EntityNotFoundException(Comment.class, id);
        }

        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Comment.class, id));

        String currentKeycloakId = SecurityUtils.getCurrentUserKeycloakId();
        boolean isAdmin = SecurityUtils.isCurrentUserAdmin();

        if (!isAdmin && !comment.getAuthor().getKeycloakId().equals(currentKeycloakId)) {
            throw new DetailedAccessDeniedException("You are not allowed to delete this comment",
                    Map.of(
                            "Comment id:", comment.getId(),
                            "Comment author keycloak id:", String.valueOf(comment.getAuthor().getKeycloakId()),
                            "Current user keycloak id:", String.valueOf(currentKeycloakId)
                    ));
        }

        commentRepository.deleteById(id);

        log.info(
                "AUDIT comment deleted: commentId={}, adId={}, authorId={}",
                comment.getId(),
                getAdvertisementId(comment),
                getAuthorId(comment)
        );
    }

    @CacheEvict(value = "comments-list", allEntries = true)
    public void deleteCommentsByAdId(Long adId){
        if(!advertisementRepository.existsById(adId)){
            throw new EntityNotFoundException(Advertisement.class, adId);
        }

        commentRepository.deleteAllByAdId(adId);

        log.info("AUDIT all comments deleted: adId={}", adId);
    }

    private Long getAuthorId(Comment comment) {
        return comment.getAuthor() != null ? comment.getAuthor().getId() : null;
    }

    private Long getAdvertisementId(Comment comment) {
        return comment.getAd() != null ? comment.getAd().getId() : null;
    }

}
