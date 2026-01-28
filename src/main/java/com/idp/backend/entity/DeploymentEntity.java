package com.idp.backend.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_deployments",
indexes = @Index(name = "idx_deploy_service_time", columnList = "serviceId,deployedAt"))
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeploymentEntity {

    @Id
    @GeneratedValue
    private UUID deployId;

    @Column(nullable = false)
    private UUID serviceId;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private String env;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant deployedAt =Instant.now();

    private String triggeredBy;
}
