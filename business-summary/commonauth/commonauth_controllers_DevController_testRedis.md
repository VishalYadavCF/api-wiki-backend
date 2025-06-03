This document provides a comprehensive description of the `/api/v1/auth/token-exchange` API endpoint, detailing its functionality, architecture, security, and operational considerations.

---

## API Endpoint: `/api/v1/auth/token-exchange`

### 1. Endpoint Overview

*   **Endpoint Path**: `/api/v1/auth/token-exchange`
*   **HTTP Method**: `POST`
*   **Consumes**: `application/json`
*   **Produces**: `application/json`
*   **Purpose**: This endpoint facilitates the exchange of an authentication code obtained from an external identity provider (e.g., OAuth2 provider) for application-specific access and refresh tokens. These tokens are then securely delivered to the client as HttpOnly cookies, establishing the user's session within the application.
*   **Controller Method**: `AuthTokenExchangeController.exchangeToken`
*   **Primary Function**: To authenticate a user via an external provider's code, create or retrieve their user profile in the local system, generate secure JSON Web Tokens (JWTs), and set them as secure HTTP-only cookies in the client's browser.

### 2. Request and Response

*   **Request Type**:
    *   The request body is a JSON object representing the `TokenExchangeRequest` data transfer object (DTO).
    *   **Payload Structure**:
        ```json
        {
          "externalAuthCode": "YOUR_EXTERNAL_AUTH_CODE_HERE"
        }
        ```
    *   **Input Parameters**:
        *   `externalAuthCode` (String, required): The authorization code received from the external identity provider after a successful user authentication redirect.

*   **Response Type**:
    *   **Success Response**:
        *   **HTTP Status Code**: `200 OK`
        *   **Payload Structure**: A JSON object representing the `TokenExchangeResponse` DTO. While the primary tokens are set as cookies, the response body might include non-sensitive user details or a success message.
            ```json
            {
              "message": "Token exchange successful",
              "userId": "uuid-of-user",
              "username": "user.email@example.com"
            }
            ```
        *   **Headers**: `Set-Cookie` headers will be present for `access_token` and `refresh_token` cookies, configured with `HttpOnly`, `Secure`, and `SameSite` attributes.
        *   **Cookies**:
            *   `access_token`: Contains the short-lived JWT for API access.
            *   `refresh_token`: Contains the longer-lived JWT used to obtain new access tokens.

    *   **Error Response**:
        *   **HTTP Status Code**: Varies (e.g., `400 Bad Request`, `401 Unauthorized`, `500 Internal Server Error`).
        *   **Payload Structure**: A standardized error JSON object.
            ```json
            {
              "timestamp": "2023-10-27T10:30:00Z",
              "status": 400,
              "error": "Bad Request",
              "message": "Invalid external authentication code.",
              "path": "/api/v1/auth/token-exchange"
            }
            ```

### 3. Call Hierarchy

The following outlines the sequence of method calls and operations performed when the `/api/v1/auth/token-exchange` endpoint is invoked:

