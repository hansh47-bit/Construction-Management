package com.construction.management.account;
import java.nio.charset.StandardCharsets;
public final class PasswordPolicy {
 private PasswordPolicy() {}
 public static void validate(String password,String login) {
  if(password==null || password.codePointCount(0,password.length())<12 || password.codePointCount(0,password.length())>64 || password.getBytes(StandardCharsets.UTF_8).length>72 || password.equalsIgnoreCase(login))
   throw new IllegalArgumentException("비밀번호는 12~64자, UTF-8 72바이트 이내이며 로그인 ID와 달라야 합니다.");
 }
 public static String normalizeLogin(String login) { return login==null ? "" : login.trim().toLowerCase(java.util.Locale.ROOT); }
}
