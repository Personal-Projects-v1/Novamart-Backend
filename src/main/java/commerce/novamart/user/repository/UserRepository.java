package commerce.novamart.user.repository;

import commerce.novamart.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    long countByRole(commerce.novamart.user.entity.Role role);

    long countByRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            commerce.novamart.user.entity.Role role, java.time.LocalDateTime from, java.time.LocalDateTime to);

    org.springframework.data.domain.Page<User> findByRole(
            commerce.novamart.user.entity.Role role, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("select u from User u where u.role = commerce.novamart.user.entity.Role.CUSTOMER and (lower(u.email) like :q or lower(concat(u.firstName, ' ', u.lastName)) like :q)")
    org.springframework.data.domain.Page<User> searchCustomers(
            @org.springframework.data.repository.query.Param("q") String q, org.springframework.data.domain.Pageable pageable);
}
