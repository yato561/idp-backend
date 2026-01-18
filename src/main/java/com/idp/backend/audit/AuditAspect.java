package com.idp.backend.audit;


import com.idp.backend.entity.AuditLog;
import com.idp.backend.repo.AuditLogRepo;
import com.idp.backend.util.SecurityUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

    private final AuditLogRepo repo;

    public AuditAspect(AuditLogRepo repo){
        this.repo=repo;
    }

    @AfterReturning("@annotation(auditable)")
    public void log(JoinPoint jp, Auditable auditable){

        AuditLog log= new AuditLog();
        log.setActor(SecurityUtil.currentUsername());
        log.setAction(auditable.action());
        log.setResource(auditable.resource());

        repo.save(log);
    }
}
