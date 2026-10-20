package com.construction.management.project.service;

import com.construction.management.project.api.ApprovalResponse.Attachment;
import com.construction.management.project.domain.VendorComparisonAttachment;
import com.construction.management.project.repository.VendorComparisonAttachmentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VendorComparisonAttachmentService {

	private final VendorComparisonAttachmentRepository attachmentRepository;
	private final Path uploadRoot;

	public VendorComparisonAttachmentService(
			VendorComparisonAttachmentRepository attachmentRepository,
			@Value("${app.vendor-comparison-attachment.upload-dir:uploads/vendor-comparisons}") String uploadDir
	) {
		this.attachmentRepository = attachmentRepository;
		this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
	}

	@Transactional
	public Attachment save(String projectId, String executionItemId, String comparisonId, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("첨부할 견적서 파일을 선택해 주세요.");
		}
		Path comparisonDirectory = uploadRoot.resolve(projectId).resolve(executionItemId).resolve(comparisonId).normalize();
		try {
			Files.createDirectories(comparisonDirectory);
			String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "estimate" : file.getOriginalFilename());
			Path target = comparisonDirectory.resolve(UUID.randomUUID() + "_" + originalName).normalize();
			if (!target.startsWith(comparisonDirectory)) {
				throw new IllegalArgumentException("첨부 파일 경로가 올바르지 않습니다.");
			}
			file.transferTo(target);
			VendorComparisonAttachment attachment = attachmentRepository.save(new VendorComparisonAttachment(
					"vca_" + UUID.randomUUID().toString().replace("-", ""),
					projectId,
					executionItemId,
					comparisonId,
					originalName,
					target.toString(),
					file.getContentType()
			));
			return toResponse(attachment);
		} catch (IOException ex) {
			throw new IllegalStateException("비교견적 첨부파일 저장에 실패했습니다.", ex);
		}
	}

	@Transactional(readOnly = true)
	public List<Attachment> getResponses(String projectId, String executionItemId, String comparisonId) {
		return attachmentRepository.findByProjectIdAndExecutionItemIdAndComparisonIdOrderByUploadedAtAsc(projectId, executionItemId, comparisonId)
				.stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public VendorComparisonAttachment getFile(String projectId, String executionItemId, String attachmentId) {
		VendorComparisonAttachment attachment = attachmentRepository.findById(attachmentId)
				.orElseThrow(() -> new EntityNotFoundException("비교견적 첨부파일을 찾을 수 없습니다."));
		if (!projectId.equals(attachment.getProjectId()) || !executionItemId.equals(attachment.getExecutionItemId())) {
			throw new EntityNotFoundException("프로젝트 또는 실행항목에 속하지 않는 비교견적 첨부파일입니다.");
		}
		return attachment;
	}

	public Resource loadFileResource(VendorComparisonAttachment attachment) {
		try {
			Resource resource = new UrlResource(Path.of(attachment.getFilePath()).toUri());
			if (!resource.exists() || !resource.isReadable()) {
				throw new EntityNotFoundException("비교견적 첨부파일을 읽을 수 없습니다.");
			}
			return resource;
		} catch (IOException ex) {
			throw new IllegalStateException("비교견적 첨부파일을 불러오지 못했습니다.", ex);
		}
	}

	@Transactional
	public void deleteAllForProject(String projectId) {
		List<VendorComparisonAttachment> attachments = attachmentRepository.findByProjectIdOrderByUploadedAtAsc(projectId);
		try {
			for (VendorComparisonAttachment attachment : attachments) {
				Path filePath = Path.of(attachment.getFilePath()).toAbsolutePath().normalize();
				if (filePath.startsWith(uploadRoot)) {
					Files.deleteIfExists(filePath);
				}
			}
		} catch (IOException ex) {
			throw new IllegalStateException("비교견적 첨부파일 삭제에 실패했습니다.", ex);
		}
		attachmentRepository.deleteByProjectId(projectId);
	}

	private Attachment toResponse(VendorComparisonAttachment attachment) {
		return new Attachment(
				attachment.getAttachmentId(),
				attachment.getFileName(),
				attachment.getContentType(),
				"/api/v1/projects/" + attachment.getProjectId() + "/execution-items/" + attachment.getExecutionItemId()
						+ "/comparison-attachments/" + attachment.getAttachmentId()
		);
	}
}