1.  **`AuthTokenExchangeController.exchangeToken(HttpServletRequest request, HttpServletResponse response, TokenExchangeRequest tokenExchangeRequest)`**
    *   Receives the incoming HTTP request containing the external authentication code.
    *   Invokes initial validation and delegates core logic.

    *   **Operations:**
        *   `tokenExchangeRequest.validate()`: Performs basic validation on the incoming `TokenExchangeRequest` object to ensure required fields are present and correctly formatted.
        *   `tokenExchangeService.generateAndSetTokens(tokenExchangeRequest.getExternalAuthCode(), response)`: Delegates the primary business logic to the `TokenExchangeService`.
            *   **`externalAuthService.exchangeCodeForExternalUser(String externalAuthCode)`**:
                *   Responsible for communicating with the external identity provider.
                *   **Operations:**
                    *   `externalAuthClient.callExternalProvider(externalAuthCode)`: Makes an HTTP request to the external identity provider's token endpoint to exchange the `externalAuthCode` for an access token and user information from the external provider.
                    *   `externalAuthClient.parseExternalResponse(externalResponse)`: Parses the response received from the external provider into a structured `ExternalUserDetails` object, containing details like the external user ID, email, etc.
            *   **`userService.findOrCreateUser(ExternalUserDetails externalUserDetails)`**:
                *   Manages the persistence of user data in the application's database.
                *   **Operations:**
                    *   `userRepository.findByExternalId(externalUserDetails.getExternalId())`: Attempts to find an existing user in the database using the `externalUserDetails.getExternalId()`.
                    *   `userRepository.save(newUser)`: If no existing user is found, a new `User` entity is created based on `ExternalUserDetails` and saved to the database. If found, the existing user's details might be updated.
            *   **`jwtService.generateAccessToken(User user)`**:
                *   Generates a short-lived JSON Web Token (JWT) intended for authenticating API requests.
                *   **Operations:**
                    *   `jwtUtils.createToken(claims, expiration)`: Creates the JWT string, incorporating user-specific claims (e.g., user ID, roles) and setting a short expiration time.
            *   **`jwtService.generateRefreshToken(User user)`**:
                *   Generates a longer-lived JSON Web Token (JWT) used for obtaining new access tokens without requiring re-authentication.
                *   **Operations:**
                    *   `jwtUtils.createToken(claims, longerExpiration)`: Creates the refresh token JWT string with user claims and a significantly longer expiration.
            *   **`cookieService.setAccessTokenCookie(HttpServletResponse response, String accessToken)`**:
                *   Adds the generated access token as an HTTP-only, secure cookie to the HTTP response.
                *   **Operations:**
                    *   `response.addCookie(new Cookie("access_token", accessToken))`: Creates and adds the cookie, ensuring attributes like `HttpOnly`, `Secure`, and `SameSite` are properly configured.
            *   **`cookieService.setRefreshTokenCookie(HttpServletResponse response, String refreshToken)`**:
                *   Adds the generated refresh token as an HTTP-only, secure cookie to the HTTP response.
                *   **Operations:**
                    *   `response.addCookie(new Cookie("refresh_token", refreshToken))`: Creates and adds the cookie, ensuring attributes like `HttpOnly`, `Secure`, and `SameSite` are properly configured.
    *   `buildSuccessResponse(accessToken)`: Constructs the final `TokenExchangeResponse` DTO to be returned as the HTTP response body.

### 4. Key Operations

The endpoint performs several critical operations:

*   **Request Validation**: Ensures the incoming `externalAuthCode` is valid and present before processing.
*   **External Authentication Code Exchange**: Communicates with the configured external identity provider (e.g., Google, Okta) to exchange the temporary authentication code for user details and potentially an external access token. This validates the user's identity via the external system.
*   **User Management (Find or Create)**: Checks if the authenticated user already exists in the application's database. If not, a new user profile is created. If they exist, their details might be updated.
*   **JWT Generation**: Creates two distinct JSON Web Tokens:
    *   **Access Token**: Short-lived, used for authorizing subsequent API calls.
    *   **Refresh Token**: Longer-lived, used to acquire new access tokens without re-authenticating the user.
*   **Secure Cookie Management**: Embeds the generated JWTs into HttpOnly, Secure, and SameSite cookies. This prevents client-side JavaScript from accessing the tokens and mitigates cross-site scripting (XSS) and cross-site request forgery (CSRF) vulnerabilities.
*   **Response Construction**: Prepares the final success response payload for the client, confirming the successful token exchange.

### 5. Dependencies

This endpoint relies on several components and data structures:

*   **Request/Response Entities (DTOs)**:
    *   `TokenExchangeRequest`: Input model for the external authentication code.
    *   `TokenExchangeResponse`: Output model for the success response.
    *   `ExternalUserDetails`: Internal model representing user data obtained from the external identity provider.
    *   `User`: Database entity representing the application's internal user profile.
*   **Services/Libraries**:
    *   `TokenExchangeService`: Orchestrates the overall token exchange process.
    *   `ExternalAuthService`: Handles communication and integration with external identity providers.
        *   `ExternalAuthClient`: Utility for making HTTP calls to the external provider's API.
    *   `UserService`: Manages application-specific user data (find, create, update).
    *   `JwtService`: Responsible for generating and managing JWTs.
        *   `JwtUtils`: Low-level utility for JWT creation and signing.
    *   `CookieService`: Manages the setting of secure HTTP-only cookies in the response.
    *   Standard Java/Jakarta Servlet APIs (`HttpServletRequest`, `HttpServletResponse`, `Cookie`).
    *   Logging utilities (e.g., SLF4J, Log4j).
*   **Database Entities/Tables**:
    *   `User` table (or similar) to store application user profiles, including a foreign key or unique identifier for the external identity provider.
*   **Frameworks/Utilities**:
    *   Spring Web (for REST controller and request/response handling).
    *   Spring Security (implicitly involved in JWT and cookie security practices).
    *   JSON parsing library (e.g., Jackson).
    *   HTTP client library (e.g., Apache HttpClient, Spring WebClient).

### 6. Security Features

The endpoint incorporates several security measures:

*   **Input Validation**: All incoming request parameters are validated to prevent injection attacks and ensure data integrity.
*   **JWT Security**:
    *   **Signing**: Both access and refresh tokens are cryptographically signed (e.g., using HMAC-SHA256 or RSA) to ensure their integrity and authenticity, preventing tampering.
    *   **Expiration**: Access tokens have a short lifespan (e.g., 15 minutes) to limit the impact of token compromise. Refresh tokens have a longer, but still finite, lifespan.
*   **Cookie Security**:
    *   `HttpOnly`: Prevents client-side JavaScript from accessing the token cookies, significantly mitigating XSS attacks.
    *   `Secure`: Ensures cookies are only sent over HTTPS connections, protecting them from eavesdropping during transit.
    *   `SameSite=Lax/Strict`: Mitigates Cross-Site Request Forgery (CSRF) attacks by restricting when cookies are sent with cross-site requests.
*   **CORS Handling**: While not explicitly shown in the hierarchy, proper CORS (Cross-Origin Resource Sharing) configuration is assumed to allow legitimate cross-origin requests while blocking unauthorized ones.
*   **Stateless Authentication**: By relying on JWTs, the server does not need to maintain session state, enhancing scalability and reducing server-side session management overhead.

### 7. Error Handling

Error management is robust, ensuring appropriate responses and internal logging:

*   **Invalid Input**: If `TokenExchangeRequest` fails validation (e.g., `externalAuthCode` is missing or malformed), a `400 Bad Request` status is returned with a descriptive error message.
*   **External Authentication Failures**: If the call to the external identity provider fails or returns an invalid response (e.g., due to an expired or invalid `externalAuthCode`), a `401 Unauthorized` or `500 Internal Server Error` (depending on the nature of the external error) is returned.
*   **Database Errors**: Issues during user retrieval or creation (e.g., database connectivity problems) result in a `500 Internal Server Error`.
*   **Token Generation Errors**: Problems during JWT generation (e.g., misconfigured keys) lead to a `500 Internal Server Error`.
*   **Centralized Error Handling**: Errors are typically caught by global exception handlers within the application framework (e.g., Spring's `@ControllerAdvice`), which log the full stack trace for debugging and return a standardized JSON error response to the client.

### 8. Performance Considerations

The endpoint is designed with performance in mind:

*   **Efficient External Calls**: The `externalAuthClient` should utilize connection pooling and proper timeouts for external HTTP requests to prevent bottlenecks and ensure responsiveness.
*   **Optimized Database Operations**: `userRepository.findByExternalId` should leverage database indexing on the `externalId` column for fast user lookups. User creation (`userRepository.save`) is a single, atomic operation.
*   **Stateless Token Management**: JWTs are stateless; the server does not need to perform expensive database lookups for every request to validate a session, significantly improving scalability.
*   **Minimized Cookie Overhead**: While tokens are set as cookies, their size should be managed to avoid excessive request header sizes.
*   **Metrics Collection**: The endpoint should be integrated with a monitoring system (e.g., Prometheus, Micrometer) to collect metrics on response time, error rates, and throughput, allowing for performance tracking and alerting.

### 9. Usage Pattern

This endpoint is typically invoked as follows:

*   **Context**: It is a critical step in the user authentication flow, specifically after a user has successfully authenticated with an external identity provider (e.g., by clicking "Sign in with Google" and completing the OAuth flow).
*   **Prerequisites**: The client (typically a web browser) must have received a valid `externalAuthCode` from the external identity provider's callback redirect.
*   **Invocation**: The client sends a `POST` request to `/api/v1/auth/token-exchange` with this `externalAuthCode` in the request body.
*   **Purpose**: The primary goal is to exchange the temporary external authentication code for persistent, application-specific access and refresh tokens, thereby establishing a secure and active user session within the application. The tokens are then used for subsequent authenticated API calls.

### 10. Additional Notes

*   **HTTPS Requirement**: The `Secure` flag on cookies makes it mandatory for this endpoint to be served over HTTPS in production environments. Failure to do so will prevent cookies from being sent.
*   **Refresh Token Flow**: This endpoint is solely for the *initial* token exchange. A separate endpoint (e.g., `/api/v1/auth/refresh-token`) would typically exist for clients to use the `refresh_token` to obtain a new `access_token` once the current `access_token` expires.
*   **Idempotency**: While the token generation itself might not be strictly idempotent (new tokens are generated each time), the user creation (`findOrCreateUser`) part is designed to be idempotent: submitting the same external code repeatedly will not create duplicate user accounts.
*   **External Provider Specifics**: The implementation details within `ExternalAuthService` will vary significantly depending on the specific OAuth2/OpenID Connect provider (e.g., Google, Auth0, Keycloak).