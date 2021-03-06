package kr.co.famfam.server.service.impl;

import kr.co.famfam.server.domain.Content;
import kr.co.famfam.server.domain.Feel;
import kr.co.famfam.server.domain.User;
import kr.co.famfam.server.model.DefaultRes;
import kr.co.famfam.server.model.FeelReq;
import kr.co.famfam.server.model.FeelRes;
import kr.co.famfam.server.model.HistoryDto;
import kr.co.famfam.server.repository.ContentRepository;
import kr.co.famfam.server.repository.FeelRepository;
import kr.co.famfam.server.repository.UserRepository;
import kr.co.famfam.server.service.FeelService;
import kr.co.famfam.server.utils.ResponseMessage;
import kr.co.famfam.server.utils.StatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static kr.co.famfam.server.utils.HistoryType.ADD_EMOTION;
import static kr.co.famfam.server.utils.PushType.PUSH_ADD_EMOTION;

/**
 * Created by ehay@naver.com on 2018-12-25
 * Blog : http://ehay.tistory.com
 * Github : http://github.com/ehayand
 */

@Slf4j
@Service
public class FeelServiceImpl implements FeelService {

    private final FeelRepository feelRepository;
    private final UserRepository userRepository;
    private final HistoryServiceImpl historyService;
    private final PushServiceImpl pushService;
    private final ContentRepository contentRepository;

    public FeelServiceImpl(FeelRepository feelRepository, UserRepository userRepository, HistoryServiceImpl historyService, PushServiceImpl pushService, ContentRepository contentRepository) {
        this.feelRepository = feelRepository;
        this.userRepository = userRepository;
        this.historyService = historyService;
        this.pushService = pushService;
        this.contentRepository = contentRepository;
    }

    @Override
    public DefaultRes findFeelsByContentIdx(int contentIdx) {
        try {
            final List<Feel> feels = feelRepository.findFeelsByContentIdxOrderByCreatedAtAsc(contentIdx);

            if (feels.isEmpty())
                return DefaultRes.res(StatusCode.NO_CONTENT, ResponseMessage.NOT_FOUND_FEEL);

            List<Integer> types = new LinkedList<>();
            for (Feel feel : feels)
                types.add(feel.getFeelType());

            Optional<User> firstUser = userRepository.findById(feels.get(0).getUserIdx());
            if (!firstUser.isPresent())
                return DefaultRes.res(StatusCode.BAD_REQUEST, ResponseMessage.INTERNAL_SERVER_ERROR);

            FeelRes feelRes = FeelRes.builder()
                    .feelTypes(feels)
                    .firstUserName(firstUser.get().getUserName())
                    .feelCount(feels.size())
                    .build();

            return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_FEEL, feelRes);
        } catch (Exception e) {
            log.error(e.getMessage());
            return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
        }
    }

    @Override
    public FeelRes findFeelsByContentIdxInternal(int contentIdx) {
        try {
            final List<Feel> feels = feelRepository.findFeelsByContentIdxOrderByCreatedAtAsc(contentIdx);

            if (feels.isEmpty())
                return null;

            List<Integer> types = new LinkedList<>();
            for (Feel feel : feels)
                types.add(feel.getFeelType());

            Optional<User> firstUser = userRepository.findById(feels.get(0).getUserIdx());
            if (!firstUser.isPresent())
                return null;

            FeelRes feelRes = FeelRes.builder()
                    .feelTypes(feels)
                    .firstUserName(firstUser.get().getUserName())
                    .feelCount(feels.size())
                    .build();

            return feelRes;
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @Override
    public DefaultRes countThisWeek(int userIdx) {
        try {
            Optional<User> user = userRepository.findById(userIdx);
            if (!user.isPresent())
                return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NOT_FOUND_USER);

            final List<User> groupUsers = userRepository.findUsersByGroupIdx(user.get().getGroupIdx());
            if (groupUsers.isEmpty())
                return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NOT_FOUND_GROUP);

            LocalDateTime startDateTime = getStartDateTime();
            LocalDateTime endDateTime = LocalDateTime.of(startDateTime.plusDays(6).toLocalDate(), LocalTime.of(23, 59, 59));

            long count = 0;

            for (User u : groupUsers)
                count += feelRepository.countByUserIdxAndCreatedAtBetween(u.getUserIdx(), startDateTime, endDateTime);

            Map<String, Long> result = new HashMap<>();
            result.put("count", count);

            return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_FEEL, result);
        } catch (Exception e) {
            log.error(e.getMessage());
            return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
        }
    }

    @Override
    @Transactional
    public DefaultRes save(FeelReq feelReq) {
        try {
            Optional<Feel> feel = feelRepository.findFeelByContentIdxAndUserIdx(feelReq.getContentIdx(), feelReq.getUserIdx());

            if (feel.isPresent()) {
                feel.get().setFeelType(feelReq.getFeelType());
                feel.get().setCreatedAt(LocalDateTime.now());
                feelRepository.save(feel.get());

                return DefaultRes.res(StatusCode.OK, ResponseMessage.UPDATE_FEEL);
            } else {
                feelRepository.save(new Feel(feelReq));

                HistoryDto historyDto = new HistoryDto(feelReq.getUserIdx(), userRepository.findById(feelReq.getUserIdx()).get().getGroupIdx(), ADD_EMOTION);
                historyService.add(historyDto);

                Optional<Content> content = contentRepository.findById(feelReq.getContentIdx());
                int contentUserIdx = content.get().getUserIdx();
                pushService.sendToDevice(userRepository.findById(contentUserIdx).get().getFcmToken(), PUSH_ADD_EMOTION, userRepository.findById(feelReq.getUserIdx()).get().getUserName());

                return DefaultRes.res(StatusCode.CREATED, ResponseMessage.CREATED_FEEL);
            }
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error(e.getMessage());
            return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
        }
    }

    @Override
    @Transactional
    public DefaultRes delete(int contentIdx, int userIdx) {
        try {
            Optional<Feel> feel = feelRepository.findFeelByContentIdxAndUserIdx(contentIdx, userIdx);
            if (!feel.isPresent())
                return DefaultRes.res(StatusCode.NO_CONTENT, ResponseMessage.NOT_FOUND_FEEL);

            feelRepository.delete(feel.get());
            return DefaultRes.res(StatusCode.OK, ResponseMessage.DELETE_FEEL);
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
