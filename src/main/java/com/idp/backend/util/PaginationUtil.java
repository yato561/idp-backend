package com.idp.backend.util;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public final class PaginationUtil {

    private PaginationUtil() {

    }

    public static <T, R extends JpaRepository<T,?> & JpaSpecificationExecutor<T>>Page<T> paginate(
            R repo,
            Specification<T> spec,
            Pageable pageable
    ){
        if(spec==null){
            return repo.findAll(pageable);
        }
        return repo.findAll(spec,pageable);
    }
}
