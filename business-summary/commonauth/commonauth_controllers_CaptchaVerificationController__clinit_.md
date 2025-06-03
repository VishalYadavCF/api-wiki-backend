## API Endpoint Documentation: User Authentication and Token Generation

This document provides a comprehensive overview of the `POST /auth/login` API endpoint, designed for user authentication and the subsequent issuance of secure access and refresh tokens.

---

### 1. Endpoint Overview

*   **Endpoint Path**: `/auth/login`
*   **HTTP Method**: `POST`
*   **Consumes**: `application/json` (for request payload)
*   **Produces**: `application/json` (for success and error responses)
*   **Controller Method**: `AuthController.authenticateUser`
*   **Purpose**: This endpoint serves as the primary mechanism for users to log in to the system. Upon successful authentication with valid credentials, it generates a short-lived JSON Web Token (JWT) for immediate access and a long-lived refresh token for session management, ensuring secure and seamless user experiences. The refresh token is typically delivered via a secure, HttpOnly cookie.

---

### 2. Request and Response

**Request**:
The request payload is a JSON object containing the user's login credentials.

*   **Request Type**: `LoginRequestDTO`
*   **Payload Structure**:
    ```json
    {
      "username": "string",  // The user's unique identifier (e.g., email or username)
      "password": "string"   // The user's password
    }
    ```
    *   **username**: Required. String.
    *   **password**: Required. String.

**Response**:

*   **Success Response (HTTP Status: 200 OK)**:
    Upon successful authentication, the API returns an `AuthResponseDTO` containing the access token and its expiration details. A crucial aspect of the successful response is the setting of a secure `HttpOnly` cookie containing the refresh token.

    *   **Payload Structure (`AuthResponseDTO`)**:
        ```json
        {
          "accessToken": "string",   // The short-lived JWT for API access
          "refreshToken": "string",  // The long-lived refresh token (optional, could be exclusively in cookie)
          "expiresIn": 3600          // Lifetime of the access token in seconds (e.g., 1 hour)
        }
        ```
        *   **accessToken**: String. The JWT used for subsequent authenticated requests.
        *   **refreshToken**: String. The refresh token string. While it might be included in the JSON payload, it's primarily intended for secure storage in the accompanying HttpOnly cookie.
        *   **expiresIn**: Integer. The duration in seconds until the `accessToken` expires.

    *   **Headers**:
        *   `Set-Cookie`: Contains the `refreshToken` with `HttpOnly`, `Secure`, and `SameSite=Strict` attributes. Example: `refreshToken=some_long_token_string; Max-Age=2592000; Path=/auth; HttpOnly; Secure; SameSite=Strict`

    *   **Cookies**:
        *   `refreshToken`: This cookie holds the refresh token. Its attributes ensure maximum security:
            *   `HttpOnly`: Prevents client-side scripts from accessing the cookie, mitigating XSS attacks.
            *   `Secure`: Ensures the cookie is only sent over HTTPS connections, protecting against man-in-the-middle attacks.
            *   `SameSite=Strict`: Prevents the cookie from being sent with cross-site requests, mitigating CSRF attacks.
            *   `Path=/auth`: Limits the cookie's scope to requests within the `/auth` path, enhancing security.
            *   `Max-Age`: Defines the cookie's expiration time, corresponding to the refresh token's validity.

*   **Error Responses**:
    Detailed in the "Error Handling" section below, but typically include:
    *   `400 Bad Request`: For invalid input or missing parameters.
    *   `401 Unauthorized`: For invalid credentials (username/password mismatch).
    *   `500 Internal Server Error`: For unexpected server-side issues (e.g., database errors, token generation failures).

---

### 3. Call Hierarchy

The `AuthController.authenticateUser` method orchestrates a series of calls to various services to perform the authentication and token generation process.

