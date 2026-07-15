package com.infinitytower.ecs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infinitytower.ecs.domain.ReportRecordEntity;
import com.infinitytower.ecs.repository.ReportRecordRepository;
import com.infinitytower.ecs.service.SimulationService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final SimulationService service;
    private final ReportRecordRepository reportRecordRepository;
    private final ObjectMapper objectMapper;

    public ReportController(SimulationService service, ReportRecordRepository reportRecordRepository, ObjectMapper objectMapper) {
        this.service = service;
        this.reportRecordRepository = reportRecordRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<?> generate(@RequestParam(defaultValue = "simulation") String type,
                                       @RequestParam(defaultValue = "json") String format) throws Exception {
        Map<String, Object> report = service.buildReport(type);
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);

        ReportRecordEntity record = new ReportRecordEntity(type, String.valueOf(report.get("buildingName")),
                String.valueOf(report.get("algorithmUsed")), json);
        reportRecordRepository.save(record);

        if ("csv".equalsIgnoreCase(format)) {
            String csv = toCsv(report);
            return fileResponse(csv, type + "-report.csv", "text/csv");
        }
        return fileResponse(json, type + "-report.json", "application/json");
    }

    private ResponseEntity<byte[]> fileResponse(String content, String filename, String mime) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).contentType(MediaType.parseMediaType(mime))
                .body(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String toCsv(Map<String, Object> report) {
        StringBuilder sb = new StringBuilder("metric,value\n");
        flatten("", report, sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private void flatten(String prefix, Object value, StringBuilder sb) {
        if (value instanceof Map) {
            ((Map<String, Object>) value).forEach((k, v) -> flatten(prefix.isEmpty() ? k : prefix + "." + k, v, sb));
        } else if (value instanceof java.util.List) {
            sb.append('"').append(prefix).append("\",\"").append(String.valueOf(value).replace("\"", "\"\"")).append("\"\n");
        } else {
            sb.append('"').append(prefix).append("\",\"").append(String.valueOf(value).replace("\"", "\"\"")).append("\"\n");
        }
    }
}
