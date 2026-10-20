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
import com.construction.management.project.service.ProjectSettlementService;
import com.construction.management.project.service.ProgressPaymentService;
import com.construction.management.project.service.PurchaseOrderService;
import com.construction.management.project.service.PaymentRequestService;
import com.construction.management.project.service.VendorComparisonService;
import com.construction.management.project.service.VendorComparisonAttachmentService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
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

	public ProjectController(
			ProjectService projectService,
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
			VendorComparisonAttachmentService vendorComparisonAttachmentService
	) {
		this.projectService = projectService;
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
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProjectResponse create(@Valid @RequestBody ProjectCreateRequest request) {
		return projectService.create(request);
	}

	@GetMapping
	public List<ProjectResponse> search(@RequestParam(name = "keyword", required = false) String keyword) {
		return projectService.search(keyword);
	}

	@DeleteMapping("/{projectId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable("projectId") String projectId) {
		projectService.delete(projectId);
	}

	@PostMapping("/{projectId}/start-construction")
	public ProjectResponse startConstruction(@PathVariable("projectId") String projectId) {
		return projectService.startConstruction(projectId);
	}

	@PostMapping("/{projectId}/synchronize-preparation-status")
	public ProjectResponse synchronizePreparationStatus(@PathVariable("projectId") String projectId) {
		return projectService.synchronizePreparationStatus(projectId);
	}

	@GetMapping("/{projectId}/integrated-summary")
	public IntegratedSummaryResponse getIntegratedSummary(@PathVariable("projectId") String projectId) {
		return projectService.getIntegratedSummary(projectId);
	}

	@GetMapping("/{projectId}/contracts")
	public CustomerContractResponse getCustomerContract(@PathVariable("projectId") String projectId) {
		return customerContractService.getContract(projectId);
	}

	@PutMapping("/{projectId}/contracts")
	public CustomerContractResponse saveCustomerContract(
			@PathVariable("projectId") String projectId,
			@Valid @RequestBody CustomerContractRequest request
	) {
		return customerContractService.saveContract(projectId, request);
	}

	@PostMapping("/{projectId}/change-orders/executive-correction")
	public ChangeOrderResponse correctAmountByExecutive(
			@PathVariable("projectId") String projectId,
			@Valid @RequestBody ExecutiveCorrectionRequest request
	) {
		return changeManagementService.correctByExecutive(projectId, request);
	}

	@GetMapping("/{projectId}/change-orders")
	public List<ChangeOrderResponse> getChangeOrders(@PathVariable("projectId") String projectId) {
		return changeManagementService.getHistory(projectId);
	}

	@PostMapping(path = "/{projectId}/contract-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public CustomerContractResponse uploadContractFile(
			@PathVariable("projectId") String projectId,
			@RequestParam(name = "fileType", defaultValue = "CONTRACT") String fileType,
			@RequestParam("file") MultipartFile file
	) {
		return customerContractService.uploadFile(projectId, fileType, file);
	}

	@DeleteMapping("/{projectId}/contract-files/{fileId}")
	public CustomerContractResponse deleteContractFile(
			@PathVariable("projectId") String projectId,
			@PathVariable("fileId") String fileId
	) {
		return customerContractService.deleteFile(projectId, fileId);
	}

	@GetMapping("/{projectId}/contract-files/{fileId}")
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
	public ExecutionBudgetDetailResponse getExecutionBudgetDetails(@PathVariable("projectId") String projectId) {
		return executionBudgetDetailService.getDetails(projectId);
	}

	@GetMapping("/{projectId}/approvals/{approvalId}/execution-budget-attachments/{attachmentId}")
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
	public ExecutionBudgetDetailResponse saveExecutionBudgetDetails(
			@PathVariable("projectId") String projectId,
			@Valid @RequestBody ExecutionBudgetDetailRequest request
	) {
		return executionBudgetDetailService.saveDetails(projectId, request);
	}

	@PostMapping(path = "/{projectId}/execution-budget-details/request-approval", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ApprovalResponse requestExecutionBudgetApproval(
			@PathVariable("projectId") String projectId,
			@RequestBody(required = false) ApprovalRequest request
	) {
		return approvalService.requestExecutionBudget(projectId, request);
	}

	@PostMapping(path = "/{projectId}/execution-budget-details/request-approval", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApprovalResponse requestExecutionBudgetApprovalWithAttachments(
			@PathVariable("projectId") String projectId,
			@RequestPart(name = "request", required = false) ApprovalRequest request,
			@RequestPart(name = "files", required = false) List<MultipartFile> files
	) {
		return approvalService.requestExecutionBudget(projectId, request, files);
	}

	@GetMapping("/{projectId}/execution-items/{itemId}/comparisons")
	public VendorComparisonResponse getVendorComparisons(
			@PathVariable("projectId") String projectId,
			@PathVariable("itemId") String itemId
	) {
		return vendorComparisonService.getComparisons(projectId, itemId);
	}

	@PostMapping("/{projectId}/execution-items/{itemId}/comparisons")
	public VendorComparisonResponse saveVendorComparisons(
			@PathVariable("projectId") String projectId,
			@PathVariable("itemId") String itemId,
			@Valid @RequestBody VendorComparisonRequest request
	) {
		return vendorComparisonService.saveComparisons(projectId, itemId, request);
	}

	@PostMapping(path = "/{projectId}/execution-items/{itemId}/comparisons/{comparisonId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApprovalResponse.Attachment uploadVendorComparisonAttachment(
			@PathVariable("projectId") String projectId,
			@PathVariable("itemId") String itemId,
			@PathVariable("comparisonId") String comparisonId,
			@RequestParam("file") MultipartFile file
	) {
		return vendorComparisonService.uploadAttachment(projectId, itemId, comparisonId, file);
	}

	@GetMapping("/{projectId}/execution-items/{itemId}/comparison-attachments/{attachmentId}")
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
	public PurchaseOrderDraftResponse getPurchaseOrderDraft(
			@PathVariable("projectId") String projectId,
			@PathVariable("itemId") String itemId
	) {
		return vendorComparisonService.getPurchaseOrderDraft(projectId, itemId);
	}

	@PostMapping("/{projectId}/purchase-orders/draft/{itemId}/request")
	public PurchaseOrderDraftResponse requestPurchaseOrder(
			@PathVariable("projectId") String projectId,
			@PathVariable("itemId") String itemId
	) {
		return vendorComparisonService.requestPurchaseOrder(projectId, itemId);
	}

	@GetMapping("/{projectId}/progress-payments")
	public ProgressPaymentResponse getProgressPayments(@PathVariable("projectId") String projectId) {
		return progressPaymentService.getStatus(projectId);
	}

	@GetMapping("/{projectId}/purchase-orders")
	public PurchaseOrderResponse getPurchaseOrders(@PathVariable("projectId") String projectId) {
		return purchaseOrderService.getPurchaseOrders(projectId);
	}

	@PostMapping("/{projectId}/payment-requests")
	@ResponseStatus(HttpStatus.CREATED)
	public void requestPayments(@PathVariable("projectId") String projectId, @RequestBody PaymentRequestCreateRequest request) {
		paymentRequestService.requestPayments(projectId, request);
	}

	@GetMapping("/{projectId}/settlement")
	public ProjectSettlementResponse getSettlement(@PathVariable("projectId") String projectId) {
		return projectSettlementService.getSettlement(projectId);
	}

	@PostMapping("/{projectId}/settlement/complete")
	public ProjectSettlementResponse completeSettlement(
			@PathVariable("projectId") String projectId,
			@RequestBody ProjectSettlementCompleteRequest request
	) {
		return projectSettlementService.completeSettlement(projectId, request);
	}

	@PostMapping("/{projectId}/progress-claims")
	public ProgressPaymentResponse createProgressClaim(
			@PathVariable("projectId") String projectId,
			@RequestBody ProgressPaymentRequest.Claim request
	) {
		return progressPaymentService.createClaim(projectId, request);
	}

	@PostMapping("/{projectId}/progress-claims/{claimId}/payments")
	public ProgressPaymentResponse createPayment(
			@PathVariable("projectId") String projectId,
			@PathVariable("claimId") String claimId,
			@RequestBody ProgressPaymentRequest.Payment request
	) {
		return progressPaymentService.createPayment(projectId, claimId, request);
	}
}
