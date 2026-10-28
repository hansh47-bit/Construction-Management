package com.construction.management.auth;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;
@Component
public class ApiErrors {
 private final ObjectMapper mapper;
 public ApiErrors(ObjectMapper mapper) { this.mapper=mapper; }
 public static String requestId() { var a=RequestContextHolder.getRequestAttributes(); if(a instanceof ServletRequestAttributes s) return requestId(s.getRequest()); return UUID.randomUUID().toString(); }
 public static String requestId(HttpServletRequest r) { Object id=r.getAttribute("request_id"); if(id==null) { id=UUID.randomUUID().toString(); r.setAttribute("request_id",id); } return id.toString(); }
 public static Map<String,Object> body(String code,String message) { return Map.of("code",code,"message",message,"request_id",requestId()); }
 public void write(HttpServletRequest req,HttpServletResponse res,int status,String code,String message) throws IOException { res.setStatus(status); res.setContentType("application/json;charset=UTF-8"); res.setHeader("Cache-Control","no-store"); mapper.writeValue(res.getOutputStream(),Map.of("code",code,"message",message,"request_id",requestId(req))); }
}
