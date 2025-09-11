package com.atharva.question_service.Service;

import com.atharva.question_service.Entity.Question;
import com.atharva.question_service.dao.QuestionDao;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;


@Service
public class GeminiAIService {

    private final WebClient webClient;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Autowired
    QuestionDao questionDao;


    public GeminiAIService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
    }

    @Transactional
    public List<Question> generateAIQuestions(String categoryName, Integer numQuestions) {
        String endpoint = "/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

        String prompt = """
            Generate a JSON of format:
            {
              "questionTitle": "",
              "category": "",
              "option1": "",
              "option2": "",
              "option3": "",
              "option4": "",
              "rightAnswer": "",
              "difficultyLevel": ""
            }
            in context with categoryname as %s and %d number of questions like above JSON.
            Category should be the categoryname context same for all questions.
            Difficulty should be Easy,Medium or Hard.
            Straight to point response, no extra content.
            The response should be an array of JSON objects.
            """.formatted(categoryName, numQuestions);

        String requestBody = """
            {
              "contents": [
                {
                  "role": "user",
                  "parts": [
                    {
                      "text": "%s"
                    }
                  ]
                }
              ]
            }
            """.formatted(prompt.replace("\"", "\\\"").replace("\n", "\\n"));

        String response = webClient.post()
                .uri(endpoint)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            String text = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            // Extract JSON array from text
            int startIndex = text.indexOf("[");
            int endIndex = text.lastIndexOf("]");
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                String jsonArrayString = text.substring(startIndex, endIndex + 1);

                List<Question> questions = mapper.readValue(jsonArrayString, new TypeReference<>() {});

                return questionDao.saveAll(questions);
            } else {
                throw new RuntimeException("JSON array not found in the response text.");
            }

        } catch (Exception e) {
            throw new RuntimeException("Error parsing or saving response: " + e.getMessage(), e);
        }
    }
}

