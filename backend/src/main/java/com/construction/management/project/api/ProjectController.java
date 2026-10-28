package com.construction.management.project.api;

import com.construction.management.project.domain.ContractFile;
import com.construction.management.project.domain.ExecutionBudgetApprovalAttachment;
import com.construction.management.project.domain.VendorComparisonAttachment;
import com.construction.management.project.service.ApprovalService;
import com.construction.management.project.service.CustomerContractService;
import com.construction.management.project.service.ChangeManagementService;
import com.construction.management.project.service.ExecutionBudgetDetailService;
import com.construction.management.project.service.ExecutionBudgetApprovalAttachmentService;
import com.construction.management.project.service.ProjectService;
import com.construction.management.project.service.ProjectRevisionService;
import com.construction.management.project.service.ProjectSettlementService;
import com.construction.management.project.service.ProgressPaymentService;
import com.construction.management.project.service.PurchaseOrderService;
import com.construction.management.project.service.PaymentRequestService;
import com.construction.management.project.service.VendorComparisonService;
import com.construction.management.project.service.VendorComparisonAttachmentService;
import com.construction.management.project.service.DailyLogService;
import com.construction.management.project.service.MeetingMinutesService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

	private final ProjectService projectService;
	private final ProjectRevisionService projectRevisionService;
	private final CustomerContractService customerContractService;
	private final ChangeManagementService changeManagementService;
	private final ExecutionBudgetDetailService executionBudgetDetailService;
	private final VendorComparisonService vendorComparisonService;
	private final ProgressPaymentService progressPaymentService;
	private final ProjectSettlementService projectSettlementService;
	private final PurchaseOrderService purchaseOrderService;
	private final PaymentRequestService paymentRequestService;
	private final ApprovalService approvalService;
	private final ExecutionBudgetApprovalAttachmentService executionBudgetApprovalAttachmentService;
	private final VendorComparisonAttachmentService vendorComparisonAttachmentService;
	private final DailyLogService dailyLogService;
	private final MeetingMinutesService meetingMinutesService;

	public ProjectController(
			ProjectService projectService,
			ProjectRevisionService projectRevisionService,
			CustomerContractService customerContractService,
			ChangeManagementService changeManagementService,
			ExecutionBudgetDetailService executionBudgetDetailService,
			VendorComparisonService vendorComparisonService,
			ProgressPaymentService progressPaymentService,
			ProjectSettlementService projectSettlementService,
			PurchaseOrderService purchaseOrderService,
			PaymentRequestService paymentRequestService,
			ApprovalService approvalService,
			ExecutionBudgetApprovalAttachmentService executionBudgetApprovalAttachmentService,
			VendorComparisonAttachmentService vendorComparisonAttachmentService,
			DailyLogService dailyLogService,
			MeetingMinutesService meetingMinutesService
	) {
		this.projectService = projectService;
		this.projectRevisionService = projectRevisionService;
		this.customerContractService = customerContractService;
		this.changeManagementService = changeManagementService;
		this.executionBudgetDetailService = executionBudgetDetailService;
		this.vendorComparisonService = vendorComparisonService;
		this.progressPaymentService = progressPaymentService;
		this.projectSettlementService = projectSettlementService;
		this.purchaseOrderService = purchaseOrderService;
		this.paymentRequestService = paymentRequestService;
		this.approvalService = approvalService;
		this.executionBudgetApprovalAttachmentService = executionBudgetApprovalAttachmentService;
		this.vendorComparisonAttachmentService = vendorComparisonAttachmentService;
		this.dailyLogService = dailyLogService;
		this.meetingMinutesService = meetingMinutesService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROJECT_CREATE')")
	public ProjectResponse create(@Valid @RequestBody ProjectCreateRequest request) {
		return projectService.create(request);
	}

	@GetMapping
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROJECT_READ')")
	public List<ProjectResponse> search(@RequestParam(name = "keyword", required = false) String keyword) {
		return projectService.search(keyword);
	}

	@DeleteMapping("/{projectId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROJECT_DELETE')")
	public void delete(@PathVariable("projectId") String projectId) {
		projectService.delete(projectId);
	}

	@GetMapping("/{projectId}/daily-logs")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_READ')")
	public List<DailyLogResponse> getDailyLogs(
			@PathVariable("projectId") String projectId,
			@RequestParam(name = "from", required = false) LocalDate from,
			@RequestParam(name = "to", required = false) LocalDate to
	) {
		return dailyLogService.list(projectId, from, to);
	}

	@GetMapping("/{projectId}/daily-logs/{dailyLogId}")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_READ')")
	public DailyLogResponse getDailyLog(@PathVariable("projectId") String projectId, @PathVariable("dailyLogId") String dailyLogId) {
		return dailyLogService.get(projectId, dailyLogId);
	}

	@PostMapping("/{projectId}/daily-logs")
	@ResponseStatus(HttpStatus.CREATED)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_WRITE')")
	public DailyLogResponse createDailyLog(@PathVariable("projectId") String projectId, @Valid @RequestBody DailyLogRequest request) {
		return dailyLogService.create(projectId, request);
	}

	@PutMapping("/{projectId}/daily-logs/{dailyLogId}")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_WRITE')")
	public DailyLogResponse updateDailyLog(@PathVariable("projectId") String projectId, @PathVariable("dailyLogId") String dailyLogId, @Valid @RequestBody DailyLogRequest request) {
		return dailyLogService.update(projectId, dailyLogId, request);
	}

	@PostMapping(path = "/{projectId}/daily-logs/{dailyLogId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_WRITE')")
	public DailyLogResponse uploadDailyLogPhoto(
			@PathVariable("projectId") String projectId, @PathVariable("dailyLogId") String dailyLogId,
			@RequestParam("file") MultipartFile file, @RequestParam(name = "phase", defaultValue = "DURING") String phase,
			@RequestParam(name = "caption", required = false) String caption, @RequestParam(name = "uploadedBy", required = false) String uploadedBy
	) { return dailyLogService.uploadPhoto(projectId, dailyLogId, phase, caption, file, uploadedBy); }

	@GetMapping("/{projectId}/daily-logs/{dailyLogId}/photos/{photoId}")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_READ')")
	public ResponseEntity<Resource> downloadDailyLogPhoto(@PathVariable("projectId") String projectId, @PathVariable("dailyLogId") String dailyLogId, @PathVariable("photoId") String photoId) {
		Resource resource = dailyLogService.loadPhoto(projectId, dailyLogId, photoId);
		return ResponseEntity.ok().cacheControl(CacheControl.noStore().mustRevalidate()).contentType(org.springframework.http.MediaTypeFactory.getMediaType(resource.getFilename()).orElse(MediaType.APPLICATION_OCTET_STREAM)).body(resource);
	}

	@DeleteMapping("/{projectId}/daily-logs/{dailyLogId}/photos/{photoId}")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_WRITE')")
	public DailyLogResponse deleteDailyLogPhoto(@PathVariable("projectId") String projectId, @PathVariable("dailyLogId") String dailyLogId, @PathVariable("photoId") String photoId) { return dailyLogService.deletePhoto(projectId, dailyLogId, photoId); }

	@DeleteMapping("/{projectId}/daily-logs/{dailyLogId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_WRITE')")
	public void deleteDailyLog(@PathVariable("projectId") String projectId, @PathVariable("dailyLogId") String dailyLogId) { dailyLogService.delete(projectId, dailyLogId); }

	@GetMapping("/{projectId}/meeting-minutes")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_READ')")
	public List<MeetingMinutesResponse> getMeetingMinutes(@PathVariable("projectId") String projectId) { return meetingMinutesService.list(projectId); }

	@GetMapping("/{projectId}/meeting-minutes/{meetingId}")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_READ')")
	public MeetingMinutesResponse getMeetingMinutes(@PathVariable("projectId") String projectId, @PathVariable("meetingId") String meetingId) { return meetingMinutesService.get(projectId, meetingId); }

	@PostMapping("/{projectId}/meeting-minutes")
	@ResponseStatus(HttpStatus.CREATED)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_WRITE')")
	public MeetingMinutesResponse createMeetingMinutes(@PathVariable("projectId") String projectId, @Valid @RequestBody MeetingMinutesRequest request) { return meetingMinutesService.create(projectId, request); }

	@PutMapping("/{projectId}/meeting-minutes/{meetingId}")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_WRITE')")
	public MeetingMinutesResponse updateMeetingMinutes(@PathVariable("projectId") String projectId, @PathVariable("meetingId") String meetingId, @Valid @RequestBody MeetingMinutesRequest request) { return meetingMinutesService.update(projectId, meetingId, request); }

	@PostMapping(path = "/{projectId}/meeting-minutes/{meetingId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_WRITE')")
	public MeetingMinutesResponse uploadMeetingAttachment(@PathVariable("projectId") String projectId, @PathVariable("meetingId") String meetingId, @RequestParam("file") MultipartFile file, @RequestParam(name = "uploadedBy", required = false) String uploadedBy) { return meetingMinutesService.uploadAttachment(projectId, meetingId, file, uploadedBy); }

	@GetMapping("/{projectId}/meeting-minutes/{meetingId}/attachments/{attachmentId}")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_READ')")
	public ResponseEntity<Resource> downloadMeetingAttachment(@PathVariable("projectId") String projectId, @PathVariable("meetingId") String meetingId, @PathVariable("attachmentId") String attachmentId) {
		Resource resource = meetingMinutesService.loadAttachment(projectId, meetingId, attachmentId);
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(resource.getFilename() == null ? "attachment" : resource.getFilename(), StandardCharsets.UTF_8).build().toString()).cacheControl(CacheControl.noStore().mustRevalidate()).contentType(org.springframework.http.MediaTypeFactory.getMediaType(resource.getFilename()).orElse(MediaType.APPLICATION_OCTET_STREAM)).body(resource);
	}

	@DeleteMapping("/{projectId}/meeting-minutes/{meetingId}/attachments/{attachmentId}")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_WRITE')")
	public MeetingMinutesResponse deleteMeetingAttachment(@PathVariable("projectId") String projectId, @PathVariable("meetingId") String meetingId, @PathVariable("attachmentId") String attachmentId) { return meetingMinutesService.deleteAttachment(projectId, meetingId, attachmentId); }

	@DeleteMapping("/{projectId}/meeting-minutes/{meetingId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SITE_WRITE')")
	public void deleteMeetingMinutes(@PathVariable("projectId") String projectId, @PathVariable("meetingId") String meetingId) { meetingMinutesService.delete(projectId, meetingId); }

	@PostMapping("/{projectId}/start-construction")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROJECT_STATUS')")
	public ProjectResponse startConstruction(@PathVariable("projectId") String projectId) {
		return projectService.startConstruction(projectId);
	}

	@PostMapping("/{projectId}/synchronize-preparation-status")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROJECT_STATUS')")
	public ProjectResponse synchronizePreparationStatus(@PathVariable("projectId") String projectId) {
		return projectService.synchronizePreparationStatus(projectId);
	}

	@GetMapping("/{projectId}/integrated-summary")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROJECT_READ')")
	public IntegratedSummaryResponse getIntegratedSummary(@PathVariable("projectId") String projectId) {
		return projectService.getIntegratedSummary(projectId);
	}

	@GetMapping("/{projectId}/contracts")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CONTRACT_READ')")
	public CustomerContractResponse getCustomerContract(@PathVariable("projectId") String projectId) {
		return customerContractService.getContract(projectId);
	}

	@PutMapping("/{projectId}/contracts")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CONTRACT_WRITE')")
	public CustomerContractResponse saveCustomerContract(
			@PathVariable("projectId") String projectId,
			@Valid @RequestBody CustomerContractRequest request
	) {
		return customerContractService.saveContract(projectId, request);
	}

	@PostMapping("/{projectId}/revisions")
	@ResponseStatus(HttpStatus.CREATED)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_WRITE')")
	public ProjectRevisionResponse createRevision(
			@PathVariable("projectId") String projectId,
			@Valid @RequestBody ProjectRevisionCreateRequest request
	) {
		return projectRevisionService.create(projectId, request);
	}

	@GetMapping("/{projectId}/revisions")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_READ')")
	public List<ProjectRevisionResponse> getRevisions(@PathVariable("projectId") String projectId) {
		return projectRevisionService.list(projectId);
	}

	@GetMapping("/{projectId}/revisions/{revisionId}")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_READ')")
	public ProjectRevisionResponse getRevision(
			@PathVariable("projectId") String projectId,
			@PathVariable("revisionId") String revisionId
	) {
		return projectRevisionService.get(projectId, revisionId);
	}

	@GetMapping("/{projectId}/revisions/{revisionId}/history")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_READ')")
	public ProjectRevisionHistoryResponse getRevisionHistory(
			@PathVariable("projectId") String projectId,
			@PathVariable("revisionId") String revisionId
	) {
		return projectRevisionService.history(projectId, revisionId);
	}

	@PutMapping("/{projectId}/revisions/{revisionId}/contract")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_WRITE')")
	public ProjectRevisionResponse saveRevisionContract(
			@PathVariable("projectId") String projectId,
			@PathVariable("revisionId") String revisionId,
			@Valid @RequestBody CustomerContractRequest request
	) {
		return projectRevisionService.saveContract(projectId, revisionId, request);
	}

	@PutMapping("/{projectId}/revisions/{revisionId}/execution-budget")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_WRITE')")
	public ProjectRevisionResponse saveRevisionExecutionBudget(
			@PathVariable("projectId") String projectId,
			@PathVariable("revisionId") String revisionId,
			@Valid @RequestBody ExecutionBudgetDetailRequest request
	) {
		return projectRevisionService.saveBudget(projectId, revisionId, request);
	}

	@PostMapping("/{projectId}/revisions/{revisionId}/request-approval")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_REQUEST')")
	public ApprovalResponse requestRevisionApproval(
			@PathVariable("projectId") String projectId,
			@PathVariable("revisionId") String revisionId,
			@RequestBody(required = false) ApprovalRequest request
	) {
		return projectRevisionService.requestApproval(projectId, revisionId, request);
	}

	@GetMapping("/{projectId}/revisions/procurement-items")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROJECT_READ')")
	public ProjectRevisionProcurementResponse getRevisionProcurementItems(@PathVariable("projectId") String projectId) {
		return projectRevisionService.procurementItemIds(projectId);
	}

 @PostMapping("/{projectId}/change-orders/executive-correction")
 @org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('EXECUTIVE_CORRECT')")
 public ChangeOrderResponse correctByExecutive(@PathVariable("projectId") String projectId,@Valid @RequestBody ExecutiveCorrectionRequest request) { return changeManagementService.correctByExecutive(projectId,request); }

	@PostMapping("/{projectId}/change-orders")
	@ResponseStatus(HttpStatus.CREATED)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_WRITE')")
	public ChangeOrderResponse createChangeOrder(
			@PathVariable("projectId") String projectId,
			@Valid @RequestBody ChangeOrderCreateRequest request
	) {
		return changeManagementService.createChangeOrder(projectId, request);
	}

	@PostMapping("/{projectId}/change-orders/{changeOrderId}/contract-changes")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_WRITE')")
	public ChangeOrderResponse saveContractChange(
			@PathVariable("projectId") String projectId,
			@PathVariable("changeOrderId") String changeOrderId,
			@Valid @RequestBody ContractChangeRequest request
	) {
		return changeManagementService.saveContractProposal(projectId, changeOrderId, request);
	}

	@PostMapping("/{projectId}/change-orders/{changeOrderId}/budget-proposals")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_WRITE')")
	public ChangeOrderResponse saveBudgetProposal(
			@PathVariable("projectId") String projectId,
			@PathVariable("changeOrderId") String changeOrderId,
			@Valid @RequestBody BudgetChangeProposalRequest request
	) {
		return changeManagementService.saveBudgetProposal(projectId, changeOrderId, request);
	}

	@PostMapping("/{projectId}/change-orders/{changeOrderId}/budget-changes/request-approval")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_REQUEST')")
	public ChangeOrderResponse requestChangeApproval(
			@PathVariable("projectId") String projectId,
			@PathVariable("changeOrderId") String changeOrderId
	) {
		return changeManagementService.requestApproval(projectId, changeOrderId);
	}

	@GetMapping("/{projectId}/change-orders/{changeOrderId}/impact-summary")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_READ')")
	public ChangeOrderResponse getChangeImpactSummary(
			@PathVariable("projectId") String projectId,
			@PathVariable("changeOrderId") String changeOrderId
	) {
		return changeManagementService.getImpactSummary(projectId, changeOrderId);
	}

	@GetMapping("/{projectId}/change-orders/{changeOrderId}/reconciliation")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_READ')")
	public ChangeOrderResponse getChangeReconciliation(
			@PathVariable("projectId") String projectId,
			@PathVariable("changeOrderId") String changeOrderId
	) {
		return changeManagementService.getReconciliation(projectId, changeOrderId);
	}

	@GetMapping("/{projectId}/change-orders")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CHANGE_READ')")
	public List<ChangeOrderResponse> getChangeOrders(@PathVariable("projectId") String projectId) {
		return changeManagementService.getHistory(projectId);
	}

	@PostMapping(path = "/{projectId}/contract-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CONTRACT_WRITE')")
	public CustomerContractResponse uploadContractFile(
			@PathVariable("projectId") String projectId,
			@RequestParam(name = "fileType", defaultValue = "CONTRACT") String fileType,
			@RequestParam("file") MultipartFile file
	) {
		return customerContractService.uploadFile(projectId, fileType, file);
	}

	@DeleteMapping("/{projectId}/contract-files/{fileId}")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CONTRACT_WRITE')")
	public CustomerContractResponse deleteContractFile(
			@PathVariable("projectId") String projectId,
			@PathVariable("fileId") String fileId
	) {
		return customerContractService.deleteFile(projectId, fileId);
	}

	@GetMapping("/{projectId}/contract-files/{fileId}")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('CONTRACT_READ')")
	public ResponseEntity<Resource> downloadContractFile(
			@PathVariable("projectId") String projectId,
			@PathVariable("fileId") String fileId
	) {
		ContractFile file = customerContractService.getFile(projectId, fileId);
		Resource resource = customerContractService.loadFileResource(file);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(file.getFileName(), StandardCharsets.UTF_8)
						.build()
						.toString())
				.cacheControl(CacheControl.noStore().mustRevalidate())
				.contentType(resolveContractFileContentType(file))
				.body(resource);
	}

	private MediaType resolveContractFileContentType(ContractFile file) {
		return org.springframework.http.MediaTypeFactory.getMediaType(file.getFileName())
				.orElse(MediaType.APPLICATION_OCTET_STREAM);
	}

	@GetMapping("/{projectId}/execution-budget-details")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('BUDGET_READ')")
	public ExecutionBudgetDetailResponse getExecutionBudgetDetails(@PathVariable("projectId") String projectId) {
		return executionBudgetDetailService.getDetails(projectId);
	}

	@GetMapping("/{projectId}/approvals/{approvalId}/execution-budget-attachments/{attachmentId}")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('APPROVAL_READ')")
	public ResponseEntity<Resource> downloadExecutionBudgetApprovalAttachment(
			@PathVariable("projectId") String projectId,
			@PathVariable("approvalId") String approvalId,
			@PathVariable("attachmentId") String attachmentId
	) {
		ExecutionBudgetApprovalAttachment attachment = executionBudgetApprovalAttachmentService.getFile(projectId, approvalId, attachmentId);
		Resource resource = executionBudgetApprovalAttachmentService.loadFileResource(attachment);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(attachment.getFileName(), StandardCharsets.UTF_8)
						.build()
						.toString())
				.cacheControl(CacheControl.noStore().mustRevalidate())
				.contentType(resolveAttachmentContentType(attachment))
				.body(resource);
	}

	private MediaType resolveAttachmentContentType(ExecutionBudgetApprovalAttachment attachment) {
		try {
			return attachment.getContentType() == null || attachment.getContentType().isBlank()
					? MediaType.APPLICATION_OCTET_STREAM
					: MediaType.parseMediaType(attachment.getContentType());
		} catch (IllegalArgumentException ex) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}

	@PutMapping("/{projectId}/execution-budget-details")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('BUDGET_WRITE')")
	public ExecutionBudgetDetailResponse saveExecutionBudgetDetails(
			@PathVariable("projectId") String projectId,
			@Valid @RequestBody ExecutionBudgetDetailRequest request
	) {
		return executionBudgetDetailService.saveDetails(projectId, request);
	}

	@PostMapping(path = "/{projectId}/execution-budget-details/request-approval", consumes = MediaType.APPLICATION_JSON_VALUE)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('BUDGET_REQUEST')")
	public ApprovalResponse requestExecutionBudgetApproval(
			@PathVariable("projectId") String projectId,
			@RequestBody(required = false) ApprovalRequest request
	) {
		return approvalService.requestExecutionBudget(projectId, request);
	}

	@PostMapping(path = "/{projectId}/execution-budget-details/request-approval", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('BUDGET_REQUEST')")
	public ApprovalResponse requestExecutionBudgetApprovalWithAttachments(
			@PathVariable("projectId") String projectId,
			@RequestPart(name = "request", required = false) ApprovalRequest request,
			@RequestPart(name = "files", required = false) List<MultipartFile> files
	) {
		return approvalService.requestExecutionBudget(projectId, request, files);
	}

	@GetMapping("/{projectId}/execution-items/{itemId}/comparisons")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROCUREMENT_READ')")
	public VendorComparisonResponse getVendorComparisons(
			@PathVariable("projectId") String projectId,
			@PathVariable("itemId") String itemId
	) {
		return vendorComparisonService.getComparisons(projectId, itemId);
	}

	@PostMapping("/{projectId}/execution-items/{itemId}/comparisons")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROCUREMENT_WRITE')")
	public VendorComparisonResponse saveVendorComparisons(
			@PathVariable("projectId") String projectId,
			@PathVariable("itemId") String itemId,
			@Valid @RequestBody VendorComparisonRequest request
	) {
		return vendorComparisonService.saveComparisons(projectId, itemId, request);
	}

	@PostMapping(path = "/{projectId}/execution-items/{itemId}/comparisons/{comparisonId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROCUREMENT_WRITE')")
	public ApprovalResponse.Attachment uploadVendorComparisonAttachment(
			@PathVariable("projectId") String projectId,
			@PathVariable("itemId") String itemId,
			@PathVariable("comparisonId") String comparisonId,
			@RequestParam("file") MultipartFile file
	) {
		return vendorComparisonService.uploadAttachment(projectId, itemId, comparisonId, file);
	}

	@GetMapping("/{projectId}/execution-items/{itemId}/comparison-attachments/{attachmentId}")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROCUREMENT_READ')")
	public ResponseEntity<Resource> downloadVendorComparisonAttachment(
			@PathVariable("projectId") String projectId,
			@PathVariable("itemId") String itemId,
			@PathVariable("attachmentId") String attachmentId
	) {
		VendorComparisonAttachment attachment = vendorComparisonAttachmentService.getFile(projectId, itemId, attachmentId);
		Resource resource = vendorComparisonAttachmentService.loadFileResource(attachment);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(attachment.getFileName(), StandardCharsets.UTF_8).build().toString())
				.cacheControl(CacheControl.noStore().mustRevalidate())
				.contentType(resolveVendorComparisonAttachmentContentType(attachment))
				.body(resource);
	}

	private MediaType resolveVendorComparisonAttachmentContentType(VendorComparisonAttachment attachment) {
		try {
			return attachment.getContentType() == null || attachment.getContentType().isBlank()
					? MediaType.APPLICATION_OCTET_STREAM
					: MediaType.parseMediaType(attachment.getContentType());
		} catch (IllegalArgumentException ex) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}

	@GetMapping("/{projectId}/purchase-orders/draft/{itemId}")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROCUREMENT_READ')")
	public PurchaseOrderDraftResponse getPurchaseOrderDraft(
			@PathVariable("projectId") String projectId,
			@PathVariable("itemId") String itemId
	) {
		return vendorComparisonService.getPurchaseOrderDraft(projectId, itemId);
	}

	@PostMapping("/{projectId}/purchase-orders/draft/{itemId}/request")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROCUREMENT_REQUEST')")
	public PurchaseOrderDraftResponse requestPurchaseOrder(
			@PathVariable("projectId") String projectId,
			@PathVariable("itemId") String itemId
	) {
		return vendorComparisonService.requestPurchaseOrder(projectId, itemId);
	}

	@GetMapping("/{projectId}/progress-payments")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROGRESS_READ')")
	public ProgressPaymentResponse getProgressPayments(@PathVariable("projectId") String projectId) {
		return progressPaymentService.getStatus(projectId);
	}

	@GetMapping("/{projectId}/purchase-orders")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROCUREMENT_READ')")
	public PurchaseOrderResponse getPurchaseOrders(@PathVariable("projectId") String projectId) {
		return purchaseOrderService.getPurchaseOrders(projectId);
	}

	@PostMapping("/{projectId}/payment-requests")
	@ResponseStatus(HttpStatus.CREATED)
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PAYMENT_REQUEST')")
	public void requestPayments(@PathVariable("projectId") String projectId, @RequestBody PaymentRequestCreateRequest request) {
		paymentRequestService.requestPayments(projectId, request);
	}

	@GetMapping("/{projectId}/settlement")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SETTLEMENT_READ')")
	public ProjectSettlementResponse getSettlement(@PathVariable("projectId") String projectId) {
		return projectSettlementService.getSettlement(projectId);
	}

	@PostMapping({"/{projectId}/settlement/request-approval", "/{projectId}/settlement/complete"})
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('SETTLEMENT_REQUEST')")
	public ProjectSettlementResponse requestSettlementApproval(
			@PathVariable("projectId") String projectId,
			@RequestBody ProjectSettlementCompleteRequest request
	) {
		return projectSettlementService.requestSettlementApproval(projectId, request);
	}

	@PostMapping("/{projectId}/progress-claims")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROGRESS_WRITE')")
	public ProgressPaymentResponse createProgressClaim(
			@PathVariable("projectId") String projectId,
			@RequestBody ProgressPaymentRequest.Claim request
	) {
		return progressPaymentService.createClaim(projectId, request);
	}

	@DeleteMapping("/{projectId}/progress-claims/{claimId}")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PROGRESS_WRITE')")
	public ProgressPaymentResponse deleteUnapprovedProgressClaim(
			@PathVariable("projectId") String projectId,
			@PathVariable("claimId") String claimId
	) {
		return progressPaymentService.deleteUnapprovedClaim(projectId, claimId);
	}

	@PostMapping("/{projectId}/progress-claims/{claimId}/payments")
	@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('PAYMENT_EXECUTE')")
	public ProgressPaymentResponse createPayment(
			@PathVariable("projectId") String projectId,
			@PathVariable("claimId") String claimId,
			@RequestBody ProgressPaymentRequest.Payment request
	) {
		return progressPaymentService.createPayment(projectId, claimId, request);
	}
}
