package com.idp.backend.util;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;


public final class SpecUtil<T> {

    private final List<Specification<T>> specs= new ArrayList<>();

    private SpecUtil(){}

    public static  <T> SpecUtil<T> of(){
        return new SpecUtil<>();
    }

    public SpecUtil<T> eq(String field, Object value){ //Equality filter
        if(value == null) return this;

        specs.add((root, query, cb) ->
                cb.equal(
                        cb.lower(root.get(field).as(String.class)),
                        value.toString().toLowerCase().trim()
                ));
        return this;
    }

    public SpecUtil<T> enforceIf(
            boolean condition, String field, Object value
    ){
        if(!condition || value== null ) return this;

        specs.add((root,query,cb)->
                cb.equal(
                        cb.lower(root.get(field).as(String.class)),
                        value.toString().toLowerCase().trim()
                ));
        return this;
    }

//    public SpecUtil<T> build(){
//        return (root,query,cb) ->{
//            List<Predicate> predicates= new ArrayList<>();
//            for (Specification<T> spec:specs){
//                predicates.add(spec.toPredicate(root,query,cb));
//            }
//            return cb.and(predicates.toArray(new Predicate[0]));
//        };
//    }

    public Specification<T> build(){
        return (root, query, cb) ->{
            List<Predicate> predicates = new ArrayList<>();

            for (Specification<T> spec:specs){
                Predicate p= spec.toPredicate(root,query,cb);
                if(p!=null){
                    predicates.add(p);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
