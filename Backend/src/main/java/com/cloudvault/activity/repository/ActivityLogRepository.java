package com.cloudvault.activity.repository;

import com.cloudvault.domain.ActivityLog;
import com.cloudvault.domain.User;
import com.cloudvault.domain.enums.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    /** All activity for a specific user, newest first. */
    Page<ActivityLog> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /** Activity filtered by event type for a specific user. */
    Page<ActivityLog> findByUserAndEventTypeOrderByCreatedAtDesc(User user, EventType eventType, Pageable pageable);

    /** All activity on a specific file. */
    Page<ActivityLog> findByFileIdOrderByCreatedAtDesc(Long fileId, Pageable pageable);
}
