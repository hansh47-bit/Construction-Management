package com.construction.management.project.api;

import com.construction.management.project.service.ApprovalService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {
	private final ApprovalService approvalService;

	public ApprovalController(ApprovalService approvalService) {
		this.approvalService = approvalService;
	}

	@GetMapping
	public List<ApprovalResponse> search(
			@RequestParam(name = "status", required = false) String status,
			@RequestParam(name = "type", required = false) String type,
			@RequestParam(name = "projectId", required = false) String projectId
	) {
		return approvalService.search(status, type, projectId);
	}

	@GetMapping("/{approvalId}")
	public ApprovalResponse get(@PathVariable("approvalId") String approvalId) {
		return approvalService.getApproval(approvalId);
	}

	@PostMapping("/{approvalId}/approve")
	public ApprovalResponse approve(
			@PathVariable("approvalId") String approvalId,
			@RequestBody(required = false) ApprovalDecisionRequest request
	) {
		return approvalService.approve(approvalId, request);
	}

	@PostMapping("/{approvalId}/reject")
	public ApprovalResponse reject(
			@PathVariable("approvalId") String approvalId,
			@RequestBody ApprovalDecisionRequest request
	) {
		return approvalService.reject(approvalId, request);
	}
}
