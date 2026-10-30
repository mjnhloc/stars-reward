package com.sr.classes.service;

import java.util.List;

import com.sr.classes.dto.request.CreateClassRequest;
import com.sr.classes.dto.response.ClassResponse;
import com.sr.classes.dto.response.CreateClassResponse;

public interface ClassService {

    List<ClassResponse> getClasses();

    CreateClassResponse createClass(CreateClassRequest request);
}
