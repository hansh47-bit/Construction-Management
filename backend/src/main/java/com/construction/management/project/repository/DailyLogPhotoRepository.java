package com.construction.management.project.repository;
import com.construction.management.project.domain.DailyLogPhoto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DailyLogPhotoRepository extends JpaRepository<DailyLogPhoto, String> { List<DailyLogPhoto> findByDailyLogIdOrderBySortOrderAscUploadedAtAsc(String dailyLogId); void deleteByDailyLogId(String dailyLogId); }
