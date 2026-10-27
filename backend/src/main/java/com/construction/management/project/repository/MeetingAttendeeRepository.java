package com.construction.management.project.repository;
import com.construction.management.project.domain.MeetingAttendee;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MeetingAttendeeRepository extends JpaRepository<MeetingAttendee, Long> { List<MeetingAttendee> findByMeetingIdOrderBySortOrderAsc(String meetingId); void deleteByMeetingId(String meetingId); }
