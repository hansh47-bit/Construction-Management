package com.construction.management.account;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Table(name="security_audit_logs") @Getter @NoArgsConstructor
public class SecurityAuditLog {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 String actorUserId; String actorName; String actorPosition;
 String action; String targetType; String targetId;
 @Column(length=16000) String beforeValue;
 @Column(length=16000) String afterValue;
 @Column(length=1000) String reason;
 String result; LocalDateTime createdAt=LocalDateTime.now();
 String requestSource; String requestId;
 public SecurityAuditLog(String actorId,String name,String position,String action,String type,String target,String before,String after,String reason,String result,String source,String requestId) {
 this.actorUserId=actorId; this.actorName=name; this.actorPosition=position; this.action=action; this.targetType=type; this.targetId=target; this.beforeValue=before; this.afterValue=after; this.reason=reason; this.result=result; this.requestSource=source; this.requestId=requestId;
 }
}
