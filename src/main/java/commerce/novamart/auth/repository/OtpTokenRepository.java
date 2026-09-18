package commerce.novamart.auth.repository;

import commerce.novamart.auth.entity.OtpToken;
import commerce.novamart.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {
    List<OtpToken> findByUserAndTypeAndVerifiedAtIsNull(User user, OtpToken.OtpType type);
    
    @Modifying
    @Query("DELETE FROM OtpToken ot WHERE ot.user = :user AND ot.type = :type")
    void deleteByUserAndType(User user, OtpToken.OtpType type);
    
    @Modifying
    @Query("DELETE FROM OtpToken ot WHERE ot.expiresAt < :now OR ot.verifiedAt IS NOT NULL")
    void deleteExpiredAndVerifiedTokens(LocalDateTime now);
}