1.  **`AuthController.authenticateUser(LoginRequestDTO loginRequest)`**:
    *   **Role**: Entry point for the authentication request. Receives user credentials, orchestrates validation, token generation, and response construction.
    *   **Input**: `loginRequest` (JSON payload with username and password).
    *   **Flow**:
        1.  **`UserService.validateCredentials(loginRequest.username, loginRequest.password)`**:
            *   **Role**: Verifies the authenticity of the provided user credentials.
            *   **Operations**:
                *   Performs a **database lookup** to retrieve the user record associated with the given `username`.
                *   Compares the provided `password` with the **hashed password** stored in the database.
                *   If credentials are valid, returns the `User` object (containing user ID, roles, etc.).
                *   If validation fails (user not found, password mismatch), an `AuthenticationException` is thrown, which is caught and handled by the controller/global error handler.
            *   **Output**: `User` object or an `AuthenticationException`.

        2.  **`JwtService.generateAccessToken(user.id, user.roles)`**:
            *   **Role**: Creates a cryptographically signed JSON Web Token (JWT) that serves as the access token.
            *   **Operations**:
                *   Constructs the JWT header (e.g., specifying algorithm like HS256, token type).
                *   Builds the JWT payload, including claims such as `sub` (subject, typically user ID), `exp` (expiration time), `iat` (issued at time), and `roles` (user's permissions).
                *   Digitally **signs** the JWT using a pre-configured secret key to ensure its integrity and authenticity.
            *   **Input**: `user.id` (unique identifier for the user), `user.roles` (list of roles/authorities).
            *   **Output**: `String` (the generated access token).

        3.  **`RefreshTokenService.generateRefreshToken(user.id)`**:
            *   **Role**: Generates and persists a long-lived refresh token.
            *   **Operations**:
                *   Generates a cryptographically secure, unique string for the refresh token.
                *   **Persists** this refresh token to the database, associating it with the `user.id` and setting its expiration time. This allows for token revocation and proper session management.
            *   **Input**: `user.id`.
            *   **Output**: `String` (the generated refresh token string).

        4.  **Creates `AuthResponseDTO`**:
            *   **Role**: Assembles the data for the API response payload.
            *   **Operations**: Populates the DTO with the `accessToken` from `JwtService`, the `refreshToken` (which may be for client-side display or solely for the cookie), and the `expiresIn` duration of the access token.

        5.  **Adds Refresh Token to HTTP-Only, Secure, SameSite=Strict Cookie**:
            *   **Role**: Securely transmits the refresh token to the client.
            *   **Operations**: Sets the `refreshToken` as a cookie in the HTTP response. Crucially, it configures the cookie with stringent security attributes (`HttpOnly`, `Secure`, `SameSite=Strict`, `Path`, `Max-Age`) to protect it from common web vulnerabilities.

        6.  **Returns `ResponseEntity<AuthResponseDTO>` with `HttpStatus.OK`**:
            *   **Role**: Finalizes the API response, sending the success payload and configured cookies to the client.

---

### 4. Key Operations

1.  **Request Validation**: Ensures that the incoming `LoginRequestDTO` payload is correctly formed and contains all required fields (username, password).
2.  **Credential Validation**: Verifies the provided username and password against the stored user credentials, often involving password hashing and comparison to prevent sensitive data exposure.
3.  **Access Token Generation (JWT)**: Creates a secure, signed JWT containing user identity and roles, establishing the user's authorization for subsequent API calls.
4.  **Refresh Token Generation and Persistence**: Generates a long-lived refresh token and stores it in the database. This token is used to obtain new access tokens after the current one expires, without requiring the user to re-authenticate with their password. It's crucial for maintaining user sessions.
5.  **Secure Cookie Management**: Sets the refresh token in an `HttpOnly`, `Secure`, and `SameSite=Strict` cookie, protecting it from client-side script access, transmission over insecure channels, and cross-site request forgery (CSRF) attacks.
6.  **Response Assembly**: Constructs the final JSON response payload, including the access token and its expiration, and attaches the secure refresh token cookie.

---

### 5. Dependencies

*   **Request/Response Entities (DTOs)**:
    *   `LoginRequestDTO`: Data Transfer Object for incoming login requests.
    *   `AuthResponseDTO`: Data Transfer Object for the outgoing authentication response.
    *   `User` (Entity/Model): Represents the user record retrieved from the database.

*   **Services**:
    *   `UserService`: Handles user-related operations, particularly credential validation and user retrieval.
    *   `JwtService`: Responsible for generating, signing, and potentially validating JWTs.
    *   `RefreshTokenService`: Manages the generation, persistence, and potential revocation of refresh tokens.

*   **Database Entities/Tables**:
    *   `Users` table: Stores user credentials (hashed passwords), roles, and other user-specific information.
    *   `RefreshTokens` table: Stores generated refresh tokens, their association with users, and expiration times.

*   **Libraries/Frameworks**:
    *   **Spring Boot/Spring Web**: The foundational framework for building the RESTful API.
    *   **Spring Security**: Likely used for managing authentication exceptions and possibly password encoding/hashing.
    *   **JWT Library**: (e.g., `jjwt`, `nimbus-jose-jwt`) For creating and signing JWTs.
    *   **Database ORM/Driver**: (e.g., Hibernate, Spring Data JPA) For interacting with the database.
    *   **Logging Utility**: (e.g., SLF4J, Logback) For logging application events and errors.

---

### 6. Security Features

*   **Input Validation**: Ensures that incoming `username` and `password` fields are present and conform to expected formats, preventing basic injection or malformed requests.
*   **Password Hashing**: Passwords are never stored in plain text. `UserService.validateCredentials` implies that passwords are compared against securely hashed versions (e.g., using BCrypt, Argon2), protecting against data breaches.
*   **JWT Security**:
    *   **Signing**: JWTs are signed with a secret key, ensuring their integrity (not tampered with) and authenticity (issued by the legitimate server).
    *   **Expiration (`exp` claim)**: Access tokens are short-lived, limiting the window of opportunity for token compromise.
    *   **Role-Based Access Control (RBAC)**: User roles embedded in the JWT payload can be used by subsequent API endpoints for authorization checks.
*   **Cookie Security (`HttpOnly`, `Secure`, `SameSite=Strict`)**:
    *   `HttpOnly`: Prevents JavaScript access to the `refreshToken` cookie, mitigating Cross-Site Scripting (XSS) attacks.
    *   `Secure`: Ensures the `refreshToken` cookie is only sent over encrypted HTTPS connections, protecting against Man-in-the-Middle (MITM) attacks.
    *   `SameSite=Strict`: Prevents the browser from sending the `refreshToken` cookie with cross-site requests, providing robust protection against Cross-Site Request Forgery (CSRF) attacks.
    *   `Path=/auth`: Limits the cookie's scope, ensuring it's only sent to relevant endpoints.
*   **Session Management**: The combination of short-lived access tokens and long-lived refresh tokens provides a robust session management system, allowing for secure token renewal without constant re-authentication. Refresh tokens stored in the database can be revoked if needed.
*   **Exception Handling**: Securely handles authentication failures by not revealing specific reasons (e.g., "username not found" vs. "incorrect password"), instead returning a generic `401 Unauthorized` message.

---

### 7. Error Handling

Error handling within this endpoint is designed to provide clear, actionable feedback to the client while protecting sensitive information.

*   **Types of Errors Handled**:
    *   **`400 Bad Request`**: Returned for invalid request payloads (e.g., missing `username` or `password`, malformed JSON).
    *   **`401 Unauthorized`**: Specifically returned when `UserService.validateCredentials` throws an `AuthenticationException` due to invalid `username` or `password`. The message should be generic (e.g., "Invalid credentials") to avoid exposing whether the username or password was incorrect.
    *   **`500 Internal Server Error`**: Caught for any unexpected server-side failures, such as:
        *   Database connection issues during user or refresh token lookup/persistence.
        *   Failures during JWT generation (e.g., problems with signing key).
        *   Any unhandled exceptions within the service layer.

*   **Error Logging**: All errors are logged to the backend system's logging infrastructure (e.g., using `SLF4J/Logback`). This includes detailed stack traces and context for debugging purposes, but this information is not exposed to the client.
*   **Error Response Structure**:
    *   Typically returns a JSON object with an error message and a specific error code or type.
    ```json
    {
      "timestamp": "ISO-8601-datetime",
      "status": 401,
      "error": "Unauthorized",
      "message": "Invalid credentials",
      "path": "/auth/login"
    }
    ```
    *   Or for `400 Bad Request`:
    ```json
    {
      "timestamp": "ISO-8601-datetime",
      "status": 400,
      "error": "Bad Request",
      "message": "Username is required",
      "path": "/auth/login"
    }
    ```
*   **Exception Propagation**: Exceptions from service layers (`UserService`, `JwtService`, `RefreshTokenService`) are caught by the `AuthController` or a global exception handler. They are then translated into appropriate HTTP status codes and error responses.

---

### 8. Performance Considerations

*   **Efficient Credential Validation**: Database lookups for user credentials are critical. Indices on the `username` column in the `Users` table ensure fast retrieval. Password hashing is a CPU-intensive operation, but typically optimized for security rather than raw speed (e.g., by using adaptive hashing algorithms like BCrypt).
*   **Fast Token Generation**: JWT generation is generally a lightweight cryptographic operation. Refresh token generation might involve a database write, which should be efficient and non-blocking.
*   **Database Optimization**: The `RefreshTokens` table should be optimized for quick inserts and lookups (e.g., by `user_id` and the token string itself).
*   **Minimal Response Overhead**: The JSON response payload is concise, and cookie size is kept minimal to reduce network transfer overhead.
*   **Metrics Collection**: The endpoint should ideally be integrated with monitoring tools (e.g., Prometheus, Micrometer) to collect metrics such as response times, error rates, and throughput. This allows for proactive identification of performance bottlenecks.

---

### 9. Usage Pattern

This `POST /auth/login` endpoint is the first step in establishing an authenticated session for a user.

*   **Typical Use Case**:
    *   A user opens the application (web or mobile).
    *   They navigate to a login form.
    *   They enter their `username` and `password`.
    *   The client application sends these credentials as a `POST` request to `/auth/login`.
    *   Upon receiving a `200 OK` response, the client extracts the `accessToken` from the JSON payload. This token is then stored, typically in memory for a Single-Page Application (SPA) or session storage, and used as a Bearer token in the `Authorization` header for all subsequent protected API calls.
    *   The `refreshToken` cookie is automatically handled by the browser and will be sent with future requests to specific `/auth` endpoints (e.g., `/auth/refresh-token`) to obtain new access tokens when the current one expires.

*   **Prerequisites**:
    *   The user must have an existing account registered in the system (i.e., their `username` and `hashed password` must be present in the `Users` database table).
    *   The client application must be capable of sending `POST` requests with a JSON payload and processing `Set-Cookie` headers from the response.
    *   The application must be running over HTTPS to ensure the `Secure` cookie attribute is respected and for overall communication security.

---

### 10. Additional Notes

*   **Token Expiration**: The `accessToken` is designed to be short-lived (e.g., 5-15 minutes) to minimize the impact of a compromised token. The `refreshToken` is long-lived (e.g., 30 days) to provide a seamless user experience without frequent re-logins.
*   **Refresh Token Revocation**: The persistence of refresh tokens in the database allows for server-side revocation. If a user logs out, or if suspicious activity is detected, their refresh token can be invalidated in the database, preventing further unauthorized access.
*   **CORS Configuration**: For front-end applications served from a different origin, appropriate Cross-Origin Resource Sharing (CORS) headers must be configured on the server to allow the client to make this request and receive the `Set-Cookie` header.
*   **Statelessness of JWTs**: While refresh tokens are stateful (persisted in DB), the access tokens (JWTs) are typically stateless. This means the server does not need to store them, simplifying scalability. Validation is done by verifying the signature and expiration.
*   **API Gateway Integration**: In a microservices architecture, this endpoint might sit behind an API Gateway, which could handle initial rate limiting, WAF (Web Application Firewall) checks, and potentially offload SSL.