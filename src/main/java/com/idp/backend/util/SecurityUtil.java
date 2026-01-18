package com.idp.backend.util;

import com.idp.backend.dao.UserDao;
import com.idp.backend.entity.ServiceCatInfo;
import com.idp.backend.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


public  class SecurityUtil {

    private SecurityUtil(){}


    private static UserDao userDao;

    public static void init(UserDao dao){
        userDao= dao;
    }
    public static String currentUsername(){
        Authentication auth= SecurityContextHolder.getContext()
                .getAuthentication();
        return auth!=null ? auth.getName() : null;
    }
    public static boolean hasRole(String role){
        Authentication auth=SecurityContextHolder.getContext()
                .getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_"+role));
    }
    public static String currentUserTeam(){
        String username=currentUsername();
        UserEntity user= userDao.findByUsername(username);
        if (user.getTeam() == null) {
            throw new IllegalStateException("User has no team assigned");
        }
        return user.getTeam().trim().toLowerCase();
    }
    public static boolean isAdmin() {
        Authentication auth =  SecurityContextHolder.getContext()
                .getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
    }
    public static Specification<ServiceCatInfo> hasRuntime(String runtime) {
        return (root, query, cb) -> {
            if (runtime == null || runtime.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(
                    cb.lower(cb.trim(root.get("runTime"))),
                    runtime.toLowerCase()
            );
        };
    }

}
