package com.streetask.app.moderation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.streetask.app.auth.payload.response.MessageResponse;
import com.streetask.app.model.Strike;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/moderation/users")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Validated
public class ModerationController {

    private final ModerationService moderationService;

    @PostMapping("/{userId}/strike")
    public ResponseEntity<MessageResponse> sendStrike(@PathVariable UUID userId, @Valid @RequestBody StrikeRequest body) {
        Strike strike = moderationService.issueStrike(userId, body.getReason(), body.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Strike sent successfully. Strike ID: " + strike.getId()));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<MessageResponse> deleteUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "false") boolean confirm) {
        
        if (!confirm) {
            return ResponseEntity.badRequest().body(new MessageResponse("Deletion requires confirmation. Use confirm=true."));
        }

        moderationService.deleteRegularUser(userId);
        return ResponseEntity.ok(new MessageResponse("User account deleted successfully."));
    }

    @GetMapping("/{userId}/strike-count")
    public ResponseEntity<Map<String, Long>> getStrikeCount(@PathVariable UUID userId) {
        long count = moderationService.getStrikeCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/{userId}/strikes")
    public ResponseEntity<List<Strike>> getUserStrikes(@PathVariable UUID userId) {
        List<Strike> strikes = moderationService.getUserStrikes(userId);
        return ResponseEntity.ok(strikes);
    }
}
