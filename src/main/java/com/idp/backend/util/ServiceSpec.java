package com.idp.backend.util;

import com.idp.backend.entity.ServiceCatInfo;
import org.springframework.data.jpa.domain.Specification;

public class ServiceSpec {

    public static Specification<ServiceCatInfo> hasRuntime(String runtime){
        return (root,query,cb) ->
                runtime==null ? null: cb.equal(root.get("runTime"),runtime);
    }

    public static Specification<ServiceCatInfo> hasStatus(String status){
        return(root,query,cb) ->
                status==null ? null : cb.equal(root.get("status"),status);
    }

    public static Specification<ServiceCatInfo> hasOwnerTeam(String ownerTeam){
        return (root,query,cb) ->
                ownerTeam == null ? null : cb.equal(root.get("ownerTeam"),ownerTeam);
    }
}
