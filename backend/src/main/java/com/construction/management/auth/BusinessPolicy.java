package com.construction.management.auth;
import com.construction.management.account.*;
import com.construction.management.project.domain.*;
import jakarta.persistence.*;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.security.access.AccessDeniedException;
@Component
public class BusinessPolicy {
 private final PermissionService permissions; private final CurrentActor actor; private final EntityManager em; private final DraftOwnershipRepository owners;
 public BusinessPolicy(PermissionService permissions,CurrentActor actor,EntityManager em,DraftOwnershipRepository owners) { this.permissions=permissions; this.actor=actor; this.em=em; this.owners=owners; }
 public boolean allow(String key) { return permissions.has(Permission.valueOf(key)); }
 public void require(String key) { permissions.require(Permission.valueOf(key)); }
 public boolean canSeeApproval(Approval a) { return permissions.has(Permission.APPROVAL_ALL) || Objects.equals(a.getCreatedUserId(),CurrentActor.id()); }
 public void approval(String id) { Approval a=em.find(Approval.class,id); if(a==null || !canSeeApproval(a)) throw new EntityNotFoundException("자료를 찾을 수 없습니다."); }
 public void ownEntity(Class<? extends AuthoredEntity> type,String id) {
  AuthoredEntity entity=em.find(type,id); if(entity==null) throw new EntityNotFoundException("자료를 찾을 수 없습니다.");
  if(actor.user().getPosition()==Position.STAFF && !Objects.equals(entity.getCreatedUserId(),CurrentActor.id())) throw new EntityNotFoundException("본인 자료만 수정할 수 있습니다.");
 }
 public void lockProject(String id) { if(em.find(Project.class,id,LockModeType.PESSIMISTIC_WRITE)==null) throw new EntityNotFoundException("프로젝트를 찾을 수 없습니다."); }
 public void draft(String kind,String projectId,String itemId) {
  Project project=em.find(Project.class,projectId,LockModeType.PESSIMISTIC_WRITE); if(project==null) throw new EntityNotFoundException("프로젝트를 찾을 수 없습니다.");
  String id=draftKey(kind,projectId,itemId);
  DraftOwnership owner=owners.findById(id).orElse(null);
  if(owner==null) { boolean legacy=legacy(kind,projectId,itemId); owner=new DraftOwnership(id,legacy); if(legacy && actor.user().getPosition()==Position.STAFF) throw new EntityNotFoundException("기존 기록은 팀장·대표가 편집할 수 있습니다."); owners.save(owner); }
  if(actor.user().getPosition()==Position.STAFF && !Objects.equals(owner.ownerUserId,CurrentActor.id())) throw new EntityNotFoundException("본인 자료만 수정할 수 있습니다.");
 }
 private boolean legacy(String kind,String projectId,String itemId) {
  String entity=switch(kind) { case "CONTRACT" -> "CustomerContractItem"; case "BUDGET" -> "ProjectExecutionBudgetDetail"; case "PROCUREMENT" -> "VendorComparison"; default -> throw new IllegalArgumentException("알 수 없는 초안입니다."); };
  var q=em.createQuery("select count(e) from "+entity+" e where e.projectId=:p"+(itemId==null?"":" and e.executionItemId=:item")+(kind.equals("PROCUREMENT") ? (procurementRevision(projectId)==null ? " and e.revisionId is null" : " and e.revisionId=:revision") : ""),Long.class).setParameter("p",projectId);
  if(itemId!=null) q.setParameter("item",itemId); if(kind.equals("PROCUREMENT") && procurementRevision(projectId)!=null) q.setParameter("revision",procurementRevision(projectId)); return q.getSingleResult()>0;
 }
 private String procurementRevision(String projectId) { return em.createQuery("select r.revisionId from ProjectRevision r where r.projectId=:p and r.status='APPROVED' order by r.revisionNumber desc",String.class).setParameter("p",projectId).setMaxResults(1).getResultStream().findFirst().orElse(null); }
 private String draftKey(String kind,String projectId,String itemId) { return kind+":"+projectId+(itemId==null?"":":"+itemId)+(kind.equals("PROCUREMENT") ? ":"+String.valueOf(procurementRevision(projectId)) : ""); }
 public boolean canDraft(String kind,String projectId,String itemId) {
  if(actor.user().getPosition()!=Position.STAFF) return true;
  String id=draftKey(kind,projectId,itemId); var o=owners.findById(id);
  return o.map(v -> Objects.equals(v.ownerUserId,CurrentActor.id())).orElseGet(() -> !legacy(kind,projectId,itemId));
 }
 public void contractUnlocked(String projectId) {
  Project p=em.find(Project.class,projectId); if(p==null) throw new EntityNotFoundException("프로젝트를 찾을 수 없습니다.");
  long n=em.createQuery("select count(a) from Approval a where a.projectId=:p and (a.status=com.construction.management.project.domain.ApprovalStatus.APPROVED or a.status=com.construction.management.project.domain.ApprovalStatus.REQUESTED)",Long.class).setParameter("p",projectId).getSingleResult();
  if(n>0 || !java.util.Set.of(ProjectStatus.견적,ProjectStatus.계약).contains(p.getStatus()) || em.createQuery("select count(c) from ProjectContract c where c.projectId=:p",Long.class).setParameter("p",projectId).getSingleResult()>0) throw new IllegalStateException("결재 요청·승인된 프로젝트의 원본 계약은 변경할 수 없습니다. 변경관리에서 증감을 등록하세요.");
 }
 public void deletableProject(String projectId) {
  long n=em.createQuery("select count(a) from Approval a where a.projectId=:p",Long.class).setParameter("p",projectId).getSingleResult();
  long po=em.createQuery("select count(p) from PurchaseOrder p where p.projectId=:p",Long.class).setParameter("p",projectId).getSingleResult();
  long contract=em.createQuery("select count(c) from ProjectContract c where c.projectId=:p",Long.class).setParameter("p",projectId).getSingleResult();
  if(n>0 || po>0 || contract>0) throw new IllegalStateException("결재·발주·최초 계약 이력이 있는 프로젝트는 삭제할 수 없습니다.");
 }
}
