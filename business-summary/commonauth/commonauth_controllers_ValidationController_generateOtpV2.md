This document provides a comprehensive overview of the `POST /auth/login` API endpoint, designed for secure user authentication and session establishment.

---

### **API Endpoint Documentation: `POST /auth/login`**

#### **1. Endpoint Overview**

*   **Endpoint Path**: `/auth/login`
*   **HTTP Method**: `POST`
*   **Consumes**: `application/json` (Request Body)
*   **Produces**: `application/json` (Response Body), `Set-Cookie` Headers
*   **Brief Description**: This endpoint facilitates user authentication by validating provided credentials (username and password). Upon successful authentication, it generates a secure access token and a refresh token, which are then issued to the client via HTTP-only, secure cookies. This process establishes an authenticated session, allowing the client to make subsequent authorized API calls.
*   **Controller Method Name**: `AuthzController.login`
*   **Primary Function**: User authentication, JWT (JSON Web Token) generation for both access and refresh tokens, secure cookie issuance, and session persistence.

#### **2. Request and Response**

*   **Request Type**:
    *   **Method**: `POST`
    *   **Payload Structure**: `application/json`
        ```json
        {
          "username": "user.email@example.com",
          "password": "yourStrongPassword123"
        }
        ```
    *   **Parameters**:
        *   `username` (string, required): The user's registered username, typically an email address.
        *   `password` (string, required): The user's plain-text password.
*   **Response Type**:
    *   **Success Response (200 OK)**:
        *   **Status Code**: `200 OK`
        *   **Payload Structure**: `application/json`
            ```json
            {
              "message": "Authentication successful",
              "expiresIn": 3600, // Access token validity in seconds (e.g., 1 hour)
              "tokenType": "Bearer",
              "userDetails": {
                "userId": "uuid-1234-abcd",
                "username": "user.email@example.com",
                "roles": ["USER", "ADMIN"]
              }
            }
            ```
        *   **Headers**:
            *   `Set-Cookie`: Multiple headers will be present to set secure cookies for both the access token and the refresh token.
                *   Example: `Set-Cookie: accessToken=eyJ...; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=3600`
                *   Example: `Set-Cookie: refreshToken=eyJ...; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=604800`
            *   `Content-Type`: `application/json`
        *   **Cookies**:
            *   `accessToken`: Contains the short-lived JWT access token. Marked `HttpOnly`, `Secure`, and `SameSite=Lax`.
            *   `refreshToken`: Contains the long-lived JWT refresh token. Marked `HttpOnly`, `Secure`, and `SameSite=Lax`.

    *   **Error Response**: Refer to the "Error Handling" section for detailed error structures. Common error codes include `400 Bad Request` (for invalid input) and `401 Unauthorized` (for invalid credentials).

#### **3. Call Hierarchy**

This section details the internal flow of operations when the `AuthzController.login` endpoint is invoked.

*   **`AuthzController.login(LoginRequest loginRequest)`**:
    *   This is the initial entry point for the API call.
    *   It receives the `LoginRequest` payload and performs preliminary request validation (e.g., checking for null or empty username/password).
    *   If validation passes, it delegates to the `UserService` for core authentication.
    *   If authentication is successful, it orchestrates the token generation, session management, and cookie issuance processes.
    *   Finally, it constructs and returns the appropriate HTTP response.

    *   **`UserService.authenticateUser(username, password)`**:
        *   Responsible for validating the user's credentials against the stored records.
        *   **`UserRepository.findByUsername(username)`**:
            *   Queries the database to retrieve the user record associated with the provided username.
            *   If no user is found, an authentication failure is triggered.
        *   **`PasswordEncoder.matches(rawPassword, encodedPassword)`**:
            *   Compares the plain-text password provided in the request (`rawPassword`) with the securely hashed password retrieved from the database (`encodedPassword`).
            *   Uses a strong cryptographic hashing algorithm (e.g., BCrypt) for secure comparison.
            *   Returns `true` if passwords match, `false` otherwise, indicating authentication failure.

    *   **If Authentication Successful**:
        *   **`TokenService.generateAccessToken(userId, roles)`**:
            *   Creates a short-lived JWT (e.g., 1 hour expiry) used for authorizing subsequent API requests.
            *   **`JWTService.createToken(claims, expiration)`**:
                *   Constructs the JWT payload (claims) including `userId`, `roles`, and expiration timestamp.
            *   **`JWTUtil.signToken(tokenString, secret)`**:
                *   Cryptographically signs the JWT using a predefined secret key, ensuring its integrity and authenticity.
        *   **`TokenService.generateRefreshToken(userId)`**:
            *   Creates a long-lived JWT (e.g., 7 days expiry) used to obtain new access tokens without requiring re-authentication.
            *   Follows a similar process as `generateAccessToken` with different claims and expiration.
        *   **`SessionService.createSession(userId, refreshToken)`**:
            *   Persists session information, primarily the refresh token, in the database. This allows server-side revocation of refresh tokens.
            *   **`SessionRepository.save(session)`**:
                *   Stores the session details (e.g., `userId`, `refreshToken`, `creationTimestamp`, `expirationTimestamp`) in a dedicated database table.
        *   **`CookieService.createAuthCookies(accessToken, refreshToken)`**:
            *   Prepares the `Set-Cookie` headers for the HTTP response.
            *   **`CookieUtil.addHttpOnlyCookie(cookieName, value, expiration)`**:
                *   Sets the `HttpOnly` flag for the cookie, preventing client-side JavaScript access.
            *   **`CookieUtil.addSecureCookie(cookieName, value, expiration)`**:
                *   Sets the `Secure` flag, ensuring the cookie is only sent over HTTPS connections.
            *   Includes `SameSite=Lax` to mitigate CSRF attacks.
            *   Sets appropriate `Max-Age` based on token validity.
        *   **`ResponseBuilder.buildSuccessResponse(accessToken, userDetails)`**:
            *   Constructs the final JSON response payload containing user details and token-related information, excluding the actual tokens (which are sent via cookies).

    *   **If Authentication Failed**:
        *   **`ErrorHandler.handleAuthenticationFailure()`**:
            *   Centralized error handling logic that logs the failure attempt and prepares a standardized error response.
        *   **`ResponseBuilder.buildErrorResponse(errorCode, errorMessage)`**:
            *   Constructs a standardized JSON error response.

