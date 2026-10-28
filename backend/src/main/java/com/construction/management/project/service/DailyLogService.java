package com.construction.management.project.service;

import com.construction.management.project.api.DailyLogRequest;
import com.construction.management.project.api.DailyLogResponse;
import com.construction.management.project.domain.*;
import com.construction.management.project.repository.*;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
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
@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROJECT_READ')")
public class DailyLogService {
	@org.springframework.beans.factory.annotation.Autowired private com.construction.management.auth.BusinessPolicy businessPolicy;

	private final ProjectRepository projectRepository;
	private final DailyLogRepository dailyLogRepository;
	private final DailyLogLaborEntryRepository laborRepository;
	private final DailyLogEquipmentEntryRepository equipmentRepository;
	private final DailyLogMaterialEntryRepository materialRepository;
	private final DailyLogPhotoRepository photoRepository;
	private final Path uploadRoot;

	public DailyLogService(ProjectRepository projectRepository, DailyLogRepository dailyLogRepository, DailyLogLaborEntryRepository laborRepository,
			DailyLogEquipmentEntryRepository equipmentRepository, DailyLogMaterialEntryRepository materialRepository, DailyLogPhotoRepository photoRepository,
			@Value("${app.site-record.upload-dir:uploads/site-records}") String uploadDir) {
		this.projectRepository = projectRepository; this.dailyLogRepository = dailyLogRepository; this.laborRepository = laborRepository;
		this.equipmentRepository = equipmentRepository; this.materialRepository = materialRepository; this.photoRepository = photoRepository;
		this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
	}

