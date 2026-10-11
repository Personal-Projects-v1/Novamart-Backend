package commerce.novamart.review.controller;

import commerce.novamart.common.dto.ApiResponse;
import commerce.novamart.common.security.CurrentUser;
import commerce.novamart.review.dto.ReviewDtos.AnswerRequest;
import commerce.novamart.review.dto.ReviewDtos.AskQuestionRequest;
import commerce.novamart.review.dto.ReviewDtos.CreateReviewRequest;
import commerce.novamart.review.dto.ReviewDtos.QuestionResponse;
import commerce.novamart.review.dto.ReviewDtos.ReviewResponse;
import commerce.novamart.review.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Reviews & Q&A")
public class ReviewController {

    private final ReviewService service;
    private final CurrentUser currentUser;

    @GetMapping("/products/{productId}/reviews")
    public ApiResponse<List<ReviewResponse>> reviews(@PathVariable UUID productId) {
        return ApiResponse.success(service.forProduct(productId));
    }

    @PostMapping("/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(@PathVariable UUID productId,
                                                              @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted", service.create(currentUser.require(), productId, request)));
    }

    @GetMapping("/users/me/reviews")
    public ApiResponse<List<ReviewResponse>> mine() {
        return ApiResponse.success(service.mine(currentUser.require()));
    }

    @DeleteMapping("/reviews/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.deleteOwn(currentUser.require(), id);
        return ApiResponse.success(null);
    }

    @PostMapping("/reviews/{id}/helpful")
    public ApiResponse<Void> helpful(@PathVariable UUID id) {
        service.markHelpful(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/products/{productId}/questions")
    public ApiResponse<List<QuestionResponse>> questions(@PathVariable UUID productId) {
        return ApiResponse.success(service.questionsFor(productId));
    }

    @PostMapping("/products/{productId}/questions")
    public ResponseEntity<ApiResponse<QuestionResponse>> ask(@PathVariable UUID productId,
                                                             @Valid @RequestBody AskQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.ask(currentUser.require(), productId, request.question())));
    }

    @PostMapping("/questions/{id}/answers")
    public ApiResponse<QuestionResponse> answer(@PathVariable UUID id, @Valid @RequestBody AnswerRequest request) {
        return ApiResponse.success(service.answer(currentUser.require(), id, request.body()));
    }

    @PostMapping("/answers/{id}/helpful")
    public ApiResponse<Void> answerHelpful(@PathVariable UUID id) {
        service.markAnswerHelpful(id);
        return ApiResponse.success(null);
    }
}
