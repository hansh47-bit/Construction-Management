package com.construction.management.auth;
import java.io.Serializable;
public record SessionUser(String id,long authVersion,String name,String position) implements Serializable {}
