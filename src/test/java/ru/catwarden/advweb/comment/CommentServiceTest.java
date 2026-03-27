package ru.catwarden.advweb.comment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.catwarden.advweb.ad.Advertisement;
import ru.catwarden.advweb.ad.AdvertisementRepository;
import ru.catwarden.advweb.comment.dto.CommentRequest;
import ru.catwarden.advweb.comment.dto.CommentResponse;
import ru.catwarden.advweb.comment.dto.CommentUpdateRequest;
import ru.catwarden.advweb.exception.DetailedAccessDeniedException;
import ru.catwarden.advweb.exception.EntityNotFoundException;
import ru.catwarden.advweb.security.SecurityUtils;
import ru.catwarden.advweb.user.User;
import ru.catwarden.advweb.user.UserRepository;
import ru.catwarden.advweb.user.UserResponseAssembler;
import ru.catwarden.advweb.user.dto.ShortUserInfoResponse;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentMapper commentMapper;
    @Mock
    private UserResponseAssembler userResponseAssembler;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private AdvertisementRepository advertisementRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    void getAllUnmoderatedCommentsAddsAuthorInfo() {
        User author = User.builder().id(1L).build();
        Comment comment = Comment.builder().id(10L).author(author).text("text").isModerated(false).build();
        CommentResponse response = CommentResponse.builder().id(10L).text("text").isModerated(false).build();
        ShortUserInfoResponse authorInfo = ShortUserInfoResponse.builder().id(1L).firstName("Ivan").build();

        when(commentRepository.findAllByIsModeratedFalse(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(java.util.List.of(comment), PageRequest.of(0, 10), 1));
        when(commentMapper.toResponse(comment)).thenReturn(response);
        when(userResponseAssembler.toShortUserInfoResponse(author)).thenReturn(authorInfo);

        Page<CommentResponse> result = commentService.getAllUnmoderatedComments(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(authorInfo, result.getContent().getFirst().getAuthorInfo());
    }

    @Test
    void getAdvertisementModeratedCommentsAddsAuthorInfo() {
        User author = User.builder().id(2L).build();
        Comment comment = Comment.builder().id(11L).author(author).text("text2").isModerated(true).build();
        CommentResponse response = CommentResponse.builder().id(11L).text("text2").isModerated(true).build();
        ShortUserInfoResponse authorInfo = ShortUserInfoResponse.builder().id(2L).firstName("Petr").build();

        when(commentRepository.findAllByAdIdAndIsModeratedTrue(7L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(java.util.List.of(comment), PageRequest.of(0, 10), 1));
        when(commentMapper.toResponse(comment)).thenReturn(response);
        when(userResponseAssembler.toShortUserInfoResponse(author)).thenReturn(authorInfo);

        Page<CommentResponse> result = commentService.getAdvertisementModeratedComments(7L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(authorInfo, result.getContent().getFirst().getAuthorInfo());
    }

    @Test
    void createCommentSetsAuthorAndAdvertisementAndSaves() {
        CommentRequest request = CommentRequest.builder()
                .advertisementId(5L)
                .text("This is a long enough comment text to satisfy validation requirements.")
                .build();
        User user = User.builder().id(3L).keycloakId("kc-user").build();
        Advertisement advertisement = Advertisement.builder().id(5L).build();
        Comment mappedComment = Comment.builder().text(request.getText()).isModerated(false).build();

        when(userRepository.findByKeycloakId("kc-user")).thenReturn(Optional.of(user));
        when(advertisementRepository.findById(5L)).thenReturn(Optional.of(advertisement));
        when(commentMapper.toEntity(request)).thenReturn(mappedComment);

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("kc-user");
            commentService.createComment(request);
        }

        assertEquals(user, mappedComment.getAuthor());
        assertEquals(advertisement, mappedComment.getAd());
        verify(commentRepository).save(mappedComment);
    }

    @Test
    void createCommentThrowsWhenCurrentUserNotFound() {
        CommentRequest request = CommentRequest.builder()
                .advertisementId(5L)
                .text("This is a long enough comment text to satisfy validation requirements.")
                .build();
        when(userRepository.findByKeycloakId("kc-user")).thenReturn(Optional.empty());

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("kc-user");
            assertThrows(EntityNotFoundException.class, () -> commentService.createComment(request));
        }
    }

    @Test
    void updateCommentThrowsWhenUserHasNoAccess() {
        User author = User.builder().keycloakId("author-id").build();
        Comment comment = Comment.builder().id(1L).author(author).text("old").isModerated(true).build();
        CommentUpdateRequest request = CommentUpdateRequest.builder()
                .text("Updated comment text that is also long enough for validation rules.")
                .build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("another-user");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            DetailedAccessDeniedException exception = assertThrows(DetailedAccessDeniedException.class,
                    () -> commentService.updateComment(1L, request));
            assertEquals("You are not allowed to update this comment", exception.getMessage());
            assertEquals(
                    Map.of(
                            "Comment id:", 1L,
                            "Comment author keycloak id:", "author-id",
                            "Actor id:", "another-user"
                    ),
                    exception.getDetails()
            );
        }

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void updateCommentUpdatesTextAndResetsModeration() {
        User author = User.builder().keycloakId("author-id").build();
        Comment comment = Comment.builder().id(1L).author(author).text("old").isModerated(true).build();
        CommentUpdateRequest request = CommentUpdateRequest.builder()
                .text("Updated comment text that is also long enough for validation rules.")
                .build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("author-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            commentService.updateComment(1L, request);
        }

        assertEquals(request.getText(), comment.getText());
        assertFalse(comment.getIsModerated());
        verify(commentRepository).save(comment);
    }

    @Test
    void updateCommentOnModerationSetsTextAndModeratedFlag() {
        Comment comment = Comment.builder().id(1L).text("old").isModerated(false).build();
        CommentRequest request = CommentRequest.builder()
                .advertisementId(5L)
                .text("This is another long enough moderation text for update operation test.")
                .build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        commentService.updateCommentOnModeration(1L, request);

        assertEquals(request.getText(), comment.getText());
        assertTrue(comment.getIsModerated());
        verify(commentRepository).save(comment);
    }

    @Test
    void deleteCommentThrowsWhenCommentMissingByIdCheck() {
        when(commentRepository.existsById(1L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> commentService.deleteComment(1L));
        verify(commentRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteCommentThrowsWhenUserHasNoAccess() {
        User author = User.builder().keycloakId("author-id").build();
        Comment comment = Comment.builder().id(1L).author(author).build();
        when(commentRepository.existsById(1L)).thenReturn(true);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("another-user");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            DetailedAccessDeniedException exception = assertThrows(DetailedAccessDeniedException.class,
                    () -> commentService.deleteComment(1L));
            assertEquals("You are not allowed to delete this comment", exception.getMessage());
            assertEquals(
                    Map.of(
                            "Comment id:", 1L,
                            "Comment author keycloak id:", "author-id",
                            "Actor id:", "another-user"
                    ),
                    exception.getDetails()
            );
        }

        verify(commentRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteCommentDeletesWhenOwner() {
        User author = User.builder().keycloakId("author-id").build();
        Comment comment = Comment.builder().id(1L).author(author).build();
        when(commentRepository.existsById(1L)).thenReturn(true);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentUserKeycloakId).thenReturn("author-id");
            securityUtilsMockedStatic.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            commentService.deleteComment(1L);
        }

        verify(commentRepository).deleteById(1L);
    }

    @Test
    void deleteCommentsByAdIdThrowsWhenAdNotFound() {
        when(advertisementRepository.existsById(5L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> commentService.deleteCommentsByAdId(5L));
        verify(commentRepository, never()).deleteAllByAdId(any(Long.class));
    }

    @Test
    void deleteCommentsByAdIdDeletesWhenAdExists() {
        when(advertisementRepository.existsById(5L)).thenReturn(true);

        commentService.deleteCommentsByAdId(5L);

        verify(commentRepository).deleteAllByAdId(5L);
    }
}

