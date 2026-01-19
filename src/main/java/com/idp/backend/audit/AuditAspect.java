package com.idp.backend.audit;


import com.idp.backend.entity.AuditLog;
import com.idp.backend.repo.AuditLogRepo;
import com.idp.backend.util.SecurityUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Aspect
@Component
public class AuditAspect {

    private final AuditLogRepo repo;

    public AuditAspect(AuditLogRepo repo){
        this.repo=repo;
    }

    @AfterReturning(pointcut = "@annotation(auditable)",
    returning = "result")
    public void logSuccess(JoinPoint jp, Auditable auditable,Object result){
        persist(auditable,jp, "SUCCESS", null);
    }

    @AfterThrowing(pointcut = "@annotation(auditable)", throwing = "ex")
    public void logFailure(JoinPoint jp, Auditable auditable, Exception ex){
        persist(auditable,jp, "FAILURE",ex.getMessage());
    }

    private void persist(Auditable auditable, JoinPoint jp, String outcome, String error){
        AuditLog log= new AuditLog();
        log.setActor(SecurityUtil.currentUsername());
        log.setAction(auditable.action());
        log.setResource(auditable.resource());
        log.setOutcome(outcome);
        log.setError(error);
        log.setTimestamp(Instant.now());

        if(!auditable.resourceIdParam().isBlank()){
            String id=extractResourceId(jp,auditable.resourceIdParam());
            log.setResourceId(id);
        }
        repo.save(log);
    }
    private String extractResourceId(JoinPoint jp, String paramName){
        var sig=(org.aspectj.lang.reflect.MethodSignature) jp.getSignature();
        String [] names= sig.getParameterNames();
        Object[] values= jp.getArgs();

        for(int i=0;i< names.length;i++){
            if(names[i].equals(paramName)){
                return String.valueOf(values[i]);
            }
        }
        return null;
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
