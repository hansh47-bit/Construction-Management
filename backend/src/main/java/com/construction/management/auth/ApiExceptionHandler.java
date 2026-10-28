package com.construction.management.auth;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
@RestControllerAdvice
public class ApiExceptionHandler {
 private final com.construction.management.account.AuditService audit; private final CurrentActor actor;
 public ApiExceptionHandler(com.construction.management.account.AuditService audit, CurrentActor actor) { this.audit=audit; this.actor=actor; }
 @ExceptionHandler(ApiException.class) ResponseEntity<?> api(ApiException e) { return error(e.getStatus(),e.getCode(),e.getMessage()); }
 @ExceptionHandler(AuthenticationException.class) ResponseEntity<?> auth(AuthenticationException e) { return error(401,"UNAUTHENTICATED","로그인이 필요합니다."); }
 @ExceptionHandler(AccessDeniedException.class) ResponseEntity<?> denied(AccessDeniedException e) { return error(403,"FORBIDDEN","이 작업에 대한 권한이 없습니다."); }
 @ExceptionHandler(EntityNotFoundException.class) ResponseEntity<?> missing(EntityNotFoundException e) { return error(404,"NOT_FOUND","자료를 찾을 수 없습니다."); }
 @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class}) ResponseEntity<?> bad(Exception e) { return error(400,"INVALID_INPUT",e instanceof IllegalArgumentException ? e.getMessage() : "입력값을 확인하세요."); }
 @ExceptionHandler({IllegalStateException.class,OptimisticLockingFailureException.class,DataIntegrityViolationException.class}) ResponseEntity<?> conflict(Exception e) { return error(409,"CONFLICT",e instanceof IllegalStateException ? e.getMessage() : "중복되거나 변경된 자료입니다. 새로 조회한 후 다시 시도하세요."); }
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException e) { return error(e.getStatusCode().value(),"BUSINESS_ERROR",e.getReason()==null ? "요청을 처리할 수 없습니다." : e.getReason()); }
 @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class) ResponseEntity<?> resource(Exception e) { return error(404,"NOT_FOUND","자료를 찾을 수 없습니다."); }
 @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class) ResponseEntity<?> upload(Exception e) { return error(400,"FILE_TOO_LARGE","허용된 파일 크기를 초과했습니다."); }
 @ExceptionHandler(Exception.class) ResponseEntity<?> unexpected(Exception e) { return error(500,"INTERNAL_ERROR","요청을 처리하지 못했습니다. 요청 ID와 함께 관리자에게 문의하세요."); }
 private ResponseEntity<?> error(int status,String code,String message) { if(status>=400 && !code.equals("LOGIN_FAILED")) audit.event(CurrentActor.optionalId()==null?null:actor.user(),code.equals("FORBIDDEN")?"ACCESS_DENIED":"REQUEST_REJECTED",null,"FAILURE"); return ResponseEntity.status(status).body(ApiErrors.body(code,message)); }
}
