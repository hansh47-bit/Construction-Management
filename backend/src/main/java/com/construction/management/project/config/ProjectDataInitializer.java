package com.construction.management.project.config;

import com.construction.management.project.domain.ActualPayment;
import com.construction.management.project.domain.ApprovalStatus;
import com.construction.management.project.domain.BudgetChange;
import com.construction.management.project.domain.ContractChange;
import com.construction.management.project.domain.CustomerContractItem;
import com.construction.management.project.domain.ExecutionBudget;
import com.construction.management.project.domain.PoChange;
import com.construction.management.project.domain.ProgressClaim;
import com.construction.management.project.domain.Project;
import com.construction.management.project.domain.ProjectExecutionBudgetDetail;
import com.construction.management.project.domain.ProjectExecutionBudgetItem;
import com.construction.management.project.domain.ProjectContract;
import com.construction.management.project.domain.ProjectStatus;
import com.construction.management.project.domain.PurchaseOrder;
import com.construction.management.project.repository.ActualPaymentRepository;
import com.construction.management.project.repository.BudgetChangeRepository;
import com.construction.management.project.repository.ContractChangeRepository;
import com.construction.management.project.repository.CustomerContractItemRepository;
import com.construction.management.project.repository.ExecutionBudgetRepository;
import com.construction.management.project.repository.PoChangeRepository;
import com.construction.management.project.repository.ProgressClaimRepository;
import com.construction.management.project.repository.ProjectContractRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetDetailRepository;
import com.construction.management.project.repository.ProjectExecutionBudgetItemRepository;
import com.construction.management.project.repository.ProjectRepository;
import com.construction.management.project.repository.PurchaseOrderRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ProjectDataInitializer implements CommandLineRunner {

	private final ProjectRepository projectRepository;
	private final ProjectContractRepository projectContractRepository;
	private final ContractChangeRepository contractChangeRepository;
	private final CustomerContractItemRepository customerContractItemRepository;
	private final ExecutionBudgetRepository executionBudgetRepository;
	private final BudgetChangeRepository budgetChangeRepository;
	private final PurchaseOrderRepository purchaseOrderRepository;
	private final PoChangeRepository poChangeRepository;
	private final ProgressClaimRepository progressClaimRepository;
	private final ActualPaymentRepository actualPaymentRepository;
	private final ProjectExecutionBudgetItemRepository executionBudgetItemRepository;
	private final ProjectExecutionBudgetDetailRepository executionBudgetDetailRepository;

	public ProjectDataInitializer(
			ProjectRepository projectRepository,
			ProjectContractRepository projectContractRepository,
			ContractChangeRepository contractChangeRepository,
			CustomerContractItemRepository customerContractItemRepository,
			ExecutionBudgetRepository executionBudgetRepository,
			BudgetChangeRepository budgetChangeRepository,
			PurchaseOrderRepository purchaseOrderRepository,
			PoChangeRepository poChangeRepository,
			ProgressClaimRepository progressClaimRepository,
			ActualPaymentRepository actualPaymentRepository,
			ProjectExecutionBudgetItemRepository executionBudgetItemRepository,
			ProjectExecutionBudgetDetailRepository executionBudgetDetailRepository
	) {
		this.projectRepository = projectRepository;
		this.projectContractRepository = projectContractRepository;
		this.contractChangeRepository = contractChangeRepository;
		this.customerContractItemRepository = customerContractItemRepository;
		this.executionBudgetRepository = executionBudgetRepository;
		this.budgetChangeRepository = budgetChangeRepository;
		this.purchaseOrderRepository = purchaseOrderRepository;
		this.poChangeRepository = poChangeRepository;
		this.progressClaimRepository = progressClaimRepository;
		this.actualPaymentRepository = actualPaymentRepository;
		this.executionBudgetItemRepository = executionBudgetItemRepository;
		this.executionBudgetDetailRepository = executionBudgetDetailRepository;
	}

	@Override
	public void run(String... args) {
		if (projectRepository.existsById("prj_2026_001")) {
			return;
		}

		Project project = new Project("prj_2026_001", "P-2026-001", "성가수녀원 리뉴얼공사")
				.updateDetails(
						"성가수녀원",
						"서울시 성북구",
						LocalDate.of(2026, 3, 1),
						LocalDate.of(2026, 3, 10),
						LocalDate.of(2026, 8, 31),
						"usr_tl_01",
						"usr_pm_02",
						ProjectStatus.공사진행,
						BigDecimal.valueOf(45),
						"1단계 프로젝트 등록 및 통합현황 샘플"
				);
		projectRepository.save(project);

		projectContractRepository.save(new ProjectContract("ctr_2026_001", "prj_2026_001", BigDecimal.valueOf(200_000_000), BigDecimal.valueOf(20_000_000)));
		contractChangeRepository.save(new ContractChange("cchg_2026_001", "prj_2026_001", "CHG-2026-001", BigDecimal.valueOf(10_000_000), ApprovalStatus.APPROVED));

		customerContractItemRepository.save(new CustomerContractItem("item_001", "prj_2026_001", "인테리어", BigDecimal.valueOf(150_000_000), BigDecimal.valueOf(140_223_762), BigDecimal.valueOf(14_022_376), "내부 집구조 리뉴얼"));
		customerContractItemRepository.save(new CustomerContractItem("item_002", "prj_2026_001", "전기", BigDecimal.valueOf(25_000_000), BigDecimal.valueOf(22_691_553), BigDecimal.valueOf(2_269_155), "조명 및 배선 공사"));
		customerContractItemRepository.save(new CustomerContractItem("item_003", "prj_2026_001", "냉난방", BigDecimal.valueOf(52_000_000), BigDecimal.valueOf(47_084_685), BigDecimal.valueOf(4_708_469), "공조 및 냉난방 설비"));

		executionBudgetRepository.save(new ExecutionBudget("bdg_001", "prj_2026_001", "철거", BigDecimal.valueOf(20_000_000)));
		executionBudgetRepository.save(new ExecutionBudget("bdg_002", "prj_2026_001", "목공", BigDecimal.valueOf(38_000_000)));
		executionBudgetRepository.save(new ExecutionBudget("bdg_003", "prj_2026_001", "전기", BigDecimal.valueOf(24_000_000)));
		executionBudgetRepository.save(new ExecutionBudget("bdg_004", "prj_2026_001", "설비", BigDecimal.valueOf(30_700_000)));
		executionBudgetRepository.save(new ExecutionBudget("bdg_005", "prj_2026_001", "잡자재/예비비", BigDecimal.valueOf(2_800_000)));
		budgetChangeRepository.save(new BudgetChange("bchg_001", "bdg_002", "BCHG-2026-001", BigDecimal.ZERO, ApprovalStatus.APPROVED));

		executionBudgetItemRepository.save(new ProjectExecutionBudgetItem("EX-001", "prj_2026_001", "인테리어공사", "철거/폐기물"));
		executionBudgetItemRepository.save(new ProjectExecutionBudgetItem("EX-002", "prj_2026_001", "인테리어공사", "목공사"));
		executionBudgetItemRepository.save(new ProjectExecutionBudgetItem("EX-008", "prj_2026_001", "공통", "현장경비/직영"));
		executionBudgetDetailRepository.save(new ProjectExecutionBudgetDetail("dtl_001", "prj_2026_001", "EX-001", "인테리어공사", "철거/폐기물", "외주비", "철거업체 견적", "식", BigDecimal.ONE, BigDecimal.valueOf(3_500_000), "", "예시"));
		executionBudgetDetailRepository.save(new ProjectExecutionBudgetDetail("dtl_002", "prj_2026_001", "EX-001", "인테리어공사", "철거/폐기물", "폐기물", "혼합폐기물 반출", "대", BigDecimal.valueOf(2), BigDecimal.valueOf(900_000), "", "예시"));
		executionBudgetDetailRepository.save(new ProjectExecutionBudgetDetail("dtl_003", "prj_2026_001", "EX-001", "인테리어공사", "철거/폐기물", "인건비", "보양·현장정리", "인", BigDecimal.valueOf(4), BigDecimal.valueOf(180_000), "", "예시"));
		executionBudgetItemRepository.findById("EX-001").ifPresent(item -> {
			item.updateBudgetAmount(BigDecimal.valueOf(6_020_000));
			executionBudgetItemRepository.save(item);
		});

		purchaseOrderRepository.save(new PurchaseOrder("po_001", "prj_2026_001", "bdg_001", "대한철거", BigDecimal.valueOf(19_500_000)));
		purchaseOrderRepository.save(new PurchaseOrder("po_002", "prj_2026_001", "bdg_002", "한빛목공", BigDecimal.valueOf(37_000_000)));
		purchaseOrderRepository.save(new PurchaseOrder("po_003", "prj_2026_001", "bdg_003", "서울전기", BigDecimal.valueOf(23_700_000)));
		purchaseOrderRepository.save(new PurchaseOrder("po_004", "prj_2026_001", "bdg_004", "청명설비", BigDecimal.valueOf(30_000_000)));
		poChangeRepository.save(new PoChange("pochg_001", "po_004", "PCHG-2026-001", BigDecimal.ZERO, ApprovalStatus.APPROVED));

		progressClaimRepository.save(new ProgressClaim("clm_001", "po_001", 1, BigDecimal.valueOf(18_000_000), ApprovalStatus.APPROVED));
		progressClaimRepository.save(new ProgressClaim("clm_002", "po_002", 1, BigDecimal.valueOf(28_000_000), ApprovalStatus.APPROVED));
		progressClaimRepository.save(new ProgressClaim("clm_003", "po_003", 1, BigDecimal.valueOf(16_000_000), ApprovalStatus.APPROVED));
		progressClaimRepository.save(new ProgressClaim("clm_004", "po_004", 1, BigDecimal.valueOf(18_000_000), ApprovalStatus.APPROVED));

		actualPaymentRepository.save(new ActualPayment("pay_001", "clm_001", BigDecimal.valueOf(18_000_000), LocalDate.of(2026, 4, 30)));
		actualPaymentRepository.save(new ActualPayment("pay_002", "clm_002", BigDecimal.valueOf(24_000_000), LocalDate.of(2026, 5, 10)));
		actualPaymentRepository.save(new ActualPayment("pay_003", "clm_003", BigDecimal.valueOf(14_000_000), LocalDate.of(2026, 5, 15)));
		actualPaymentRepository.save(new ActualPayment("pay_004", "clm_004", BigDecimal.valueOf(14_000_000), LocalDate.of(2026, 5, 20)));
	}
}
