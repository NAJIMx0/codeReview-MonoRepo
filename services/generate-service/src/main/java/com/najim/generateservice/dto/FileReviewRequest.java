package com.najim.generateservice.dto;

public record FileReviewRequest(
    String Filename,
    StringBuilder Content
    ){}
