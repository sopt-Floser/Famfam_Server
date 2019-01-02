package kr.co.famfam.server.service.impl;

import kr.co.famfam.server.domain.User;
import kr.co.famfam.server.model.DefaultRes;
import kr.co.famfam.server.model.LoginReq;
import kr.co.famfam.server.repository.UserRepository;
import kr.co.famfam.server.service.JwtService;
import kr.co.famfam.server.service.LoginService;
import kr.co.famfam.server.utils.ResponseMessage;
import kr.co.famfam.server.utils.StatusCode;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LoginServiceImpl implements LoginService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public LoginServiceImpl(UserRepository userRepository, JwtService jwtService) {

        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    /***
     *
     * @param loginReq
     * @return
     */
    public DefaultRes login(LoginReq loginReq) {
        if (loginReq.isLogin()) {
            User loginUser = new User(loginReq);

            final Optional<User> user = userRepository.findUserByUserIdAndUserPw(loginUser.getUserId(), loginUser.getUserPw());

            if (user != null) {
                final JwtService.TokenRes tokenRes = new JwtService.TokenRes(jwtService.create(user.get().getUserIdx()));
                return DefaultRes.res(StatusCode.OK, ResponseMessage.LOGIN_SUCCESS, tokenRes);
            }
        }
        return DefaultRes.res(StatusCode.BAD_REQUEST, ResponseMessage.LOGIN_FAIL);
    }
}

