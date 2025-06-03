This document provides a comprehensive overview of the `/api/v1/auth/token-exchange` API endpoint, detailing its functionality, internal workings, security considerations, and usage.

---

### API Endpoint Description: `/api/v1/auth/token-exchange`

This endpoint facilitates the exchange of an authorization code, typically obtained from an external identity provider, for application-specific authentication tokens (access token and refresh token). It acts as a crucial step in the user authentication flow, enabling the application to establish a secure, authenticated session with the user.

---

#### 1. Endpoint Overview

*   **Endpoint Path**: `/api/v1/auth/token-exchange`
*   **HTTP Method**: `POST`
*   **Consumes**: `application/json` (or `application/x-www-form-urlencoded` if code/redirectUri are form parameters)
*   **Produces**: `application/json`
*   **Purpose**: To exchange an authorization code (obtained from an external OAuth provider) for internal JWT-based access and refresh tokens, and establish an authenticated user session by setting secure HTTP-only cookies.
*   **Controller Method**: `authController.exchangeCodeForTokens`
*   **Primary Function**: Manages the flow of authenticating a user via an external identity provider and issuing internal application tokens for subsequent API access.

---

#### 2. Request and Response

*   **Request Type**:
    *   **Method**: `POST`
    *   **Payload Structure**: A JSON object or URL-encoded form parameters containing:
        *   `code`: (String, Required) The authorization code received from the external identity provider.
        *   `redirectUri`: (String, Required) The `redirect_uri` used during the initial authorization request to the external identity provider. This is crucial for validation and security.
*   **Response Type**:
    *   **Success Response (HTTP 200 OK)**:
        *   **Payload**: A JSON object containing:
            *   `accessToken`: (String) A JWT access token for immediate API authorization. (Note: While the token is also set as a cookie, providing it in the body can be useful for clients that prefer explicit handling or for debugging.)
            *   `refreshToken`: (String) A JWT refresh token, used to obtain new access tokens after the current one expires, without requiring re-authentication. (Similar to accessToken, can be included for client-side storage if needed, but primarily for server-side cookie management here.)
            *   `expiresIn`: (Number) The lifetime in seconds of the `accessToken`.
            *   `tokenType`: (String) Typically "Bearer".
        *   **Headers**:
            *   `Content-Type`: `application/json`
        *   **Cookies**: Two HTTP-only cookies are set:
            *   `accessTokenCookie`: Contains the JWT access token.
            *   `refreshTokenCookie`: Contains the JWT refresh token.
            *   These cookies are configured with `HttpOnly`, `Secure`, and `SameSite` attributes for enhanced security (details in Security Features).
    *   **Error Response**: (See Error Handling section)
        *   HTTP status codes like `400 Bad Request` (for invalid `code` or `redirectUri`), `401 Unauthorized` (if external authentication fails), or `500 Internal Server Error` (for unexpected server issues).
        *   Payload typically includes an error message and possibly an error code.

---

#### 3. Call Hierarchy

The following represents the detailed flow of execution when the `/api/v1/auth/token-exchange` endpoint is invoked:

1.  **`authController.exchangeCodeForTokens(code, redirectUri)`**
    *   **Role**: Entry point for the API call. Handles initial request parsing and delegates to the service layer.
    *   **Operations**:
        *   Receives `code` and `redirectUri` from the HTTP request.
        *   Performs basic input validation (e.g., presence of parameters).
        *   Invokes `authService.processTokenExchange` to handle the core logic.

