package com.redcat.tutorials.web.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class GenericResponseDto <T, E>{
    private T data;
    private E error;
    private boolean success;
    private String message;
}
