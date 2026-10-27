package com.construction.management.project.repository;
import com.construction.management.project.domain.DailyLog;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DailyLogRepository extends JpaRepository<DailyLog, String> {
	Optional<DailyLog> findByProjectIdAndLogDate(String projectId, LocalDate logDate);
	List<DailyLog> findByProjectIdAndLogDateBetweenOrderByLogDateDesc(String projectId, LocalDate from, LocalDate to);
	List<DailyLog> findByProjectIdOrderByLogDateDesc(String projectId);
	void deleteByProjectId(String projectId);
}