2.  **`authService.processTokenExchange(code, redirectUri)`**
    *   **Role**: Orchestrates the entire token exchange process, acting as the business logic layer.
    *   **Operations**:
        *   **2.1. `externalOAuthService.getTokensFromAuthCode(code, redirectUri)`**
            *   **Role**: Communicates with the external Identity Provider (IdP) to exchange the authorization `code` for tokens and user details specific to the IdP (e.g., OAuth tokens, user profile).
            *   **Inputs**: The authorization `code` and the `redirectUri`.
            *   **Outputs**: External user details (e.g., `externalUserDetails`) and potentially external access/refresh tokens.
            *   **Key Operation**: Makes an HTTP call to the external IdP's token endpoint. Handles potential network errors or IdP-specific authentication failures.
        *   **2.2. `userService.findOrCreateUser(externalUserDetails)`**
            *   **Role**: Manages the application's internal user representation. It maps external user details to an internal user profile.
            *   **Inputs**: `externalUserDetails` (e.g., external user ID, email, name).
            *   **Operations**:
                *   Checks if a user with the given `externalUserDetails` (e.g., external ID) already exists in the application's database.
                *   If the user exists, it retrieves their profile.
                *   If the user does not exist, it creates a new user entry in the application's database based on the provided external details.
            *   **Outputs**: An internal `User` object, including their assigned roles.
        *   **2.3. `tokenService.generateAuthTokens(user, roles)`**
            *   **Role**: Responsible for creating the application's internal JWT-based access and refresh tokens.
            *   **Inputs**: The internal `User` object and their `roles`.
            *   **Operations**:
                *   Generates a short-lived **Access Token** (JWT), digitally signed and containing claims like user ID, roles, and expiration time.
                *   Generates a long-lived **Refresh Token** (JWT), also digitally signed, used for obtaining new access tokens without requiring re-login.
                *   (Implicitly) Stores the refresh token securely in the database for revocation and validity checks.
            *   **Outputs**: An `AuthTokens` object containing the `accessToken` and `refreshToken` strings.
        *   **2.4. Return `AuthTokens`** to `authController`.

3.  **`authController.exchangeCodeForTokens` (continued)**
    *   **Role**: Takes the generated `AuthTokens` and prepares the HTTP response.
    *   **Operations**:
        *   **3.1. `cookieService.setAuthCookies(accessToken, refreshToken)`**
            *   **Role**: Configures and adds the access and refresh tokens as secure, HTTP-only cookies to the HTTP response.
            *   **Inputs**: The `accessToken` and `refreshToken` strings.
            *   **Operations**:
                *   Creates `Cookie` objects for both tokens.
                *   Sets `HttpOnly`, `Secure`, `SameSite` attributes (see Security Features).
                *   Adds these cookies to the `HttpServletResponse`.
        *   **3.2. Constructs the JSON response payload** with the `accessToken`, `refreshToken` (optional in body), `expiresIn`, and `tokenType`.
        *   **3.3. Returns the `ResponseEntity`** with HTTP 200 OK status code.

---

#### 4. Key Operations

*   **Request Validation**: Ensures `code` and `redirectUri` parameters are present and conform to expected formats at the `authController` level.
*   **External OAuth Code Exchange**: Securely communicates with the external Identity Provider to validate the authorization code and retrieve user information. This is a critical step for user authentication.
*   **User Provisioning/Lookup**: Manages the application's internal user accounts by either creating a new user or retrieving an existing one based on details obtained from the external IdP.
*   **Internal Token Generation**: Creates secure JWTs (Access and Refresh Tokens) that are specific to the application, digitally signed, and contain relevant user claims (e.g., user ID, roles).
*   **Refresh Token Persistence**: Stores the generated refresh token (or its relevant identifier/hash) in the database to enable revocation and to track active sessions.
*   **Secure Cookie Management**: Sets the generated tokens as secure, HTTP-only cookies in the browser, establishing the authenticated session for the client without exposing the tokens to client-side JavaScript.
*   **Response Formatting**: Prepares a clear and structured JSON response containing token details and status.

---

#### 5. Dependencies

*   **Request/Response Entities (DTOs)**:
    *   `TokenExchangeRequest`: Represents the input payload (`code`, `redirectUri`).
    *   `TokenExchangeResponse`: Represents the output payload (`accessToken`, `refreshToken`, `expiresIn`, `tokenType`).
    *   `ExternalUserDetails`: Data model for user information received from the external IdP.
    *   `User`: Internal data model for application users.
    *   `AuthTokens`: Internal data model encapsulating access and refresh tokens.
