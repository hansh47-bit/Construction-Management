package com.construction.management.dashboard;

import com.construction.management.project.api.IntegratedSummaryResponse;
import com.construction.management.project.domain.Approval;
import com.construction.management.project.domain.ApprovalStatus;
import com.construction.management.project.domain.Project;
import com.construction.management.project.repository.ApprovalRepository;
import com.construction.management.project.repository.ProjectRepository;
import com.construction.management.project.service.ProjectService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Executive dashboard values are calculated from the same project summaries shown in Project Single View. */
@Service
@org.springframework.security.access.prepost.PreAuthorize("@businessPolicy.allow('DASHBOARD_READ')")
public class DashboardService {
	private final ProjectRepository projectRepository;
	private final ApprovalRepository approvalRepository;
	private final ProjectService projectService;

	public DashboardService(ProjectRepository projectRepository, ApprovalRepository approvalRepository, ProjectService projectService) {
		this.projectRepository = projectRepository;
		this.approvalRepository = approvalRepository;
		this.projectService = projectService;
	}

	public DashboardResponse getDashboard() {
		List<Project> projects = projectRepository.findAll();
		List<Approval> requested = approvalRepository.findByStatusOrderByRequestedAtDesc(ApprovalStatus.REQUESTED);
		List<Approval> ceoRequested = requested.stream()
				.filter(approval -> approval.getApprovalStage() == com.construction.management.project.domain.ApprovalStage.CEO)
				.toList();
		List<IntegratedSummaryResponse> summaries = projects.stream().map(project -> projectService.getIntegratedSummary(project.getProjectId())).toList();
		BigDecimal expectedProfit = summaries.stream().map(summary -> summary.profitAndBalance().expectedProfit()).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal expectedCost = summaries.stream().map(summary -> summary.profitAndBalance().expectedFinalCost()).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal currentContract = summaries.stream().map(summary -> summary.flowSummary().currentContractAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal unpaid = summaries.stream().map(summary -> summary.profitAndBalance().totalUnpaidBalance()).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal margin = currentContract.signum() == 0 ? BigDecimal.ZERO : expectedProfit.multiply(BigDecimal.valueOf(100)).divide(currentContract, 1, RoundingMode.HALF_UP);
		return new DashboardResponse(
				List.of(
						new MetricResponse("진행 프로젝트", String.valueOf(projects.stream().filter(project -> !"정산완료".equals(project.getStatus().name())).count()), "전체 " + projects.size() + "개 프로젝트"),
						new MetricResponse("대표 결재 대기", String.valueOf(ceoRequested.size()) + "건", "팀장 검토를 마친 건과 대표 직행 결재"),
						new MetricResponse("예상 이익률", margin + "%", "예상 이익 " + won(expectedProfit) + " / 예상 원가 " + won(expectedCost)),
						new MetricResponse("총 미지급 잔액", won(unpaid), "승인 기성 및 향후 지급 포함")
				),
				getWorkflow(projects, requested),
				ceoRequested.stream().limit(10).map(approval -> new ApprovalResponse(approval.getApprovalId(), approval.getTitle(), approval.getRequestUser(), won(approval.getRequestedAmount()), "대표 결재 대기")).toList()
		);
	}

	public List<WorkflowStepResponse> getWorkflow() {
		return getWorkflow(projectRepository.findAll(), approvalRepository.findByStatusOrderByRequestedAtDesc(ApprovalStatus.REQUESTED));
	}

	private List<WorkflowStepResponse> getWorkflow(List<Project> projects, List<Approval> requested) {
		long active = projects.stream().filter(project -> "공사진행".equals(project.getStatus().name())).count();
		return List.of(
				new WorkflowStepResponse(1, "1단계", "프로젝트·계약", "공사부", projects.isEmpty() ? "READY" : "IN_PROGRESS"),
				new WorkflowStepResponse(2, "1단계", "실행예산", "견적팀", requested.stream().anyMatch(approval -> "EXECUTION_BUDGET".equals(approval.getApprovalType())) ? "PENDING" : "READY"),
				new WorkflowStepResponse(3, "2단계", "업체 비교견적·발주", "현장 소장", requested.stream().anyMatch(approval -> "PURCHASE_ORDER".equals(approval.getApprovalType()) || "PO_CHANGE".equals(approval.getApprovalType())) ? "PENDING" : "READY"),
				new WorkflowStepResponse(4, "3단계", "변경관리", "대표", requested.stream().anyMatch(approval -> approval.getApprovalType().endsWith("_CHANGE")) ? "PENDING" : "READY"),
				new WorkflowStepResponse(5, "4단계", "기성·지급·정산", "공무/경리부", active > 0 ? "IN_PROGRESS" : "READY")
		);
	}

	private static String won(BigDecimal amount) { return NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원"; }
}
