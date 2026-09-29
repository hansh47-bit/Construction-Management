package com.construction.management.project.api;

import com.construction.management.project.domain.ContractFile;
import com.construction.management.project.service.CustomerContractService;
import com.construction.management.project.service.ExecutionBudgetDetailService;
import com.construction.management.project.service.ProjectService;
import com.construction.management.project.service.ProjectSettlementService;
import com.construction.management.project.service.ProgressPaymentService;
import com.construction.management.project.service.VendorComparisonService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

	private final ProjectService projectService;
	private final CustomerContractService customerContractService;
	private final ExecutionBudgetDetailService executionBudgetDetailService;
	private final VendorComparisonService vendorComparisonService;
	private final ProgressPaymentService progressPaymentService;
	private final ProjectSettlementService projectSettlementService;

	public ProjectController(
			ProjectService projectService,
			CustomerContractService customerContractService,
			ExecutionBudgetDetailService executionBudgetDetailService,
			VendorComparisonService vendorComparisonService,
			ProgressPaymentService progressPaymentService,
			ProjectSettlementService projectSettlementService
	) {
		this.projectService = projectService;
		this.customerContractService = customerContractService;
		this.executionBudgetDetailService = executionBudgetDetailService;
		this.vendorComparisonService = vendorComparisonService;
		this.progressPaymentService = progressPaymentService;
		this.projectSettlementService = projectSettlementService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProjectResponse create(@Valid @RequestBody ProjectCreateRequest request) {
		return projectService.create(request);
	}

	@GetMapping
	public List<ProjectResponse> search(@RequestParam(required = false) String keyword) {
		return projectService.search(keyword);
	}

	@GetMapping("/{projectId}/integrated-summary")
	public IntegratedSummaryResponse getIntegratedSummary(@PathVariable String projectId) {
		return projectService.getIntegratedSummary(projectId);
	}

	@GetMapping("/{projectId}/contracts")
	public CustomerContractResponse getCustomerContract(@PathVariable String projectId) {
		return customerContractService.getContract(projectId);
	}

	@PutMapping("/{projectId}/contracts")
	public CustomerContractResponse saveCustomerContract(
			@PathVariable String projectId,
			@Valid @RequestBody CustomerContractRequest request
	) {
		return customerContractService.saveContract(projectId, request);
	}

	@PostMapping(path = "/{projectId}/contract-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public CustomerContractResponse uploadContractFile(
			@PathVariable String projectId,
			@RequestParam(defaultValue = "CONTRACT") String fileType,
			@RequestParam MultipartFile file
	) {
		return customerContractService.uploadFile(projectId, fileType, file);
	}

	@GetMapping("/{projectId}/contract-files/{fileId}")
	public ResponseEntity<Resource> downloadContractFile(@PathVariable String projectId, @PathVariable String fileId) {
		ContractFile file = customerContractService.getFile(projectId, fileId);
		Resource resource = customerContractService.loadFileResource(file);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(file.getFileName())
						.build()
						.toString())
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(resource);
	}

	@GetMapping("/{projectId}/execution-budget-details")
	public ExecutionBudgetDetailResponse getExecutionBudgetDetails(@PathVariable String projectId) {
		return executionBudgetDetailService.getDetails(projectId);
	}

	@PutMapping("/{projectId}/execution-budget-details")
	public ExecutionBudgetDetailResponse saveExecutionBudgetDetails(
			@PathVariable String projectId,
			@Valid @RequestBody ExecutionBudgetDetailRequest request
	) {
		return executionBudgetDetailService.saveDetails(projectId, request);
	}

	@GetMapping("/{projectId}/execution-items/{itemId}/comparisons")
	public VendorComparisonResponse getVendorComparisons(@PathVariable String projectId, @PathVariable String itemId) {
		return vendorComparisonService.getComparisons(projectId, itemId);
	}

	@PostMapping("/{projectId}/execution-items/{itemId}/comparisons")
	public VendorComparisonResponse saveVendorComparisons(
			@PathVariable String projectId,
			@PathVariable String itemId,
			@Valid @RequestBody VendorComparisonRequest request
	) {
		return vendorComparisonService.saveComparisons(projectId, itemId, request);
	}

	@GetMapping("/{projectId}/purchase-orders/draft/{itemId}")
	public PurchaseOrderDraftResponse getPurchaseOrderDraft(@PathVariable String projectId, @PathVariable String itemId) {
		return vendorComparisonService.getPurchaseOrderDraft(projectId, itemId);
	}

	@PostMapping("/{projectId}/purchase-orders/draft/{itemId}/request")
	public PurchaseOrderDraftResponse requestPurchaseOrder(@PathVariable String projectId, @PathVariable String itemId) {
		return vendorComparisonService.requestPurchaseOrder(projectId, itemId);
	}

	@GetMapping("/{projectId}/progress-payments")
	public ProgressPaymentResponse getProgressPayments(@PathVariable String projectId) {
		return progressPaymentService.getStatus(projectId);
	}

	@GetMapping("/{projectId}/settlement")
	public ProjectSettlementResponse getSettlement(@PathVariable String projectId) {
		return projectSettlementService.getSettlement(projectId);
	}

	@PostMapping("/{projectId}/settlement/complete")
	public ProjectSettlementResponse completeSettlement(
			@PathVariable String projectId,
			@RequestBody ProjectSettlementCompleteRequest request
	) {
		return projectSettlementService.completeSettlement(projectId, request);
	}

	@PostMapping("/{projectId}/progress-claims")
	public ProgressPaymentResponse createProgressClaim(
			@PathVariable String projectId,
			@RequestBody ProgressPaymentRequest.Claim request
	) {
		return progressPaymentService.createClaim(projectId, request);
	}

	@PostMapping("/{projectId}/progress-claims/{claimId}/approve")
	public ProgressPaymentResponse approveProgressClaim(@PathVariable String projectId, @PathVariable String claimId) {
		return progressPaymentService.approveClaim(projectId, claimId);
	}

	@PostMapping("/{projectId}/progress-claims/{claimId}/payments")
	public ProgressPaymentResponse createPayment(
			@PathVariable String projectId,
			@PathVariable String claimId,
			@RequestBody ProgressPaymentRequest.Payment request
	) {
		return progressPaymentService.createPayment(projectId, claimId, request);
	}
}
