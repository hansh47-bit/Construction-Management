package com.construction.management.project.repository;
import com.construction.management.project.domain.MeetingActionItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MeetingActionItemRepository extends JpaRepository<MeetingActionItem, Long> { List<MeetingActionItem> findByMeetingIdOrderBySortOrderAsc(String meetingId); void deleteByMeetingId(String meetingId); }
