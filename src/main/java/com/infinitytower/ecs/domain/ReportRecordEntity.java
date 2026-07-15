package com.infinitytower.ecs.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "report_record")
public class ReportRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_type")
    private String reportType;

    @Column(name = "building_name")
    private String buildingName;

    @Column(name = "algorithm_used")
    private String algorithmUsed;

    @Column(name = "payload_json", columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(name = "generated_at")
    private Instant generatedAt = Instant.now();

    public ReportRecordEntity() {}

    public ReportRecordEntity(String reportType, String buildingName, String algorithmUsed, String payloadJson) {
        this.reportType = reportType;
        this.buildingName = buildingName;
        this.algorithmUsed = algorithmUsed;
        this.payloadJson = payloadJson;
    }

    public Long getId() { return id; }
    public String getReportType() { return reportType; }
    public String getBuildingName() { return buildingName; }
    public String getAlgorithmUsed() { return algorithmUsed; }
    public String getPayloadJson() { return payloadJson; }
    public Instant getGeneratedAt() { return generatedAt; }
}
