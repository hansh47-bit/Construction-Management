package com.construction.management.auth;
import jakarta.persistence.*;
@Entity @Table(name="business_draft_owners")
public class DraftOwnership {
 @Id String id;
 String ownerUserId;
 String ownerName;
 protected DraftOwnership() {}
 public DraftOwnership(String id,boolean legacy) { this.id=id; if(!legacy) { ownerUserId=CurrentActor.id(); ownerName=CurrentActor.name(); } }
}