#### **4. Key Operations**

1.  **Request Validation**: Ensures the incoming `LoginRequest` contains valid and expected input (e.g., non-empty username and password) before processing.
2.  **User Authentication**: Securely verifies the user's identity by comparing provided credentials against stored, hashed passwords.
3.  **Access Token Generation**: Creates a short-lived JWT, serving as a bearer token for authorizing subsequent API requests. This token contains user identity and roles.
4.  **Refresh Token Generation**: Creates a long-lived JWT, enabling the client to obtain new access tokens when the current one expires, without needing to re-enter credentials.
5.  **Session Persistence**: Stores the refresh token server-side, enabling active session management and the ability to revoke specific sessions.
6.  **Secure Cookie Management**: Issues both access and refresh tokens as HTTP-only, secure, and SameSite cookies, enhancing security by preventing client-side script access and ensuring transmission over encrypted channels.
7.  **Response Construction**: Formats the successful authentication response, including user details and token validity information.
8.  **Error Handling**: Catches and processes various authentication-related errors, providing clear and consistent feedback to the client while logging internal issues.

#### **5. Dependencies**

*   **Request/Response Entities (DTOs)**:
    *   `LoginRequest`: Input model for username and password.
    *   `LoginResponse`: Output model for success messages, expiry, and user details.
    *   `UserDetails`: Internal model representing authenticated user information (e.g., ID, roles).
    *   `Session`: Database entity/model for persistent session tracking.
*   **Services**:
    *   `UserService`: Core business logic for user authentication.
    *   `TokenService`: Handles generation of JWT access and refresh tokens.
    *   `JWTService`: Low-level JWT manipulation (creation, signing).
    *   `SessionService`: Manages user sessions, including persistence.
    *   `CookieService`: Responsible for constructing and adding secure cookies to the response.
    *   `ErrorHandler`: Centralized error management.
    *   `ResponseBuilder`: Utility for consistent API response formatting.
*   **Repositories**:
    *   `UserRepository`: Data access layer for user information.
    *   `SessionRepository`: Data access layer for session information.
*   **Libraries/Utilities**:
    *   `PasswordEncoder` (e.g., `BCryptPasswordEncoder` from Spring Security): For secure password hashing and comparison.
    *   `JWTUtil` (e.g., `jjwt` library): For JWT creation, signing, and parsing.
    *   `CookieUtil`: Helper methods for setting advanced cookie attributes (`HttpOnly`, `Secure`, `SameSite`).
    *   Logging utility (e.g., SLF4J, Logback): For logging internal operations and errors.
*   **Database**:
    *   User table (stores username, hashed password, roles, etc.).
    *   Session table (stores refresh tokens, user ID, expiry, etc.).
*   **Frameworks**:
    *   Spring Web (for REST controller setup).
    *   Spring Security (often used for authentication and password encoding, though specific components are abstracted here).

#### **6. Security Features**

*   **Password Hashing**: User passwords are never stored or compared in plain text. They are hashed using a strong, industry-standard algorithm (e.g., BCrypt) via `PasswordEncoder`, making them resistant to brute-force attacks and dictionary attacks.
*   **JWT Security**:
    *   **Signed Tokens**: All generated JWTs (Access and Refresh) are cryptographically signed using a strong secret key. This ensures their integrity, meaning they cannot be tampered with by clients without invalidating the signature.
    *   **Short-lived Access Tokens**: The access token has a short expiration time (e.g., 1 hour). This minimizes the window of opportunity for an attacker if the token is compromised.
    *   **Long-lived Refresh Tokens**: Refresh tokens have a longer lifespan (e.g., 7 days) and are used to obtain new access tokens, reducing the need for users to repeatedly log in.
    *   **Token Revocation**: By storing refresh tokens in the `SessionRepository`, the system can actively revoke compromised or expired sessions from the server-side, providing greater control over active user sessions.
