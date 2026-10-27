package com.construction.management.project.repository;
import com.construction.management.project.domain.DailyLogEquipmentEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DailyLogEquipmentEntryRepository extends JpaRepository<DailyLogEquipmentEntry, Long> { List<DailyLogEquipmentEntry> findByDailyLogIdOrderBySortOrderAsc(String dailyLogId); void deleteByDailyLogId(String dailyLogId); }
