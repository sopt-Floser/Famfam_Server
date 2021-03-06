package kr.co.famfam.server.service.impl;

import kr.co.famfam.server.domain.History;
import kr.co.famfam.server.domain.User;
import kr.co.famfam.server.model.DefaultRes;
import kr.co.famfam.server.model.HistoryDto;
import kr.co.famfam.server.repository.HistoryRepository;
import kr.co.famfam.server.repository.UserRepository;
import kr.co.famfam.server.service.HistoryService;
import kr.co.famfam.server.utils.HistoryType;
import kr.co.famfam.server.utils.ResponseMessage;
import kr.co.famfam.server.utils.StatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.*;


@Slf4j
@Service
public class HistoryServiceImpl implements HistoryService {

    private HistoryRepository historyRepository;
    private UserRepository userRepository;

    public HistoryServiceImpl(HistoryRepository historyRepository, UserRepository userRepository) {
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Boolean add(final HistoryDto historyDto) {
        try {
            int userIdx = historyDto.getUserIdx();

            Optional<User> user = userRepository.findById(userIdx);
            if (!user.isPresent())
                return false;

            String userName = user.get().getUserName();
            int groupIdx = user.get().getGroupIdx();

            StringBuilder stb = new StringBuilder();
            stb.append(userName);

            switch (historyDto.getHistoryType()) {
                case HistoryType.ADD_SCHEDULE:
                    stb.append(" 님이 일정을 추가했습니다.");
                    break;
                case HistoryType.ADD_EMOTION:
                    stb.append(" 님이 감정을 표현했습니다.");
                    break;
                case HistoryType.ADD_CONTENT:
                    stb.append(" 님이 글을 추가했습니다.");
                    break;
                case HistoryType.ADD_COMMENT:
                    stb.append(" 님이 댓글을 추가했습니다.");
                    break;
                case HistoryType.ADD_ANNIVERSARY:
                    stb.append(" 님이 기념일을 추가했습니다.");
                    break;
            }

            historyDto.setGroupIdx(groupIdx);
            historyDto.setContent(stb.toString());

            History history = historyDto.toEntity();

            historyRepository.save(history);

            return true;
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error(e.getMessage());
            return false;
        }
    }

    @Override
    public Boolean batchHistory(final HistoryDto historyDto, String content) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("\"").append(content).append("\"");

            switch (historyDto.getHistoryType()) {
                case HistoryType.ADD_FAMILYCALENDAR_PUSH:
                    sb.append(" -7일전입니다.");
                    break;
                case HistoryType.ADD_ANNIVERSARY_PUSH:
                    sb.append(" -7일전입니다.");
                    break;
            }

            historyDto.setContent(sb.toString());

            History history = historyDto.toEntity();

            historyRepository.save(history);

            return true;
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error(e.getMessage());
            return false;
        }
    }


    @Override
    public DefaultRes findAllHistoryByUserIdx(int userIdx, Pageable pageable) {
        try {
            Optional<User> user = userRepository.findById(userIdx);
            if (!user.isPresent())
                return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NOT_FOUND_USER);

            int groupIdx = user.get().getGroupIdx();

            Page<History> historyPage = historyRepository.findAllByGroupIdxAndUserIdxIsNotIn(groupIdx, userIdx, pageable);
            if (historyPage.isEmpty())
                return DefaultRes.res(StatusCode.NO_CONTENT, ResponseMessage.NOT_FOUND_HISTORY);

            Map<String, Object> result = new HashMap<>();
            List<History> histories = new LinkedList<>();

            for (History history : historyPage)
                histories.add(history);


            result.put("histories", histories);
            result.put("totalPage", historyPage.getTotalPages());

            return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_HISTORY, result);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error(e.getMessage());
            return DefaultRes.res(StatusCode.DB_ERROR, ResponseMessage.DB_ERROR);
        }
    }
}
