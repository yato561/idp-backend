package com.idp.backend.config;

import com.idp.backend.dao.UserDao;
import com.idp.backend.util.SecurityUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class SecurityBootStrap {

    private final UserDao userDao;

    public SecurityBootStrap(UserDao userDao){
        this.userDao=userDao;
    }

    @PostConstruct
    public void init(){
        SecurityUtil.init(userDao);
    }
}
