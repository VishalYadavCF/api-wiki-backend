package com.redcat.tutorials.web.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateProjectRequestDto {
    private String name;
    private String description;
    private String owner;
    private String gitUrl;
}