*   **Services**:
    *   `AuthService`: Orchestrates the business logic.
    *   `ExternalOAuthService`: Handles communication with the external Identity Provider.
    *   `UserService`: Manages user creation, retrieval, and updates in the application's database.
    *   `TokenService`: Responsible for JWT generation, signing, and refresh token management.
    *   `CookieService`: Utility for setting and managing HTTP-only cookies.
*   **Database Entities/Tables**:
    *   `User` table: Stores application user profiles.
    *   `RefreshToken` table: Stores details about issued refresh tokens (e.g., token ID, user ID, expiration, status).
*   **Libraries/Frameworks**:
    *   Spring Framework (for Controllers, Services, Dependency Injection).
    *   Spring Security (for general security context, potentially `UserDetailsService` or similar).
    *   JWT Library (e.g., `jjwt` or `auth0-java-jwt`) for token creation, signing, and parsing.
    *   HTTP Client Library (e.g., `RestTemplate`, `WebClient`, or `OkHttpClient`) for calling the external OAuth service.
    *   Logging utilities (e.g., SLF4J/Logback).

---

#### 6. Security Features

*   **HTTPS Enforcement**: Assumed that the endpoint is only accessible over HTTPS to prevent eavesdropping and Man-in-the-Middle attacks.
*   **JWT Token Security**:
    *   **Digital Signing**: Access and refresh tokens are digitally signed using a strong cryptographic algorithm and a secret key, ensuring their integrity and authenticity (i.e., they haven't been tampered with and were issued by the legitimate server).
    *   **Expiration**: Access tokens are short-lived, minimizing the window of opportunity for misuse if intercepted. Refresh tokens are longer-lived but managed securely.
    *   **No Sensitive Data**: JWTs contain minimal sensitive user data, typically just user ID and roles, not passwords or highly confidential information.
*   **Cookie Security**:
    *   **`HttpOnly`**: Cookies are set with the `HttpOnly` flag, preventing client-side JavaScript from accessing them. This mitigates XSS (Cross-Site Scripting) attacks where malicious scripts might try to steal session tokens.
    *   **`Secure`**: Cookies are set with the `Secure` flag, ensuring they are only sent over HTTPS connections.
    *   **`SameSite=Lax` (or `Strict`)**: This attribute prevents the browser from sending the cookie with cross-site requests, protecting against CSRF (Cross-Site Request Forgery) attacks. `Lax` is commonly used for convenience while `Strict` provides stronger protection but may require explicit handling for certain cross-site navigations.
*   **`redirect_uri` Validation**: The `redirectUri` provided in the request is critically important. It must be validated against a pre-registered whitelist of allowed URIs to prevent open redirect vulnerabilities and ensure tokens are sent only to legitimate client applications.
*   **Input Validation**: Strict validation of the `code` and `redirectUri` parameters helps prevent injection attacks and ensures proper formatting.
*   **Refresh Token Management**: Refresh tokens are stored server-side, allowing for their revocation (e.g., on logout, password change, or security breach). This provides an additional layer of control over active sessions.

---

#### 7. Error Handling

*   **Validation Errors (HTTP 400 Bad Request)**:
    *   If `code` or `redirectUri` are missing or malformed, a `400 Bad Request` is returned with a descriptive error message.
*   **External Authentication Failures (HTTP 401 Unauthorized)**:
    *   If the `externalOAuthService` fails to exchange the `code` with the external IdP (e.g., invalid or expired code, IdP issues), an `401 Unauthorized` status is returned. The response might include details about the external error if appropriate and safe.
*   **Internal Server Errors (HTTP 500 Internal Server Error)**:
    *   Unhandled exceptions or critical system failures (e.g., database connection issues, token generation failures due to configuration errors) result in a `500 Internal Server Error`.
    *   These errors are typically logged with full stack traces for debugging but returned to the client with a generic message to avoid leaking sensitive internal details.
*   **Logging**:
    *   All errors are logged to the application's log management system, providing context (e.g., request details, user ID if available) to aid in troubleshooting and monitoring.
    *   Sensitive information like authorization codes are not logged directly but rather their states (e.g., "invalid code received").
*   **Error Response Structure**:
    *   Error responses typically follow a consistent JSON format, e.g.:
        ```json
        {
          "timestamp": "2023-10-27T10:30:00Z",
          "status": 400,
          "error": "Bad Request",
          "message": "Missing required parameter: code",
          "path": "/api/v1/auth/token-exchange"
        }
        ```

---

#### 8. Performance Considerations

*   **External API Call Latency**: The most significant performance factor is typically the latency of the `externalOAuthService.getTokensFromAuthCode` call to the external Identity Provider. This is an external dependency that the application has limited control over.
*   **Database Operations**:
    *   `userService.findOrCreateUser`: Database lookups (`findByExternalId`) and potential inserts (`save`) should be optimized with appropriate indexing on `external_id` (or similar unique identifier).
    *   `tokenService.generateAuthTokens`: If refresh tokens are persisted, this involves a database write. Efficient database access (e.g., connection pooling, optimized queries) is crucial.
*   **JWT Generation**: While computationally inexpensive for typical payloads, repeated generation of JWTs should be efficient. The chosen JWT library should be performant.
*   **Metrics Collection**: It is highly recommended to collect metrics for this endpoint, such as:
    *   **Request Latency**: Total time taken to process a request.
    *   **External Call Latency**: Time spent waiting for the external IdP response.
    *   **Database Query Latency**: Time for user lookup/creation and refresh token persistence.
    *   **Success Rate**: Percentage of successful token exchanges.
    *   **Error Rates**: Breakdown of different error types.
*   **Cookie Size**: Keep cookie payload size minimal, although for JWTs, this is usually not a major concern.
*   **Rate Limiting**: While not explicitly listed in the hierarchy, implementing rate limiting on this endpoint is a common practice to prevent brute-force attacks or abuse.

---

#### 9. Usage Pattern

This endpoint is primarily used in the context of an OAuth 2.0 Authorization Code Flow, specifically as the **callback target** after a user has successfully authenticated with an external Identity Provider (e.g., Google, Facebook, Okta, Auth0).

1.  **User Redirection**: The client application (e.g., web browser) redirects the user to the external IdP's authorization endpoint.
2.  **External Authentication**: The user authenticates with the external IdP.
3.  **IdP Redirect with Code**: The external IdP redirects the user back to the application's `redirect_uri` (which points to this `/api/v1/auth/token-exchange` endpoint), appending an authorization `code` and `state` parameter.
4.  **Endpoint Invocation**: The client (typically the front-end application) then makes a `POST` request to this `/api/v1/auth/token-exchange` endpoint, passing the `code` and the `redirectUri` it received from the IdP.
5.  **Session Establishment**: Upon successful processing, the server sets secure HTTP-only cookies in the user's browser, establishing their authenticated session with the application. The client can then make subsequent API calls to protected resources, which will automatically include these cookies.

**Prerequisites**:
*   The application must be registered as a client with the external Identity Provider, with a configured `client_id` and the `redirect_uri` pointing to this endpoint.
*   A successful authorization step with the external IdP resulting in a valid, unexpired authorization `code`.
*   The `redirect_uri` sent in the request to this endpoint *must exactly match* the one used in the initial authorization request to the external IdP and registered with the IdP.

---

#### 10. Additional Notes

*   **Refresh Token Rotation**: For enhanced security, consider implementing refresh token rotation, where a new refresh token is issued with every successful use of the old one, and the old one is immediately revoked. This makes it harder for stolen refresh tokens to be used.
*   **JTI (JWT ID) Claim**: Including a `jti` (JWT ID) claim in both access and refresh tokens, and storing this ID in the database for refresh tokens, enables explicit token revocation.
*   **Error Transparency**: While general errors should be generic for security, specific client-side errors (e.g., invalid `redirect_uri`) should provide enough detail for developers to debug without compromising server internals.
*   **IdP Configuration**: The `externalOAuthService` will need to be configured with the specific details of the external Identity Provider (e.g., client ID, client secret, authorization endpoint, token endpoint, user info endpoint).
*   **Scalability**: Ensure that the database operations and token generation processes are designed to handle high concurrency and load for optimal performance.