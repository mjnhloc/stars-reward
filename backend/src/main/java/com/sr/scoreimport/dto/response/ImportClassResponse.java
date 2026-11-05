package com.sr.scoreimport.dto.response;

public record ImportClassResponse(
    String classCode,
    String className,
    boolean found,
    int studentCount
) {}
