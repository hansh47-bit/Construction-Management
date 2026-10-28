package com.construction.management.account;
import java.util.*;
public enum Permission {
 PROJECT_READ("프로젝트·통합현황 조회", null), DASHBOARD_READ("대표 대시보드", "PROJECT_READ"),
 PROJECT_CREATE("프로젝트 등록", "PROJECT_READ"), PROJECT_DELETE("프로젝트 삭제", "PROJECT_READ"), PROJECT_STATUS("공사 상태 전환", "PROJECT_READ"),
 CONTRACT_READ("계약 조회", "PROJECT_READ"), CONTRACT_WRITE("계약 초안 작성·수정", "CONTRACT_READ"),
 BUDGET_READ("실행예산 조회", "PROJECT_READ"), BUDGET_WRITE("실행예산 작성·수정", "BUDGET_READ"), BUDGET_REQUEST("실행예산 결재 요청", "BUDGET_READ"),
 PROCUREMENT_READ("비교견적·발주 조회", "PROJECT_READ"), PROCUREMENT_WRITE("비교견적 작성·수정", "PROCUREMENT_READ"), PROCUREMENT_REQUEST("발주 결재 요청", "PROCUREMENT_READ"),
 CHANGE_READ("변경 이력 조회", "PROJECT_READ"), CHANGE_WRITE("변경 초안 작성·수정", "CHANGE_READ"), CHANGE_REQUEST("변경 결재 요청", "CHANGE_READ"),
 PROGRESS_READ("기성·지급 조회", "PROJECT_READ"), PROGRESS_WRITE("기성 등록·삭제·결재 요청", "PROGRESS_READ"), PAYMENT_REQUEST("지급 요청 작성", "PROGRESS_READ"),
 PAYMENT_EXECUTE("실제 지급 등록 (재무 전용)", "PROGRESS_READ"),
 SETTLEMENT_READ("준공정산 조회", "PROJECT_READ"), SETTLEMENT_REQUEST("준공정산 작성·결재 요청", "SETTLEMENT_READ"),
 SITE_READ("현장 기록 조회", "PROJECT_READ"), SITE_WRITE("일지·회의록 작성·수정", "SITE_READ"),
 APPROVAL_READ("본인 결재 조회", "PROJECT_READ"), APPROVAL_ALL("전체 통합결재함", "APPROVAL_READ"),
 APPROVAL_DECIDE("최종 결재 (대표 전용)", "APPROVAL_READ"), EXECUTIVE_CORRECT("금액 정정 (대표 전용)", "CHANGE_READ");
 public final String label; public final String dependency;
 Permission(String label,String dependency) { this.label=label; this.dependency=dependency; }
 public boolean locked() { return this==PAYMENT_EXECUTE || this==APPROVAL_DECIDE || this==EXECUTIVE_CORRECT; }
 public boolean defaultAllowed(Position p) {
  if (this==PAYMENT_EXECUTE) return p==Position.FINANCE;
  if (this==APPROVAL_DECIDE || this==EXECUTIVE_CORRECT) return p==Position.CEO;
  if (this==DASHBOARD_READ || this==APPROVAL_ALL) return p==Position.CEO || p==Position.FINANCE;
  if (this==PROJECT_CREATE || this==PROJECT_DELETE) return p==Position.CEO;
  if (this==SETTLEMENT_REQUEST) return p!=Position.STAFF;
  if (this==SITE_WRITE) return p!=Position.FINANCE;
  return true;
 }
}
