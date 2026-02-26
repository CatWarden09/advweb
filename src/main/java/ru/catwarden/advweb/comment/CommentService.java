package ru.catwarden.advweb.comment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.catwarden.advweb.ad.Advertisement;
import ru.catwarden.advweb.ad.AdvertisementRepository;
import ru.catwarden.advweb.comment.dto.CommentRequest;
import ru.catwarden.advweb.comment.dto.CommentResponse;
import ru.catwarden.advweb.repository.UserRepository;
import ru.catwarden.advweb.user.User;
import ru.catwarden.advweb.user.UserMapper;
import ru.catwarden.advweb.user.dto.ShortUserInfoResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;

    private final CommentRepository commentRepository;
    private final AdvertisementRepository advertisementRepository;
    private final UserRepository userRepository;

    public void createComment(CommentRequest commentRequest){
        User author = userRepository.findById(commentRequest.getAuthorId())
            .orElseThrow(() -> new RuntimeException("User not found"));

        Advertisement advertisement = advertisementRepository.findById(commentRequest.getAdvertisementId())
            .orElseThrow(() -> new RuntimeException("Advertisement not found"));

        Comment comment = commentMapper.toEntity(commentRequest);
        comment.setAuthor(author);
        comment.setAd(advertisement);

        commentRepository.save(comment);
    }

    public List<CommentResponse> getAdvertisementComments(Long advertisementId){
        return commentRepository.findAllByAdId(advertisementId)
            .stream()
            .map(comment -> {
                CommentResponse response = commentMapper.toResponse(comment);

                ShortUserInfoResponse authorInfo = userMapper.toShortUserInfoResponse(comment.getAuthor());
                response.setAuthorInfo(authorInfo);

                return response;
            })
            .toList();

    }
}
