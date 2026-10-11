package commerce.novamart.review.service;

import commerce.novamart.catalog.entity.Product;
import commerce.novamart.catalog.repository.ProductRepository;
import commerce.novamart.common.exception.BadRequestException;
import commerce.novamart.common.exception.ForbiddenException;
import commerce.novamart.common.exception.ResourceNotFoundException;
import commerce.novamart.common.util.Dates;
import commerce.novamart.order.repository.OrderRepository;
import commerce.novamart.review.dto.ReviewDtos.AnswerResponse;
import commerce.novamart.review.dto.ReviewDtos.CreateReviewRequest;
import commerce.novamart.review.dto.ReviewDtos.QuestionResponse;
import commerce.novamart.review.dto.ReviewDtos.ReviewResponse;
import commerce.novamart.review.entity.ProductQuestion;
import commerce.novamart.review.entity.QuestionAnswer;
import commerce.novamart.review.entity.Review;
import commerce.novamart.review.repository.AnswerRepository;
import commerce.novamart.review.repository.QuestionRepository;
import commerce.novamart.review.repository.ReviewRepository;
import commerce.novamart.user.entity.Role;
import commerce.novamart.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviews;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final ProductRepository products;
    private final OrderRepository orders;

    // ----- reviews ----------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ReviewResponse> forProduct(UUID productId) {
        return reviews.findApprovedForProduct(productId).stream().map(ReviewService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> mine(User user) {
        return reviews.findByUser(user.getId()).stream().map(ReviewService::toResponse).toList();
    }

    public ReviewResponse create(User user, UUID productId, CreateReviewRequest req) {
        Product product = products.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (reviews.existsByProductIdAndUserId(productId, user.getId())) {
            throw new BadRequestException("You've already reviewed this product");
        }

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setUserName(displayName(user));
        review.setRating(req.rating());
        review.setTitle(req.title().trim());
        review.setBody(req.body().trim());
        review.setImages(req.images() == null ? List.of() : List.copyOf(req.images()));
        review.setVerifiedPurchase(orders.hasPurchased(user.getId(), productId));
        review.setStatus(Review.Status.APPROVED);
        reviews.save(review);

        adjustRating(product, req.rating(), +1);
        return toResponse(review);
    }

    public void deleteOwn(User user, UUID reviewId) {
        Review review = reviews.findById(reviewId).orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (review.getUser() == null || !review.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You can only delete your own reviews");
        }
        if (review.getStatus() == Review.Status.APPROVED) adjustRating(review.getProduct(), review.getRating(), -1);
        reviews.delete(review);
    }

    public void markHelpful(UUID reviewId) {
        if (reviews.incrementHelpful(reviewId) == 0) throw new ResourceNotFoundException("Review not found");
    }

    // ----- admin moderation -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public commerce.novamart.common.dto.PagedResult<ReviewResponse> adminList(Review.Status status, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100),
                org.springframework.data.domain.Sort.by("createdAt").descending());
        var result = status == null ? reviews.findAll(pageable) : reviews.findByStatus(status, pageable);
        return commerce.novamart.common.dto.PagedResult.of(result, ReviewService::toResponse);
    }

    public ReviewResponse setStatus(UUID reviewId, Review.Status status) {
        Review review = reviews.findById(reviewId).orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (review.getStatus() == status) return toResponse(review);
        boolean wasCounted = review.getStatus() == Review.Status.APPROVED;
        boolean willCount = status == Review.Status.APPROVED;
        if (wasCounted && !willCount) adjustRating(review.getProduct(), review.getRating(), -1);
        if (!wasCounted && willCount) adjustRating(review.getProduct(), review.getRating(), +1);
        review.setStatus(status);
        return toResponse(review);
    }

    public void adminDelete(UUID reviewId) {
        Review review = reviews.findById(reviewId).orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (review.getStatus() == Review.Status.APPROVED) adjustRating(review.getProduct(), review.getRating(), -1);
        reviews.delete(review);
    }

    // ----- Q&A --------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<QuestionResponse> questionsFor(UUID productId) {
        return questions.findByProductIdOrderByCreatedAtDesc(productId).stream().map(ReviewService::toResponse).toList();
    }

    public QuestionResponse ask(User user, UUID productId, String text) {
        Product product = products.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        ProductQuestion q = new ProductQuestion();
        q.setProduct(product);
        q.setUser(user);
        q.setAskedBy(displayName(user));
        q.setQuestion(text.trim());
        return toResponse(questions.save(q));
    }

    public QuestionResponse answer(User user, UUID questionId, String body) {
        ProductQuestion q = questions.findById(questionId).orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        boolean staff = user.getRole() == Role.ADMIN || user.getRole() == Role.SUPPORT;
        QuestionAnswer a = new QuestionAnswer();
        a.setQuestion(q);
        a.setUser(user);
        a.setStore(staff);
        a.setAnsweredBy(staff ? "NovaMart Support" : displayName(user));
        a.setBody(body.trim());
        q.getAnswers().add(a);
        questions.save(q);
        return toResponse(q);
    }

    public void markAnswerHelpful(UUID answerId) {
        if (answers.incrementHelpful(answerId) == 0) throw new ResourceNotFoundException("Answer not found");
    }

    // ----- helpers ----------------------------------------------------------------------------------

    /** Running average, so a seeded baseline (e.g. 4.8 from 2,310 reviews) is preserved rather than reset. */
    private void adjustRating(Product product, int rating, int direction) {
        int count = product.getReviewCount();
        BigDecimal sum = product.getRating().multiply(BigDecimal.valueOf(count)).add(BigDecimal.valueOf((long) rating * direction));
        int newCount = Math.max(0, count + direction);
        product.setReviewCount(newCount);
        product.setRating(newCount == 0 ? BigDecimal.ZERO
                : sum.divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP).max(BigDecimal.ZERO).min(BigDecimal.valueOf(5)));
    }

    private static String displayName(User u) {
        String last = u.getLastName() == null || u.getLastName().isBlank() ? "" : " " + Character.toUpperCase(u.getLastName().charAt(0)) + ".";
        return u.getFirstName() + last;
    }

    static ReviewResponse toResponse(Review r) {
        return new ReviewResponse(r.getId().toString(), r.getProduct().getId().toString(), r.getUserName(), null,
                r.getRating(), r.getTitle(), r.getBody(), r.getImages(), r.isVerifiedPurchase(), r.getHelpfulCount(),
                Dates.iso(r.getCreatedAt()), r.getStatus().name().toLowerCase(), r.getProduct().getName(), r.getProduct().getSlug(), r.getProduct().getThumbnail());
    }

    static QuestionResponse toResponse(ProductQuestion q) {
        List<AnswerResponse> a = q.getAnswers().stream()
                .map(x -> new AnswerResponse(x.getId().toString(), x.getBody(), x.getAnsweredBy(), x.isStore(),
                        Dates.iso(x.getCreatedAt()), x.getHelpfulCount()))
                .toList();
        return new QuestionResponse(q.getId().toString(), q.getProduct().getId().toString(), q.getQuestion(),
                q.getAskedBy(), Dates.iso(q.getCreatedAt()), a);
    }
}
