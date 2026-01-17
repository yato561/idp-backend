package com.idp.backend.util;

import com.idp.backend.entity.ServiceCatInfo;
import org.springframework.data.jpa.domain.Specification;

public class ServiceSpec {

    public static Specification<ServiceCatInfo> hasRuntime(String runtime) {
        return (root, query, cb) -> {
            if (runtime == null || runtime.isBlank()) {
                return cb.conjunction(); // ALWAYS TRUE
            }
            return cb.equal(root.get("runtime"), runtime);
        };
    }

    public static Specification<ServiceCatInfo> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }


    public static Specification<ServiceCatInfo> hasOwnerTeam(String ownerTeam){
        return (root, query, cb) -> {
            if (ownerTeam == null || ownerTeam.isBlank()) {
                throw new IllegalStateException("Owner team cannot be null");
            }
            return cb.equal( cb.lower(cb.trim(root.get("ownerTeam"))), ownerTeam);
        };
    }
}
