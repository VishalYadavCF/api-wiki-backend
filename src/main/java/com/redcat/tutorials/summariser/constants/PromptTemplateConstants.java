package com.redcat.tutorials.summariser.constants;

public class PromptTemplateConstants {

    public static final String CODE_SUMMARY_PROMPT_FOR_BUSINESS = "You are an expert API documentation generator with deep knowledge of software architecture, authentication systems, and API design. I am providing you with the complete call hierarchy and relevant details for an API endpoint. Your task is to generate a comprehensive, well-structured, and detailed description of the API endpoint, incorporating all relevant aspects, including functionality, call hierarchy, dependencies, security features, error handling, performance considerations, and usage patterns.\n" +
            "\n" +
            "Please produce a detailed text document for the specified API endpoint with the following sections and requirements:\n" +
            "\n" +
            "1. **Endpoint Overview**:\n" +
            "   - Specify the endpoint path, HTTP method, consumes/produces media types (e.g., application/json), and a brief description of its purpose.\n" +
            "   - Include the controller method name (if applicable) and its primary function (e.g., token generation, data retrieval).\n" +
            "\n" +
            "2. **Request and Response**:\n" +
            "   - Describe the request type (e.g., input parameters, payload structure) and response type (e.g., response entity, structure).\n" +
            "   - Mention success response details (e.g., status code, payload, headers, cookies).\n" +
            "\n" +
            "3. **Call Hierarchy**:\n" +
            "   - Provide a detailed breakdown of the method call hierarchy, including:\n" +
            "     - The main controller method and its invoked services or methods.\n" +
            "     - Key operations performed (e.g., validation, authentication, token generation, database operations).\n" +
            "     - Sub-methods or services called, with their roles and inputs/outputs.\n" +
            "     - Use a clear, hierarchical format (e.g., a tree-like structure or ordered list) to represent the flow.\n" +
            "     - Include any utility methods or external service interactions.\n" +
            "\n" +
            "4. **Key Operations**:\n" +
            "   - List and describe the primary operations performed by the endpoint (e.g., request validation, token creation, cookie management, database updates).\n" +
            "   - Highlight the purpose and significance of each operation.\n" +
            "\n" +
            "5. **Dependencies**:\n" +
            "   - List all dependencies, including:\n" +
            "     - Request/response entities (e.g., data models or DTOs).\n" +
            "     - Services or libraries used (e.g., authentication services, JWT libraries).\n" +
            "     - Database entities or tables involved.\n" +
            "     - Frameworks or utilities (e.g., Spring Security, logging utilities).\n" +
            "\n" +
            "6. **Security Features**:\n" +
            "   - Describe security mechanisms implemented, such as:\n" +
            "     - Token security (e.g., JWT signing, expiration).\n" +
            "     - Cookie security (e.g., HttpOnly, Secure, SameSite policies).\n" +
            "     - CORS handling, session management, or input validation.\n" +
            "     - Any other relevant security practices.\n" +
            "\n" +
            "7. **Error Handling**:\n" +
            "   - Detail how errors are managed, including:\n" +
            "     - Types of errors handled (e.g., invalid input, authentication failures, database errors).\n" +
            "     - How errors are logged, rethrown, or returned to the client.\n" +
            "     - Error response structure (if applicable).\n" +
            "\n" +
            "8. **Performance Considerations**:\n" +
            "   - Explain any performance-related features or optimizations, such as:\n" +
            "     - Metrics collection for monitoring.\n" +
            "     - Efficient database or token generation operations.\n" +
            "     - Minimization of overhead (e.g., cookie size, response time).\n" +
            "\n" +
            "9. **Usage Pattern**:\n" +
            "   - Describe the typical use case or context in which the endpoint is called (e.g., after external authentication, for session establishment).\n" +
            "   - Highlight any prerequisites or dependencies for invoking the endpoint.\n" +
            "\n" +
            "10. **Additional Notes** (optional):\n" +
            "    - Include any other relevant details, such as limitations, assumptions, or specific configurations not covered above.\n" +
            "\n" +
            "**Instructions**:\n" +
            "- You have to think reverse and come up with a detailed summary from business developer/ product develepor perspective.\n" +
            "- Use the provided call hierarchy and any additional details to ensure accuracy and completeness.\n" +
            "- Ensure the tone is non-technical yet accessible, suitable for developers and architects.\n" +
            "- Avoid speculative information; base the response strictly on the provided details or logical inferences from them.\n" +
            "- If certain details are missing, note them as assumptions or suggest contacting the system owner for clarification.\n" +
            "- Do not include internal model instructions or metadata unless explicitly requested.\n" +
            "**Input Provided**: " +
            "apiEndpointName: %s\n" +
            "method_call_hierarchy: %s\n";

    public static final String CODE_SUMMARY_PROMPT_FOR_CODE_DOCUMENTATION = "You are a code summariser. Your task is to " +
            "summarise the given code snippet in a concise and clear manner.\n"
            + "Code Snippet:\n"
            + "%s\n\n"
            + "Summary:%s";
}
