package com.idp.backend.util;

import com.idp.backend.dao.UserDao;
import com.idp.backend.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public  class SecurityUtil {

    @Autowired
    private UserDao userDao;
    private SecurityUtil(){}

    public  String currentUsername(){
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }
    public  boolean hasRole(String role){
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
        return user.getTeam().trim().toLowerCase();
    }
    public static boolean isAdmin() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

}
