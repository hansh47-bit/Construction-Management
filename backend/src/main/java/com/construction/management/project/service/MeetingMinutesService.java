package com.construction.management.project.service;

import com.construction.management.project.api.MeetingMinutesRequest;
import com.construction.management.project.api.MeetingMinutesResponse;
import com.construction.management.project.domain.*;
import com.construction.management.project.repository.*;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MeetingMinutesService {
	private final ProjectRepository projectRepository; private final MeetingMinutesRepository meetingRepository; private final MeetingAttendeeRepository attendeeRepository; private final MeetingActionItemRepository actionRepository; private final MeetingAttachmentRepository attachmentRepository; private final Path uploadRoot;
	public MeetingMinutesService(ProjectRepository projectRepository, MeetingMinutesRepository meetingRepository, MeetingAttendeeRepository attendeeRepository, MeetingActionItemRepository actionRepository, MeetingAttachmentRepository attachmentRepository, @Value("${app.site-record.upload-dir:uploads/site-records}") String uploadDir) { this.projectRepository = projectRepository; this.meetingRepository = meetingRepository; this.attendeeRepository = attendeeRepository; this.actionRepository = actionRepository; this.attachmentRepository = attachmentRepository; this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize(); }

	@Transactional(readOnly = true) public List<MeetingMinutesResponse> list(String projectId) { ensureProject(projectId); return meetingRepository.findByProjectIdOrderByMeetingAtDesc(projectId).stream().filter(m -> !"DELETED".equals(m.getStatus())).map(this::toResponse).toList(); }
	@Transactional(readOnly = true) public MeetingMinutesResponse get(String projectId, String meetingId) { return toResponse(getMeeting(projectId, meetingId)); }
	@Transactional public MeetingMinutesResponse create(String projectId, MeetingMinutesRequest request) { ensureWritable(projectId); String author = author(request.createdBy()); MeetingMinutes meeting = new MeetingMinutes("MIN-" + UUID.randomUUID().toString().replace("-", ""), projectId, request.meetingAt(), author); apply(meeting, request, author); meetingRepository.save(meeting); replaceChildren(meeting.getMeetingId(), request); return toResponse(meeting); }
	@Transactional public MeetingMinutesResponse update(String projectId, String meetingId, MeetingMinutesRequest request) { ensureWritable(projectId); MeetingMinutes meeting = getMeeting(projectId, meetingId); if ("DELETED".equals(meeting.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "삭제된 회의록은 수정할 수 없습니다."); apply(meeting, request, author(request.createdBy())); replaceChildren(meetingId, request); return toResponse(meeting); }

	@Transactional public MeetingMinutesResponse uploadAttachment(String projectId, String meetingId, MultipartFile file, String uploadedBy) {
		ensureWritable(projectId); MeetingMinutes meeting = getMeeting(projectId, meetingId); if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "업로드할 파일을 선택하세요.");
		String originalName = cleanName(file.getOriginalFilename(), "meeting-attachment"); String extension = extension(originalName);
		if (!Set.of("pdf", "doc", "docx", "xls", "xlsx", "csv", "jpg", "jpeg", "png", "webp").contains(extension)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PDF, 문서, 도면 또는 이미지 파일만 첨부할 수 있습니다.");
		Path target = store(projectId, "meeting-minutes/" + meetingId, originalName, file); String mimeType = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");
		attachmentRepository.save(new MeetingAttachment("MFA-" + UUID.randomUUID().toString().replace("-", ""), meetingId, target.toString(), originalName, mimeType, file.getSize(), author(uploadedBy)));
		return toResponse(meeting);
	}
	@Transactional public MeetingMinutesResponse deleteAttachment(String projectId, String meetingId, String attachmentId) { ensureWritable(projectId); MeetingMinutes meeting = getMeeting(projectId, meetingId); MeetingAttachment attachment = attachmentRepository.findById(attachmentId).orElseThrow(() -> new EntityNotFoundException("회의 첨부파일을 찾을 수 없습니다.")); if (!meetingId.equals(attachment.getMeetingId())) throw new EntityNotFoundException("해당 회의록의 첨부파일이 아닙니다."); deleteStoredFile(attachment.getStorageKey()); attachmentRepository.delete(attachment); return toResponse(meeting); }
	@Transactional(readOnly = true) public Resource loadAttachment(String projectId, String meetingId, String attachmentId) { getMeeting(projectId, meetingId); MeetingAttachment attachment = attachmentRepository.findById(attachmentId).orElseThrow(() -> new EntityNotFoundException("회의 첨부파일을 찾을 수 없습니다.")); if (!meetingId.equals(attachment.getMeetingId())) throw new EntityNotFoundException("해당 회의록의 첨부파일이 아닙니다."); return loadStoredFile(attachment.getStorageKey()); }
	@Transactional public void delete(String projectId, String meetingId) { ensureWritable(projectId); MeetingMinutes meeting = getMeeting(projectId, meetingId); if (!"DRAFT".equals(meeting.getStatus())) { meeting.update(meeting.getMeetingAt(), meeting.getLocation(), meeting.getSubject(), meeting.getAgendaContent(), meeting.getDecisionContent(), "DELETED", "system"); return; } deleteAllFiles(meetingId); attendeeRepository.deleteByMeetingId(meetingId); actionRepository.deleteByMeetingId(meetingId); meetingRepository.delete(meeting); }
	@Transactional public void deleteAllForProject(String projectId) { for (MeetingMinutes meeting : meetingRepository.findByProjectIdOrderByMeetingAtDesc(projectId)) { deleteAllFiles(meeting.getMeetingId()); attendeeRepository.deleteByMeetingId(meeting.getMeetingId()); actionRepository.deleteByMeetingId(meeting.getMeetingId()); } meetingRepository.deleteByProjectId(projectId); }

	private void apply(MeetingMinutes meeting, MeetingMinutesRequest request, String author) { meeting.update(request.meetingAt(), trim(request.location()), request.subject().trim(), sanitizeRichText(request.agendaContent()), sanitizeRichText(request.decisionContent()), status(request.status()), author); }
	private void replaceChildren(String meetingId, MeetingMinutesRequest request) {
		attendeeRepository.deleteByMeetingId(meetingId); actionRepository.deleteByMeetingId(meetingId);
		List<MeetingMinutesRequest.Attendee> attendees = Optional.ofNullable(request.attendees()).orElse(List.of());
		attendeeRepository.saveAll(java.util.stream.IntStream.range(0, attendees.size()).filter(i -> StringUtils.hasText(attendees.get(i).attendeeName()) && StringUtils.hasText(attendees.get(i).organizationName())).mapToObj(i -> { MeetingMinutesRequest.Attendee a = attendees.get(i); return new MeetingAttendee(meetingId, organizationType(a.organizationType()), a.organizationName().trim(), a.attendeeName().trim(), trim(a.positionName()), trim(a.contact()), i); }).toList());
		List<MeetingMinutesRequest.ActionItem> actions = Optional.ofNullable(request.actionItems()).orElse(List.of());
		actionRepository.saveAll(java.util.stream.IntStream.range(0, actions.size()).filter(i -> StringUtils.hasText(actions.get(i).actionContent())).mapToObj(i -> { MeetingMinutesRequest.ActionItem a = actions.get(i); return new MeetingActionItem(meetingId, a.actionContent().trim(), trim(a.assigneeName()), trim(a.assigneeUserId()), a.dueDate(), actionStatus(a.status()), i); }).toList());
	}
	private MeetingMinutesResponse toResponse(MeetingMinutes meeting) {
		String id = meeting.getMeetingId(); List<MeetingActionItem> actions = actionRepository.findByMeetingIdOrderBySortOrderAsc(id);
		return new MeetingMinutesResponse(id, meeting.getProjectId(), meeting.getMeetingAt(), meeting.getLocation(), meeting.getSubject(), meeting.getAgendaContent(), meeting.getDecisionContent(), meeting.getStatus(), meeting.getCreatedBy(), meeting.getCreatedAt(), meeting.getUpdatedAt(), actions.stream().filter(a -> !"DONE".equals(a.getStatus())).count(),
				attendeeRepository.findByMeetingIdOrderBySortOrderAsc(id).stream().map(a -> new MeetingMinutesResponse.Attendee(a.getId(), a.getOrganizationType(), a.getOrganizationName(), a.getAttendeeName(), a.getPositionName(), a.getContact())).toList(),
				actions.stream().map(a -> new MeetingMinutesResponse.ActionItem(a.getId(), a.getActionContent(), a.getAssigneeName(), a.getAssigneeUserId(), a.getDueDate(), a.getStatus(), a.getCompletedAt())).toList(),
				attachmentRepository.findByMeetingIdOrderByUploadedAtDesc(id).stream().map(a -> new MeetingMinutesResponse.Attachment(a.getAttachmentId(), a.getOriginalFileName(), a.getMimeType(), a.getFileSize(), "/api/v1/projects/" + meeting.getProjectId() + "/meeting-minutes/" + id + "/attachments/" + a.getAttachmentId(), a.getUploadedAt())).toList());
	}
	private MeetingMinutes getMeeting(String projectId, String meetingId) { MeetingMinutes meeting = meetingRepository.findById(meetingId).orElseThrow(() -> new EntityNotFoundException("회의록을 찾을 수 없습니다.")); if (!projectId.equals(meeting.getProjectId())) throw new EntityNotFoundException("해당 프로젝트의 회의록이 아닙니다."); return meeting; }
	private void ensureProject(String projectId) { if (!projectRepository.existsById(projectId)) throw new EntityNotFoundException("프로젝트를 찾을 수 없습니다."); }
	private void ensureWritable(String projectId) { Project project = projectRepository.findById(projectId).orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다.")); if ("정산완료".equals(project.getStatus().name())) throw new ResponseStatusException(HttpStatus.CONFLICT, "정산완료 프로젝트의 현장 기록은 조회만 가능합니다."); }
	private Path store(String projectId, String group, String name, MultipartFile file) { try { Path dir = uploadRoot.resolve(projectId).resolve(group).normalize(); if (!dir.startsWith(uploadRoot)) throw new IllegalStateException("잘못된 파일 경로입니다."); Files.createDirectories(dir); Path target = dir.resolve(UUID.randomUUID() + "_" + name).normalize(); if (!target.startsWith(dir)) throw new IllegalStateException("잘못된 파일 경로입니다."); file.transferTo(target); return target; } catch (IOException ex) { throw new IllegalStateException("파일 저장에 실패했습니다.", ex); } }
	private Resource loadStoredFile(String key) { try { Path path = Path.of(key).toAbsolutePath().normalize(); if (!path.startsWith(uploadRoot)) throw new IllegalStateException("잘못된 파일 경로입니다."); Resource resource = new UrlResource(path.toUri()); if (!resource.exists() || !resource.isReadable()) throw new EntityNotFoundException("파일을 읽을 수 없습니다."); return resource; } catch (IOException ex) { throw new IllegalStateException("파일을 읽을 수 없습니다.", ex); } }
	private void deleteAllFiles(String meetingId) { for (MeetingAttachment a : attachmentRepository.findByMeetingIdOrderByUploadedAtDesc(meetingId)) deleteStoredFile(a.getStorageKey()); attachmentRepository.deleteByMeetingId(meetingId); }
	private void deleteStoredFile(String key) { try { Path path = Path.of(key).toAbsolutePath().normalize(); if (!path.startsWith(uploadRoot)) throw new IllegalStateException("잘못된 파일 경로입니다."); Files.deleteIfExists(path); } catch (IOException ex) { throw new IllegalStateException("첨부파일 삭제에 실패했습니다.", ex); } }
	private static String sanitizeRichText(String value) {
		if (!StringUtils.hasText(value)) return null;
		String text = value.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", "")
				.replaceAll("(?i)\\s+on[a-z]+\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s>]+)", "")
				.replaceAll("(?i)javascript:", "")
				.replaceAll("(?i)<(p|br|strong|b|em|i|ul|ol|li|div|span)(?:\\s[^>]*)?>", "<$1>")
				.replaceAll("(?i)</?(?!p\\b|br\\b|strong\\b|b\\b|em\\b|i\\b|ul\\b|ol\\b|li\\b|div\\b|span\\b)[^>]+>", "");
		return text.length() > 16000 ? text.substring(0, 16000) : text;
	}
	private static String status(String value) { return Set.of("DRAFT", "SHARED", "ARCHIVED").contains(value) ? value : "DRAFT"; }
	private static String actionStatus(String value) { return Set.of("TODO", "IN_PROGRESS", "DONE").contains(value) ? value : "TODO"; }
	private static String organizationType(String value) { return Set.of("CLIENT", "CONTRACTOR", "VENDOR", "OTHER").contains(value) ? value : "OTHER"; }
	private static String author(String value) { return StringUtils.hasText(value) ? value.trim() : "현장 담당자"; }
	private static String trim(String value) { return value == null ? null : value.trim(); }
	private static String cleanName(String value, String fallback) { String name = StringUtils.cleanPath(value == null ? fallback : value); return StringUtils.hasText(name) ? name : fallback; }
	private static String extension(String value) { int pos = value.lastIndexOf('.'); return pos < 0 ? "" : value.substring(pos + 1).toLowerCase(Locale.ROOT); }
}
