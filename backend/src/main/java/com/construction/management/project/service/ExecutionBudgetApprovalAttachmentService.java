package com.construction.management.project.service;

import com.construction.management.project.api.ApprovalResponse.Attachment;
import com.construction.management.project.domain.ExecutionBudgetApprovalAttachment;
import com.construction.management.project.repository.ExecutionBudgetApprovalAttachmentRepository;
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
@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROJECT_READ')")
public class ExecutionBudgetApprovalAttachmentService {
	@org.springframework.beans.factory.annotation.Autowired private com.construction.management.auth.BusinessPolicy businessPolicy;


	private final ExecutionBudgetApprovalAttachmentRepository attachmentRepository;
	private final Path uploadRoot;

	public ExecutionBudgetApprovalAttachmentService(
			ExecutionBudgetApprovalAttachmentRepository attachmentRepository,
			@Value("${app.execution-budget-attachment.upload-dir:uploads/execution-budget-approvals}") String uploadDir
	) {
		this.attachmentRepository = attachmentRepository;
		this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('BUDGET_REQUEST')")
	public void saveAll(String projectId, String approvalId, List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
			return;
		}
		Path approvalDirectory = uploadRoot.resolve(projectId).resolve(approvalId).normalize();
		try {
			Files.createDirectories(approvalDirectory);
			for (MultipartFile file : files) {
				if (file == null || file.isEmpty()) {
					continue;
				}
				String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename());
				Path target = approvalDirectory.resolve(UUID.randomUUID() + "_" + originalName).normalize();
				if (!target.startsWith(approvalDirectory)) {
					throw new IllegalArgumentException("첨부파일 경로가 올바르지 않습니다.");
				}
				file.transferTo(target);
				attachmentRepository.save(new ExecutionBudgetApprovalAttachment(
						"eba_" + UUID.randomUUID().toString().replace("-", ""),
						projectId,
						approvalId,
						originalName,
						target.toString(),
						file.getContentType()
				));
			}
		} catch (IOException ex) {
			throw new IllegalStateException("실행예산 결재 첨부파일 저장에 실패했습니다.", ex);
		}
	}

	@Transactional(readOnly = true)
	public List<Attachment> getResponses(String approvalId, String projectId) {
		return attachmentRepository.findByApprovalIdOrderByUploadedAtAsc(approvalId).stream()
				.filter(attachment -> projectId.equals(attachment.getProjectId()))
				.map(attachment -> new Attachment(
						attachment.getAttachmentId(),
						attachment.getFileName(),
						attachment.getContentType(),
						"/api/v1/projects/" + projectId + "/approvals/" + approvalId
								+ "/execution-budget-attachments/" + attachment.getAttachmentId()
				))
				.toList();
	}

	@Transactional(readOnly = true)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('APPROVAL_READ')")
	public ExecutionBudgetApprovalAttachment getFile(String projectId, String approvalId, String attachmentId) {
		businessPolicy.approval(approvalId);
		ExecutionBudgetApprovalAttachment attachment = attachmentRepository.findById(attachmentId)
				.orElseThrow(() -> new EntityNotFoundException("결재 첨부파일을 찾을 수 없습니다."));
		if (!projectId.equals(attachment.getProjectId()) || !approvalId.equals(attachment.getApprovalId())) {
			throw new EntityNotFoundException("프로젝트에 속하지 않은 결재 첨부파일입니다.");
		}
		return attachment;
	}

	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('APPROVAL_READ')")
	public Resource loadFileResource(ExecutionBudgetApprovalAttachment attachment) {
		try {
			Resource resource = new UrlResource(Path.of(attachment.getFilePath()).toUri());
			if (!resource.exists() || !resource.isReadable()) {
				throw new EntityNotFoundException("결재 첨부파일을 읽을 수 없습니다.");
			}
			return resource;
		} catch (IOException ex) {
			throw new IllegalStateException("결재 첨부파일을 불러오지 못했습니다.", ex);
		}
	}

	@Transactional
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROJECT_DELETE')")
	public void deleteAllForProject(String projectId) {
		List<ExecutionBudgetApprovalAttachment> attachments = attachmentRepository.findByProjectIdOrderByUploadedAtAsc(projectId);
		try {
			for (ExecutionBudgetApprovalAttachment attachment : attachments) {
				Path filePath = Path.of(attachment.getFilePath()).toAbsolutePath().normalize();
				if (filePath.startsWith(uploadRoot)) {
					Files.deleteIfExists(filePath);
				}
			}
		} catch (IOException ex) {
			throw new IllegalStateException("결재 첨부파일 삭제에 실패했습니다.", ex);
		}
		attachmentRepository.deleteByProjectId(projectId);
	}
}
