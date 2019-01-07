package kr.co.famfam.server.service.impl;

import kr.co.famfam.server.domain.Mission;
import kr.co.famfam.server.domain.User;
import kr.co.famfam.server.model.DefaultRes;
import kr.co.famfam.server.repository.MissionRepository;
import kr.co.famfam.server.repository.UserRepository;
import kr.co.famfam.server.service.MissionService;
import kr.co.famfam.server.utils.ResponseMessage;
import kr.co.famfam.server.utils.StatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.*;

/**
 * Created by ehay@naver.com on 2019-01-07
 * Blog : http://ehay.tistory.com
 * Github : http://github.com/ehayand
 */

@Slf4j
@Service
public class MissionServiceImpl implements MissionService {

    private final UserRepository userRepository;
    private final MissionRepository missionRepository;

    public MissionServiceImpl(UserRepository userRepository, MissionRepository missionRepository) {
        this.userRepository = userRepository;
        this.missionRepository = missionRepository;
    }

    public DefaultRes findById(int userIdx) {
        Optional<User> user = userRepository.findById(userIdx);
        if (!user.isPresent())
            return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NOT_FOUND_USER);

        Optional<Mission> mission = missionRepository.findById(user.get().getMissionIdx());
        if (!mission.isPresent())
            return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NOT_FOUND_MISSION);

        Optional<User> target = userRepository.findById(user.get().getMissionTargetUserIdx());
        if (!target.isPresent())
            return DefaultRes.res(StatusCode.NOT_FOUND, ResponseMessage.NOT_FOUND_USER);

        Map<String, Object> result = new HashMap<>();
        result.put("mission", mission);
        result.put("targetUser", target);

        return DefaultRes.res(StatusCode.OK, ResponseMessage.READ_MISSION, result);
    }

    @Transactional
    public Boolean updateUser(final User user) {

        List<Mission> missions = missionRepository.findAll();
        if (missions.isEmpty())
            return false;

        List<User> users = userRepository.findUsersByGroupIdxAndUserIdxIsNotIn(user.getGroupIdx(), user.getUserIdx());
        if (users.isEmpty())
            return false;

        Mission mission = missions.get(new Random().nextInt(missions.size()));
        User target = users.get(new Random().nextInt(users.size()));

        user.setMissionIdx(mission.getMissionIdx());
        user.setMissionTargetUserIdx(target.getUserIdx());

        try {
            userRepository.save(user);
            return true;
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error(e.getMessage());
            return false;
        }
    }

    @Transactional
    public Boolean save(Mission mission) {
        try {
            missionRepository.save(mission);

            return true;
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error(e.getMessage());
        }

        return false;
    }
}