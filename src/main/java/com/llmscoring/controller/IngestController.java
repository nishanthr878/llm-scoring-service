package com.llmscoring.controller;

import com.llmscoring.dto.IngestRequest;
import com.llmscoring.model.ScoringResult;
import com.llmscoring.service.IngestService;
import com.llmscoring.transformer.TransformerRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class IngestController {

    private final IngestService ingestService;
    private final TransformerRegistry transformerRegistry;

    @PostMapping("/ingest")
    public ResponseEntity<Object> ingest(
            @Valid @RequestBody IngestRequest request,
            @RequestParam(defaultValue = "false") boolean sync) {

        if (sync) {
            ScoringResult result = ingestService.ingestSync(request);
            return ResponseEntity.ok(result);
        }

        String eventId = ingestService.ingest(request);
        return ResponseEntity.accepted().body(Map.of(
                "eventId", eventId,
                "status", "accepted",
                "message", "Event queued for scoring"
        ));
    }

    @GetMapping("/formats")
    public ResponseEntity<List<String>> getAvailableFormats() {
        return ResponseEntity.ok(transformerRegistry.getAvailableFormats());
    }
}
