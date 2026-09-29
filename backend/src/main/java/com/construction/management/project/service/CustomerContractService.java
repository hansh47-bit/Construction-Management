package com.construction.management.project.service;

import com.construction.management.project.api.CustomerContractRequest;
import com.construction.management.project.api.CustomerContractResponse;
import com.construction.management.project.api.CustomerContractResponse.FileResponse;
import com.construction.management.project.api.CustomerContractResponse.Item;
import com.construction.management.project.domain.ContractFile;
import com.construction.management.project.domain.CustomerContractItem;
import com.construction.management.project.domain.Project;
import com.construction.management.project.repository.ContractFileRepository;
import com.construction.management.project.repository.CustomerContractItemRepository;
import com.construction.management.project.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
public class CustomerContractService {

	private final ProjectRepository projectRepository;
	private final CustomerContractItemRepository itemRepository;
	private final ContractFileRepository fileRepository;
	private final Path uploadRoot;

	public CustomerContractService(
			ProjectRepository projectRepository,
			CustomerContractItemRepository itemRepository,
			ContractFileRepository fileRepository,
			@Value("${app.contract-file.upload-dir:uploads/contracts}") String uploadDir
	) {
		this.projectRepository = projectRepository;
		this.itemRepository = itemRepository;
		this.fileRepository = fileRepository;
		this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
	}

	@Transactional(readOnly = true)
	public CustomerContractResponse getContract(String projectId) {
		Project project = getProject(projectId);
		return toResponse(project);
	}

	@Transactional
	public CustomerContractResponse saveContract(String projectId, CustomerContractRequest request) {
		Project project = getProject(projectId);
		project.updateTargetProfitRate(defaultZero(request.targetProfitRate()));

		itemRepository.deleteByProjectId(projectId);
		List<CustomerContractItem> items = request.items().stream()
				.filter(item -> StringUtils.hasText(item.workCategory()))
				.map(item -> new CustomerContractItem(
						"item_" + UUID.randomUUID().toString().replace("-", ""),
						projectId,
						item.workCategory(),
						item.quotedAmount(),
						item.contractAmount(),
						item.vatAmount(),
						item.note()
				))
				.toList();
		itemRepository.saveAll(items);

		return toResponse(project);
	}

	@Transactional
	public CustomerContractResponse uploadFile(String projectId, String fileType, MultipartFile file) {
		Project project = getProject(projectId);
		if (file.isEmpty()) {
			throw new IllegalArgumentException("업로드할 파일이 비어 있습니다.");
		}

		try {
			Path projectDir = uploadRoot.resolve(projectId).normalize();
			Files.createDirectories(projectDir);
			String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "contract-file" : file.getOriginalFilename());
			String storedName = UUID.randomUUID() + "_" + originalName;
			Path target = projectDir.resolve(storedName).normalize();
			file.transferTo(target);

			fileRepository.save(new ContractFile(
					"file_" + UUID.randomUUID().toString().replace("-", ""),
					projectId,
					originalName,
					target.toString(),
					StringUtils.hasText(fileType) ? fileType : "CONTRACT"
			));
		} catch (IOException ex) {
			throw new IllegalStateException("파일 저장에 실패했습니다.", ex);
		}

		return toResponse(project);
	}

	@Transactional(readOnly = true)
	public ContractFile getFile(String projectId, String fileId) {
		ContractFile file = fileRepository.findById(fileId)
				.orElseThrow(() -> new EntityNotFoundException("파일을 찾을 수 없습니다: " + fileId));
		if (!projectId.equals(file.getProjectId())) {
			throw new EntityNotFoundException("프로젝트에 속한 파일이 아닙니다.");
		}
		return file;
	}

	public Resource loadFileResource(ContractFile file) {
		try {
			Resource resource = new UrlResource(Path.of(file.getFilePath()).toUri());
			if (!resource.exists() || !resource.isReadable()) {
				throw new EntityNotFoundException("파일을 읽을 수 없습니다: " + file.getFileId());
			}
			return resource;
		} catch (IOException ex) {
			throw new IllegalStateException("파일 로드에 실패했습니다.", ex);
		}
	}

	private CustomerContractResponse toResponse(Project project) {
		List<CustomerContractItem> items = itemRepository.findByProjectIdOrderByWorkCategoryAsc(project.getProjectId());
		BigDecimal totalSupply = items.stream()
				.map(CustomerContractItem::getContractAmount)
				.map(CustomerContractService::defaultZero)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalVat = items.stream()
				.map(CustomerContractItem::getVatAmount)
				.map(CustomerContractService::defaultZero)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalWithVat = totalSupply.add(totalVat);
		BigDecimal targetCostLimit = totalSupply
				.multiply(BigDecimal.ONE.subtract(defaultZero(project.getTargetProfitRate()).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
				.setScale(0, RoundingMode.HALF_UP);

		return new CustomerContractResponse(
				project.getProjectId(),
				project.getProjectCode(),
				project.getProjectName(),
				project.getClientName(),
				project.getTargetProfitRate(),
				totalSupply,
				totalVat,
				totalWithVat,
				targetCostLimit,
				items.stream().map(item -> new Item(
						item.getItemId(),
						item.getWorkCategory(),
						item.getQuotedAmount(),
						item.getContractAmount(),
						item.getVatAmount(),
						item.getTotalAmount(),
						item.getNote()
				)).toList(),
				fileRepository.findByProjectIdOrderByUploadedAtDesc(project.getProjectId()).stream()
						.map(file -> new FileResponse(
								file.getFileId(),
								file.getFileName(),
								file.getFileType(),
								"/api/v1/projects/" + project.getProjectId() + "/contract-files/" + file.getFileId()
						))
						.toList()
		);
	}

	private Project getProject(String projectId) {
		return projectRepository.findById(projectId)
				.orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다: " + projectId));
	}

	private static BigDecimal defaultZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
