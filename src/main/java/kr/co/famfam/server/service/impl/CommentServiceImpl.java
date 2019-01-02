package kr.co.famfam.server.service.impl;

import kr.co.famfam.server.domain.Comment;
import kr.co.famfam.server.domain.User;
import kr.co.famfam.server.model.CommentDto;
import kr.co.famfam.server.model.DefaultRes;
import kr.co.famfam.server.repository.CommentRepository;
import kr.co.famfam.server.repository.UserRepository;
import kr.co.famfam.server.service.CommentService;
import kr.co.famfam.server.utils.ResponseMessage;
import kr.co.famfam.server.utils.StatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by ehay@naver.com on 2018-12-25
 * Blog : http://ehay.tistory.com
 * Github : http://github.com/ehayand
 */

@Slf4j
@Service
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public CommentServiceImpl(CommentRepository commentRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    public DefaultRes findCommentsByContentIdx(int contentIdx) {
        try {
            final List<Comment> comments = commentRepository.findCommentsByContentIdxOrderByCreatedDateAsc(contentIdx);
            if (comments.isEmpty())
                return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NOT_FOUND_COMMENT);

            return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_COMMENT, comments);
        } catch (Exception e) {
            log.error(e.getMessage());
            return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
        }
    }

    public DefaultRes countThisWeek(int userIdx) {
        Optional<User> user = userRepository.findById(userIdx);
        if (!user.isPresent())
            return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NOT_FOUND_USER);

        final List<User> groupUsers = userRepository.findUsersByGroupIdx(user.get().getGroupIdx());
        if (groupUsers.isEmpty())
            return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NOT_FOUND_USER);

        try {
            LocalDateTime startDateTime = getStartDateTime();
            LocalDateTime endDateTime = LocalDateTime.of(startDateTime.plusDays(6).toLocalDate(), LocalTime.of(23, 59, 59));
            long count = 0;

            for (User u : groupUsers) {
                count += commentRepository.countByUserIdxAndCreatedDateBetween(u.getUserIdx(), startDateTime, endDateTime);
            }

            return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_COMMENT, count);
        } catch (Exception e) {
            log.error(e.getMessage());
            return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
        }
    }

    @Transactional
    public DefaultRes save(CommentDto commentDto) {
        try {
            commentRepository.save(new Comment(commentDto));
            return DefaultRes.res(StatusCode.CREATED, ResponseMessage.CREATED_COMMENT);
        } catch (Exception e) {
            //Rollback
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error(e.getMessage());
            return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
        }
    }

    @Transactional
    public DefaultRes update(int commentIdx, CommentDto commentDto) {
        try {
            Optional<Comment> comment = commentRepository.findById(commentIdx);
            if (!comment.isPresent())
                return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NOT_FOUND_COMMENT);

            comment.get().setContent(commentDto.getContent());

            commentRepository.save(comment.get());
            return DefaultRes.res(StatusCode.NO_CONTENT, ResponseMessage.UPDATE_COMMENT);
        } catch (Exception e) {
            //Rollback
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error(e.getMessage());
            return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
        }
    }

    @Transactional
    public DefaultRes delete(int commentIdx) {
        try {
            Optional<Comment> comment = commentRepository.findById(commentIdx);
            if (!comment.isPresent())
                return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NOT_FOUND_COMMENT);

            commentRepository.delete(comment.get());
            return DefaultRes.res(StatusCode.NO_CONTENT, ResponseMessage.DELETE_COMMENT);
        } catch (Exception e) {
            //Rollback
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error(e.getMessage());
            return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
        }
    }

    private LocalDateTime getStartDateTime() {
        LocalDate today = LocalDate.now();
        LocalDateTime startDateTime =
                LocalDateTime.of(today.minusDays(today.getDayOfWeek().getValue() - 1), LocalTime.of(0, 0, 0));
        return startDateTime;
    }
}
