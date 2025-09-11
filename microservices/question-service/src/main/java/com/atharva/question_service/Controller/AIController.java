package com.atharva.question_service.Controller;

import com.atharva.question_service.Entity.Question;
import com.atharva.question_service.Service.GeminiAIService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final GeminiAIService geminiService;

    public AIController(GeminiAIService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/gemini")
    public List<Question> askGemini(@RequestBody Map<String, String> body) {
        return geminiService.generateAIQuestions(body.get("categoryName"),Integer.parseInt(body.get("numQuestions")));
    }

}