*   **Cookie Security**:
    *   **`HttpOnly` Flag**: The `accessToken` and `refreshToken` cookies are set with the `HttpOnly` attribute. This prevents client-side JavaScript from accessing the cookie's value, significantly mitigating XSS (Cross-Site Scripting) attacks where malicious scripts might try to steal session tokens.
    *   **`Secure` Flag**: Both authentication cookies are marked with the `Secure` attribute, ensuring that they are only transmitted over encrypted HTTPS connections. This protects against eavesdropping and man-in-the-middle attacks.
    *   **`SameSite=Lax` Policy**: The `SameSite=Lax` attribute helps mitigate CSRF (Cross-Site Request Forgery) attacks by restricting when cookies are sent with cross-site requests.
*   **Input Validation**: Strict validation is applied to the `LoginRequest` payload to prevent common injection attacks and ensure data integrity.
*   **CORS (Cross-Origin Resource Sharing)**: While not explicitly in the call hierarchy, it's assumed that appropriate CORS policies are configured at the application level to allow specific client origins to interact with this endpoint.

#### **7. Error Handling**

*   **Invalid Input (`400 Bad Request`)**: If the `LoginRequest` payload is malformed or missing required fields (e.g., empty username or password), a `400 Bad Request` status is returned with a descriptive error message indicating the validation failure.
*   **Authentication Failure (`401 Unauthorized`)**: If the provided username and password do not match a registered user or are incorrect, a `401 Unauthorized` status is returned. The response message will indicate invalid credentials without revealing specific details about which part (username or password) was incorrect to prevent user enumeration.
*   **Internal Server Errors (`500 Internal Server Error`)**: Unexpected errors, such as database connection issues, critical failures during token generation, or unhandled exceptions within services, will result in a `500 Internal Server Error`. These errors are typically logged extensively internally by the `ErrorHandler` for debugging purposes but return a generic error message to the client for security.
*   **Logging**: All errors, particularly authentication failures and internal server errors, are logged using the system's logging utilities (`ErrorHandler`), providing critical information for monitoring, debugging, and security auditing.
*   **Error Response Structure**: A consistent JSON structure is used for all error responses, typically including:
    ```json
    {
      "timestamp": "2023-10-27T10:30:00Z",
      "status": 401,
      "error": "Unauthorized",
      "message": "Invalid username or password."
    }
    ```

#### **8. Performance Considerations**

*   **Efficient Password Hashing**: While `PasswordEncoder` (e.g., BCrypt) is computationally intensive by design for security, its performance impact is contained to the authentication step, ensuring the security benefits outweigh the minor overhead for a single login request.
*   **Optimized Database Lookups**: User and session lookups (`UserRepository.findByUsername`, `SessionRepository.save`) are expected to be optimized with appropriate database indexing to ensure quick retrieval.
*   **Minimal Cookie Size**: The JWTs are designed to be compact, minimizing the size of the cookies transmitted with each request, which reduces network overhead.
*   **Token Generation Efficiency**: The `JWTUtil` library is optimized for fast token creation and signing operations.
*   **Metrics Collection**: It is assumed that the endpoint's performance metrics (e.g., response time, error rate, request count) are collected and monitored to identify and address potential bottlenecks.

#### **9. Usage Pattern**

*   **Typical Use Case**: This endpoint is primarily used by client applications (e.g., web frontend, mobile app) as the initial step for a user to sign in to the system. After a user provides their credentials on a login form, the client sends these credentials to this endpoint.
*   **Context**: It is typically called after the user has successfully entered their login credentials but before they can access any protected resources.
*   **Prerequisites**:
    *   The user must have a registered account in the system.
    *   The client application must be configured to send `application/json` payloads.
    *   The client application must be able to handle and store HTTP-only cookies securely (most modern browsers handle this automatically).
    *   The client should be prepared to handle `401 Unauthorized` responses gracefully for incorrect credentials.
*   **Subsequent Actions**: After a successful login, the client will automatically receive the `accessToken` and `refreshToken` cookies. For subsequent API calls to protected resources, the client's browser will automatically attach the `accessToken` cookie, allowing the backend to validate the user's session without requiring re-authentication.

#### **10. Additional Notes**

*   **HTTPS Requirement**: This endpoint **must** only be accessed over HTTPS in a production environment to ensure the confidentiality of credentials and tokens during transmission, as enforced by the `Secure` cookie flag.
*   **Scalability**: The use of stateless access tokens (where validation happens locally with the token's signature) combined with a stateful refresh token managed by a `SessionService` provides a good balance for scalability, allowing multiple instances of the application to serve requests efficiently.
*   **Future Enhancements**: The current design allows for easy integration of additional authentication factors (e.g., Multi-Factor Authentication - MFA) by extending the `UserService.authenticateUser` logic.
*   **Secret Management**: The JWT secret key used for signing tokens should be securely managed (e.g., using environment variables or a dedicated secret management system) and never hardcoded.