	@Transactional(readOnly = true)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_READ')")
	public List<DailyLogResponse> list(String projectId, LocalDate from, LocalDate to) {
		ensureProject(projectId);
		List<DailyLog> logs = from != null && to != null
				? dailyLogRepository.findByProjectIdAndLogDateBetweenOrderByLogDateDesc(projectId, from, to)
				: dailyLogRepository.findByProjectIdOrderByLogDateDesc(projectId);
		return logs.stream().filter(log -> !"DELETED".equals(log.getStatus())).map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_READ')")
	public DailyLogResponse get(String projectId, String dailyLogId) { return toResponse(getLog(projectId, dailyLogId)); }

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_WRITE')")
	@com.construction.management.auth.BusinessAction
	public DailyLogResponse create(String projectId, DailyLogRequest request) {
		ensureWritable(projectId);
		if (dailyLogRepository.findByProjectIdAndLogDate(projectId, request.logDate()).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "해당 날짜의 작업일지가 이미 있습니다.");
		}
		String author = author(request.createdBy());
		DailyLog log = new DailyLog("DL-" + UUID.randomUUID().toString().replace("-", ""), projectId, request.logDate(), author);
		apply(log, request, author);
		dailyLogRepository.save(log);
		replaceChildren(log.getDailyLogId(), request);
		return toResponse(log);
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_WRITE')")
	@com.construction.management.auth.BusinessAction
	public DailyLogResponse update(String projectId, String dailyLogId, DailyLogRequest request) {
		businessPolicy.ownEntity(com.construction.management.project.domain.DailyLog.class, dailyLogId);
		ensureWritable(projectId);
		DailyLog log = getLog(projectId, dailyLogId);
		if (!log.getLogDate().equals(request.logDate())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "작업일지 작성일자는 변경할 수 없습니다.");
		}
		if ("DELETED".equals(log.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "삭제된 작업일지는 수정할 수 없습니다.");
		apply(log, request, author(request.createdBy()));
		replaceChildren(log.getDailyLogId(), request);
		return toResponse(log);
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_WRITE')")
	@com.construction.management.auth.BusinessAction
	public DailyLogResponse uploadPhoto(String projectId, String dailyLogId, String phase, String caption, MultipartFile file, String uploadedBy) {
		businessPolicy.ownEntity(com.construction.management.project.domain.DailyLog.class, dailyLogId);
		ensureWritable(projectId);
		DailyLog log = getLog(projectId, dailyLogId);
		if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "업로드할 사진을 선택하세요.");
		String mimeType = Optional.ofNullable(file.getContentType()).orElse("");
		if (!mimeType.startsWith("image/")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현장 사진은 이미지 파일만 업로드할 수 있습니다.");
		String normalizedPhase = Set.of("BEFORE", "DURING", "AFTER").contains(phase) ? phase : "DURING";
		String originalName = cleanName(file.getOriginalFilename(), "site-photo");
		Path target = store(projectId, "daily-logs/" + dailyLogId, originalName, file);
		photoRepository.save(new DailyLogPhoto("DLP-" + UUID.randomUUID().toString().replace("-", ""), dailyLogId, normalizedPhase, target.toString(), originalName, mimeType, file.getSize(), trim(caption), photoRepository.findByDailyLogIdOrderBySortOrderAscUploadedAtAsc(dailyLogId).size(), author(uploadedBy)));
		return toResponse(log);
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_WRITE')")
	@com.construction.management.auth.BusinessAction
	public DailyLogResponse deletePhoto(String projectId, String dailyLogId, String photoId) {
		businessPolicy.ownEntity(com.construction.management.project.domain.DailyLog.class, dailyLogId);
		ensureWritable(projectId);
		DailyLog log = getLog(projectId, dailyLogId);
		DailyLogPhoto photo = photoRepository.findById(photoId).orElseThrow(() -> new EntityNotFoundException("현장 사진을 찾을 수 없습니다."));
		if (!dailyLogId.equals(photo.getDailyLogId())) throw new EntityNotFoundException("해당 작업일지의 사진이 아닙니다.");
		deleteStoredFile(photo.getStorageKey()); photoRepository.delete(photo);
		return toResponse(log);
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_WRITE')")
	@com.construction.management.auth.BusinessAction
	public void delete(String projectId, String dailyLogId) {
		businessPolicy.ownEntity(com.construction.management.project.domain.DailyLog.class, dailyLogId);
		ensureWritable(projectId); DailyLog log = getLog(projectId, dailyLogId);
		if (!"DRAFT".equals(log.getStatus())) { log.update(log.getWeatherCondition(), log.getTemperatureC(), log.getSiteManagerName(), log.getTodayWorkContent(), log.getTomorrowPlan(), log.isSafetyTrainingCompleted(), log.getSafetyTrainingNote(), log.getComplaintNote(), log.getDesignChangeIssueNote(), log.getOtherNote(), "DELETED", com.construction.management.auth.CurrentActor.name()); return; }
		deleteAllFiles(dailyLogId); laborRepository.deleteByDailyLogId(dailyLogId); equipmentRepository.deleteByDailyLogId(dailyLogId); materialRepository.deleteByDailyLogId(dailyLogId); dailyLogRepository.delete(log);
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROJECT_DELETE')")
	public void deleteAllForProject(String projectId) {
		for (DailyLog log : dailyLogRepository.findByProjectIdOrderByLogDateDesc(projectId)) { deleteAllFiles(log.getDailyLogId()); laborRepository.deleteByDailyLogId(log.getDailyLogId()); equipmentRepository.deleteByDailyLogId(log.getDailyLogId()); materialRepository.deleteByDailyLogId(log.getDailyLogId()); }
		dailyLogRepository.deleteByProjectId(projectId);
	}

	@Transactional(readOnly = true)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_READ')")
	public Resource loadPhoto(String projectId, String dailyLogId, String photoId) {
		getLog(projectId, dailyLogId); DailyLogPhoto photo = photoRepository.findById(photoId).orElseThrow(() -> new EntityNotFoundException("현장 사진을 찾을 수 없습니다."));
		if (!dailyLogId.equals(photo.getDailyLogId())) throw new EntityNotFoundException("해당 작업일지의 사진이 아닙니다.");
		return loadStoredFile(photo.getStorageKey());
	}

	private void apply(DailyLog log, DailyLogRequest request, String author) {
		log.update(trim(request.weatherCondition()), request.temperatureC(), request.siteManagerName().trim(), trim(request.todayWorkContent()), trim(request.tomorrowPlan()), request.safetyTrainingCompleted(), trim(request.safetyTrainingNote()), trim(request.complaintNote()), trim(request.designChangeIssueNote()), trim(request.otherNote()), status(request.status()), author);
	}
	private void replaceChildren(String id, DailyLogRequest request) {
		laborRepository.deleteByDailyLogId(id); equipmentRepository.deleteByDailyLogId(id); materialRepository.deleteByDailyLogId(id);
		List<DailyLogRequest.LaborEntry> labor = Optional.ofNullable(request.laborEntries()).orElse(List.of());
		laborRepository.saveAll(java.util.stream.IntStream.range(0, labor.size()).filter(i -> StringUtils.hasText(labor.get(i).tradeName())).mapToObj(i -> { DailyLogRequest.LaborEntry e = labor.get(i); return new DailyLogLaborEntry(id, e.tradeName().trim(), trim(e.workforceType()), trim(e.vendorName()), e.headcount() == null ? BigDecimal.ZERO : e.headcount(), trim(e.note()), i); }).toList());
		List<DailyLogRequest.EquipmentEntry> equipment = Optional.ofNullable(request.equipmentEntries()).orElse(List.of());
		equipmentRepository.saveAll(java.util.stream.IntStream.range(0, equipment.size()).filter(i -> StringUtils.hasText(equipment.get(i).equipmentName())).mapToObj(i -> { DailyLogRequest.EquipmentEntry e = equipment.get(i); return new DailyLogEquipmentEntry(id, e.equipmentName().trim(), e.quantity() == null ? BigDecimal.ZERO : e.quantity(), trim(e.note()), i); }).toList());
		List<DailyLogRequest.MaterialEntry> material = Optional.ofNullable(request.materialEntries()).orElse(List.of());
		materialRepository.saveAll(java.util.stream.IntStream.range(0, material.size()).filter(i -> StringUtils.hasText(material.get(i).itemName())).mapToObj(i -> { DailyLogRequest.MaterialEntry e = material.get(i); String type = "WASTE_OUTBOUND".equals(e.entryType()) ? "WASTE_OUTBOUND" : "INBOUND"; return new DailyLogMaterialEntry(id, type, e.itemName().trim(), e.quantity(), trim(e.unit()), trim(e.note()), i); }).toList());
	}
	private DailyLogResponse toResponse(DailyLog log) {
		String id = log.getDailyLogId();
		return new DailyLogResponse(id, log.getProjectId(), log.getLogDate(), log.getWeatherCondition(), log.getTemperatureC(), log.getSiteManagerName(), log.getTodayWorkContent(), log.getTomorrowPlan(), log.isSafetyTrainingCompleted(), log.getSafetyTrainingNote(), log.getComplaintNote(), log.getDesignChangeIssueNote(), log.getOtherNote(), log.getStatus(), log.getCreatedUserId()==null ? "기존 기록 · "+log.getCreatedBy() : log.getCreatedBy(), log.getCreatedUserId(), log.getCreatedAt(), log.getUpdatedAt(),
				laborRepository.findByDailyLogIdOrderBySortOrderAsc(id).stream().map(e -> new DailyLogResponse.LaborEntry(e.getId(), e.getTradeName(), e.getWorkforceType(), e.getVendorName(), e.getHeadcount(), e.getNote())).toList(),
				equipmentRepository.findByDailyLogIdOrderBySortOrderAsc(id).stream().map(e -> new DailyLogResponse.EquipmentEntry(e.getId(), e.getEquipmentName(), e.getQuantity(), e.getNote())).toList(),
				materialRepository.findByDailyLogIdOrderBySortOrderAsc(id).stream().map(e -> new DailyLogResponse.MaterialEntry(e.getId(), e.getEntryType(), e.getItemName(), e.getQuantity(), e.getUnit(), e.getNote())).toList(),
				photoRepository.findByDailyLogIdOrderBySortOrderAscUploadedAtAsc(id).stream().map(p -> new DailyLogResponse.Photo(p.getPhotoId(), p.getPhase(), p.getOriginalFileName(), p.getMimeType(), p.getFileSize(), p.getCaption(), "/api/v1/projects/" + log.getProjectId() + "/daily-logs/" + id + "/photos/" + p.getPhotoId(), p.getUploadedAt())).toList());
	}
	private DailyLog getLog(String projectId, String id) { DailyLog log = dailyLogRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("작업일지를 찾을 수 없습니다.")); if (!projectId.equals(log.getProjectId())) throw new EntityNotFoundException("해당 프로젝트의 작업일지가 아닙니다."); return log; }
	private void ensureProject(String projectId) { if (!projectRepository.existsById(projectId)) throw new EntityNotFoundException("프로젝트를 찾을 수 없습니다."); }
	private void ensureWritable(String projectId) { Project project = projectRepository.findById(projectId).orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다.")); if ("정산완료".equals(project.getStatus().name())) throw new ResponseStatusException(HttpStatus.CONFLICT, "정산완료 프로젝트의 현장 기록은 조회만 가능합니다."); }
	private Path store(String projectId, String group, String originalName, MultipartFile file) { try { Path dir = uploadRoot.resolve(projectId).resolve(group).normalize(); if (!dir.startsWith(uploadRoot)) throw new IllegalStateException("잘못된 파일 경로입니다."); Files.createDirectories(dir); Path target = dir.resolve(UUID.randomUUID() + "_" + originalName).normalize(); if (!target.startsWith(dir)) throw new IllegalStateException("잘못된 파일 경로입니다."); file.transferTo(target); return target; } catch (IOException ex) { throw new IllegalStateException("파일 저장에 실패했습니다.", ex); } }
	private Resource loadStoredFile(String storageKey) { try { Path path = Path.of(storageKey).toAbsolutePath().normalize(); if (!path.startsWith(uploadRoot)) throw new IllegalStateException("잘못된 파일 경로입니다."); Resource resource = new UrlResource(path.toUri()); if (!resource.exists() || !resource.isReadable()) throw new EntityNotFoundException("파일을 읽을 수 없습니다."); return resource; } catch (IOException ex) { throw new IllegalStateException("파일을 읽을 수 없습니다.", ex); } }
	private void deleteAllFiles(String dailyLogId) { for (DailyLogPhoto photo : photoRepository.findByDailyLogIdOrderBySortOrderAscUploadedAtAsc(dailyLogId)) deleteStoredFile(photo.getStorageKey()); photoRepository.deleteByDailyLogId(dailyLogId); }
	private void deleteStoredFile(String key) { try { Path path = Path.of(key).toAbsolutePath().normalize(); if (!path.startsWith(uploadRoot)) throw new IllegalStateException("잘못된 파일 경로입니다."); Files.deleteIfExists(path); } catch (IOException ex) { throw new IllegalStateException("첨부파일 삭제에 실패했습니다.", ex); } }
	private static String status(String value) { return Set.of("DRAFT", "SHARED", "ARCHIVED").contains(value) ? value : "DRAFT"; }
	private static String author(String value) { return com.construction.management.auth.CurrentActor.name(); }
	private static String trim(String value) { return value == null ? null : value.trim(); }
	private static String cleanName(String value, String fallback) { String name = StringUtils.cleanPath(value == null ? fallback : value); return StringUtils.hasText(name) ? name : fallback; }
}
