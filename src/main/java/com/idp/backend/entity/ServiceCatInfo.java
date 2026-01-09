package com.idp.backend.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name="services")
public class ServiceCatInfo {

    @Id
    @GeneratedValue
    private UUID serviceId;

    @Column(name = "service_name", nullable = false, unique = true)
    private String serviceName;

    @Column(name = "repo_url")
    private String repoUrl;

    @Column(name = "owner_team")
    private String ownerTeam;

    @Column(name = "runtime")
    private String runTime;

    @Column(nullable = false)
    private String status;

    private LocalDateTime createdAt;

}
