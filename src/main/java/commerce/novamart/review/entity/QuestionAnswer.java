package commerce.novamart.review.entity;

import commerce.novamart.common.entity.BaseEntity;
import commerce.novamart.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "question_answers")
@Getter
@Setter
@NoArgsConstructor
public class QuestionAnswer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private ProductQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 160)
    private String answeredBy;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "is_store", nullable = false)
    private boolean store;

    @Column(nullable = false)
    private int helpfulCount;
}
