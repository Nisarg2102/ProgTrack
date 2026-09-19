package com.threespace.progtrack.repository;

import com.threespace.progtrack.entity.DailyTask;
import com.threespace.progtrack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyTaskRepository extends JpaRepository<DailyTask, Long> {

    List<DailyTask> findByUserAndTaskDate(User user, LocalDate taskDate);

    List<DailyTask> findByUserOrderByTaskDateDesc(User user);

    List<DailyTask> findByUserAndTaskDateBetweenOrderByTaskDateDesc(User user, LocalDate from, LocalDate to);

    List<DailyTask> findByUserAndSubmittedFalse(User user);

    @Query("SELECT dt FROM DailyTask dt WHERE dt.user.department = :dept AND dt.taskDate = :date ORDER BY dt.user.fullName")
    List<DailyTask> findByDepartmentAndDate(@Param("dept") String department, @Param("date") LocalDate date);

    @Query("SELECT dt FROM DailyTask dt WHERE dt.taskDate BETWEEN :from AND :to ORDER BY dt.taskDate DESC, dt.user.fullName")
    List<DailyTask> findAllByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT dt FROM DailyTask dt WHERE dt.user.department = :dept AND dt.taskDate BETWEEN :from AND :to")
    List<DailyTask> findByDepartmentAndDateRange(@Param("dept") String dept,
                                                  @Param("from") LocalDate from,
                                                  @Param("to") LocalDate to);

    @Query("SELECT SUM(dt.timeSpent) FROM DailyTask dt WHERE dt.user = :user AND dt.taskDate = :date")
    Double sumTimeSpentByUserAndDate(@Param("user") User user, @Param("date") LocalDate date);

    @Query("SELECT SUM(dt.timeSpent) FROM DailyTask dt WHERE dt.user = :user AND dt.taskDate BETWEEN :from AND :to")
    Double sumTimeSpentByUserAndDateRange(@Param("user") User user, @Param("from") LocalDate from, @Param("to") LocalDate to);

    boolean existsByUserAndTaskDateAndSubmitted(User user, LocalDate taskDate, Boolean submitted);

    long countByUserAndTaskDateBetween(User user, LocalDate from, LocalDate to);
}
