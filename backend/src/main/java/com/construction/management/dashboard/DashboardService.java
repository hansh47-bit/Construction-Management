package com.construction.management.dashboard;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

	public DashboardResponse getDashboard() {
		return new DashboardResponse(getMetrics(), getWorkflow(), getApprovals());
	}

	public List<WorkflowStepResponse> getWorkflow() {
		return List.of(
				new WorkflowStepResponse(1, "1단계", "프로젝트 등록", "공사부", "READY"),
				new WorkflowStepResponse(2, "1단계", "고객 견적/계약 등록", "견적팀", "READY"),
				new WorkflowStepResponse(3, "1단계", "세부 실행원가 산출", "견적팀", "PENDING"),
				new WorkflowStepResponse(4, "1단계", "실행예산 작성 및 대표 결재", "대표", "BLOCKED"),
				new WorkflowStepResponse(5, "2단계", "업체 비교견적 및 발주품의", "현장 소장", "PLANNED"),
				new WorkflowStepResponse(6, "3단계", "변경계약 및 실행예산 변경", "현장 소장", "PLANNED"),
				new WorkflowStepResponse(7, "4단계", "기성/잔금 정산", "공무/경리부", "PLANNED")
		);
	}

	private List<MetricResponse> getMetrics() {
		return List.of(
				new MetricResponse("진행 프로젝트", "0", "등록 대기"),
				new MetricResponse("대표 결재 대기", "0", "대표 전용 화면으로 분리 예정"),
				new MetricResponse("목표 마진", "15~20%", "실행예산 확정 전 검증"),
				new MetricResponse("미확정 공종 한도", "3%", "잡자재 및 예비비 통제")
		);
	}

	private List<ApprovalResponse> getApprovals() {
		return List.of(
				new ApprovalResponse("APR-001", "실행예산 확정", "견적팀", "0원", "대기"),
				new ApprovalResponse("APR-002", "발주품의 대표 결재", "현장 소장", "0원", "예정")
		);
	}
}
