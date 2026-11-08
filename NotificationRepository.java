package com.smarthr.repository;

import com.smarthr.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop50ByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    List<Notification> findTop5ByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientUsernameIgnoreCaseAndReadFlagFalse(String username);

    @Modifying
    @Query("update Notification n set n.readFlag = true where n.recipient.id = :uid and n.readFlag = false")
    int markAllRead(@Param("uid") Long userId);
}
