package com.construction.management.project.repository;
import com.construction.management.project.domain.MeetingAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MeetingAttachmentRepository extends JpaRepository<MeetingAttachment, String> { List<MeetingAttachment> findByMeetingIdOrderByUploadedAtDesc(String meetingId); void deleteByMeetingId(String meetingId); }
