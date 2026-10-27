package com.construction.management.project.repository;
import com.construction.management.project.domain.DailyLogLaborEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DailyLogLaborEntryRepository extends JpaRepository<DailyLogLaborEntry, Long> { List<DailyLogLaborEntry> findByDailyLogIdOrderBySortOrderAsc(String dailyLogId); void deleteByDailyLogId(String dailyLogId); }
