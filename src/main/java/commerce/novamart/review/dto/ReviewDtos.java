package commerce.novamart.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class ReviewDtos {

    private ReviewDtos() {
    }

    /** Matches the frontend {@code Review} type. */
    public record ReviewResponse(
            String id, String productId, String userName, String userAvatar, int rating, String title, String body,
            List<String> images, boolean verifiedPurchase, int helpfulCount, String createdAt,
            String status, String productName, String productSlug, String productImage) {
    }

    public record CreateReviewRequest(
            @Min(value = 1, message = "Rating must be between 1 and 5") @Max(value = 5, message = "Rating must be between 1 and 5") int rating,
            @NotBlank(message = "Title is required") @Size(max = 200) String title,
            @NotBlank(message = "Review text is required") @Size(max = 5000) String body,
            @Size(max = 6) List<String> images) {
    }

    public record AnswerResponse(
            String id, String body, String answeredBy, @JsonProperty("isStore") boolean isStore, String answeredAt,
            int helpfulCount) {
    }

    /** Matches the frontend {@code ProductQuestion} type. */
    public record QuestionResponse(
            String id, String productId, String question, String askedBy, String askedAt, List<AnswerResponse> answers) {
    }

    public record AskQuestionRequest(@NotBlank(message = "Question is required") @Size(max = 1000) String question) {
    }

    public record AnswerRequest(@NotBlank(message = "Answer is required") @Size(max = 3000) String body) {
    }
}
