package com.redcat.tutorials.web.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wiki/content")
public class ApiWikiContentController {

    /**
     * 1. Fetch Index ->
     *
     * a) Controller Summary
     * b) Design Patterns
     * c) Class Dependencies Summary / LLD architecture
     * d) Schema Design Summary -> Either MongoDB or RDS
     * e) Class Level Summary -> Only For Service Classes annotated with @Service
     * f) Config Summary -> Only For Config Classes annotated with @Configuration
     * g) External Service Interaction -> REST or Grpc or SQS or Kafka
     * h) Unit Test Class Summary -> Only For Test Classes present inside src/test/java
     *
     * 2. Fetch Content by ID ->
     *
     * 3. Fetch Controller Summary by ID ->
     *
     * 4. Fetch Class Level Summary by ID ->
     *
     * 5. Fetch Test Class Summary by ID ->
     */
}
