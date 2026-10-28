package com.construction.management.auth;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
public class ProjectAccessController {
 private final BusinessPolicy policy;
 public ProjectAccessController(BusinessPolicy policy) { this.policy=policy; }
 @GetMapping("/api/v1/projects/{projectId}/access") public Object access(@PathVariable String projectId,@RequestParam(required=false) String itemId) {
  policy.require("PROJECT_READ"); var result=new LinkedHashMap<String,Boolean>();
  for(String kind:List.of("CONTRACT","BUDGET","PROCUREMENT")) result.put(kind+"_WRITE",policy.allow(kind+"_WRITE") && policy.canDraft(kind,projectId,kind.equals("PROCUREMENT")?itemId:null));
  return result;
 }
}
