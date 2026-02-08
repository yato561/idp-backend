package com.idp.backend.security;


import com.idp.backend.dao.UserDao;
import com.idp.backend.entity.ServiceCatInfo;
import com.idp.backend.entity.UserEntity;
import com.idp.backend.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.AccessDeniedException;

@Component
public class OwnershipGuard {

    @Autowired
    private UserDao userDao;

    public void assertCanModify(ServiceCatInfo serviceCatInfo) throws AccessDeniedException {
        if (SecurityUtil.isAdmin()){
            return;
        }

        String username= SecurityUtil.currentUsername();
        UserEntity user= userDao.findByUsername(username);

        String userTeam = normalize(user.getTeam());
        String ownerTeam = normalize(serviceCatInfo.getOwnerTeam());

        if(!userTeam.equals(ownerTeam)){
            throw new AccessDeniedException(
                    "You do not have permission to modify this service"
            );
        }
    }
    private String normalize(String value){
        return value== null ? "" : value.trim().toLowerCase();
    }
}
