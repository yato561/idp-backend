package com.idp.backend.util;

import com.idp.backend.dao.UserDao;
import com.idp.backend.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {

    @Autowired
    private UserDao userDao;
    private SecurityUtil(){}

    public static String currentUsername(){
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }
    public static boolean hasRole(String role){
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_"+role));
    }
    public String currentUserTeam(){
        String username=currentUsername();
        UserEntity user= userDao.findByUsername(username);
        if (user.getTeam() == null) {
            throw new IllegalStateException("User has no team assigned");
        }
        return user.getTeam();
    }
}
