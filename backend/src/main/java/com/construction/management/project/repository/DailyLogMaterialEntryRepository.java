package com.construction.management.project.repository;
import com.construction.management.project.domain.DailyLogMaterialEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DailyLogMaterialEntryRepository extends JpaRepository<DailyLogMaterialEntry, Long> { List<DailyLogMaterialEntry> findByDailyLogIdOrderBySortOrderAsc(String dailyLogId); void deleteByDailyLogId(String dailyLogId); }
