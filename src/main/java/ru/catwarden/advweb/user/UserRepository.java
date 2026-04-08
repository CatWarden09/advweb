    package ru.catwarden.advweb.user;

    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Modifying;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;

    import java.util.Optional;

    public interface UserRepository extends JpaRepository<User, Long> {
        Optional<User> findByKeycloakId(String keycloakId);
        Optional<User> findByEmail(String email);
        Optional<User> findByPhone(String phone);

        @Modifying
        @Query("UPDATE User u SET u.rating = :rating, u.ratingCount = :ratingCount WHERE u.id = :userId")
        void updateUserRatingStats (@Param("userId") Long userId,
                                   @Param("rating") Double rating,
                                   @Param("ratingCount") Long ratingCount);


        @Modifying
        @Query("UPDATE User u SET u.totalEarned = COALESCE(u.totalEarned, 0) + :newPrice WHERE u.id = :userId")
        void  updateUserTotalEarned (@Param("userId") Long userId, @Param("newPrice") Double newPrice);
    }
