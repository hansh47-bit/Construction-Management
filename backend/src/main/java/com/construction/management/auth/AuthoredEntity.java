package com.construction.management.auth;
import jakarta.persistence.*;
@MappedSuperclass
public abstract class AuthoredEntity {
 @Column(name="created_user_id",updatable=false) private String createdUserId;
 @Column(name="created_user_name",updatable=false) private String createdUserName;
 private String updatedUserId;
 private String updatedUserName;
 protected AuthoredEntity() { captureCreator(); }
 @PrePersist protected void captureCreator() { if(CurrentActor.optionalId()!=null) { createdUserId=CurrentActor.id(); createdUserName=CurrentActor.name(); } }
 @PreUpdate protected void captureEditor() { if(CurrentActor.optionalId()!=null) { updatedUserId=CurrentActor.id(); updatedUserName=CurrentActor.name(); } }
 public String getCreatedUserId() { return createdUserId; }
 public String getCreatedUserName() { return createdUserName; }
}
