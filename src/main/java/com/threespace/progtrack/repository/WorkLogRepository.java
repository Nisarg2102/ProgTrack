package com.threespace.progtrack.repository;

import com.threespace.progtrack.entity.WorkLog;
import com.threespace.progtrack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {

    Optional<WorkLog> findBySessionIdAndLogoutDateTimeIsNull(String sessionId);

    List<WorkLog> findByUserOrderByLoginDateTimeDesc(User user);

    List<WorkLog> findByUserAndLoginDateTimeBetween(User user, LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(wl.totalSeconds), 0) FROM WorkLog wl WHERE wl.user = :user AND wl.loginDateTime >= :from AND wl.loginDateTime < :to")
    Long sumTotalSecondsByUserAndDateRange(@Param("user") User user,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    @Query("SELECT wl FROM WorkLog wl WHERE wl.user.department = :dept AND wl.loginDateTime >= :from ORDER BY wl.loginDateTime DESC")
    List<WorkLog> findByDepartmentAndDateFrom(@Param("dept") String dept, @Param("from") LocalDateTime from);

    Optional<WorkLog> findTopByUserOrderByLoginDateTimeDesc(User user);
}
