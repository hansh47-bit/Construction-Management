package com.construction.management.project.repository;
import com.construction.management.project.domain.MeetingMinutes;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MeetingMinutesRepository extends JpaRepository<MeetingMinutes, String> { List<MeetingMinutes> findByProjectIdOrderByMeetingAtDesc(String projectId); void deleteByProjectId(String projectId); }
