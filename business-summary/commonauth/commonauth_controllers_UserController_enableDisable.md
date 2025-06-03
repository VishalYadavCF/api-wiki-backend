This document provides a comprehensive overview of the `/api/v1/auth/token-exchange` API endpoint, designed for exchanging an external authentication code for internal session tokens.

---

### **API Endpoint: `/api/v1/auth/token-exchange`**

### 1. Endpoint Overview

*   **Endpoint Path**: `/api/v1/auth/token-exchange`
*   **HTTP Method**: `POST`
*   **Consumes**: `application/json`
*   **Produces**: `application/json`
*   **Purpose**: This endpoint is responsible for taking an authorization code obtained from an external Identity Provider (IdP) (e.g., OAuth2, OpenID Connect) and exchanging it for internal API access tokens (JWTs) and a secure session cookie. It serves as the bridge between external authentication success and the establishment of a secure, internal user session.
*   **Controller Method**: `AuthController.exchangeToken()`
*   **Primary Function**: Facilitates the secure issuance of internal API access tokens and refresh tokens, and manages the associated session cookie after successful external identity verification.

### 2. Request and Response

*   **Request Type**:
    *   **Input Parameters**: The endpoint expects a JSON payload containing the external authorization code and potentially a `state` parameter for security validation.
    *   **Payload Structure (Example)**:
        ```json
        {
          "authCode": "your_external_authorization_code_here",
          "state": "your_original_state_parameter"
        }
        ```
*   **Response Type**:
    *   **Success Response (HTTP Status: 200 OK)**:
        *   **Payload**: A JSON object containing the `accessToken` (a short-lived JWT), `refreshToken` (a long-lived JWT), and potentially user profile information.
        *   **Headers**: `Set-Cookie` header containing the `session_token` cookie.
        *   **Cookies**:
            *   `session_token`: An `HttpOnly`, `Secure`, `SameSite=Lax` cookie containing the `accessToken`. This cookie is primarily for browser-based applications to manage the user session automatically.
        *   **Payload Structure (Example)**:
            ```json
            {
              "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
              "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
              "userId": "internal_user_id_123",
              "username": "johndoe",
              "roles": ["USER", "ADMIN"]
            }
            ```
    *   **Error Response**: (See section 7. Error Handling for details)

### 3. Call Hierarchy

The `exchangeToken` endpoint orchestrates several key operations, involving multiple services, to securely establish a user session. The flow is as follows:

1.  **`AuthController.exchangeToken(authCode, state)`**:
    *   **Purpose**: Main entry point for the token exchange process.
    *   **Operations**:
        *   Receives `authCode` and `state` from the client.
        *   Initiates external code validation.
        *   Manages user creation/lookup.
        *   Generates internal tokens and cookies.
        *   Logs audit events.
        *   Handles potential exceptions.
    *   **Calls**:
        *   `externalIdentityProviderService.validateAuthCode(authCode)`:
            *   **Purpose**: Communicates with the external Identity Provider (e.g., Google, Okta) to verify the provided `authCode`. It exchanges the code for user profile data.
            *   **Input**: External authorization code (`authCode`).
            *   **Output**: `UserProfile` object (containing `userId`, `email`, `roles` as provided by the IdP).
            *   **Potential Errors**: `InvalidAuthCodeException`, `IdPCommunicationException` (e.g., network issues, IdP configuration errors).
        *   `userService.findOrCreateUser(userProfile)`:
            *   **Purpose**: Checks if a user already exists in our internal database based on the `UserProfile` data (specifically, the external `userId`). If not, it creates a new user record.
            *   **Input**: `UserProfile` received from the `externalIdentityProviderService`.
            *   **Output**: `InternalUserEntity` (representing the user's internal record, including our internal `userId`, `username`, and `roles`).
            *   **Internal Calls**:
                *   `userRepository.findByExternalId(externalId)`: Queries the database to find an existing user.
                *   `userRepository.save(newUser)`: If no user is found, a new user record is persisted.
            *   **Potential Errors**: `DatabaseOperationException`.
        *   `jwtService.generateAccessToken(internalUserEntity)`:
            *   **Purpose**: Creates a short-lived JSON Web Token (JWT) used for subsequent API requests.
            *   **Input**: `InternalUserEntity` (specifically `userId` and `roles`).
            *   **Output**: Signed `accessToken` string.
            *   **Details**: The token is signed using HS256 and typically has a short expiration time (e.g., 15 minutes). It contains claims like `userId` and `roles`.
        *   `jwtService.generateRefreshToken(internalUserEntity)`:
            *   **Purpose**: Creates a long-lived JWT used to obtain new access tokens once the current access token expires.
            *   **Input**: `InternalUserEntity` (specifically `userId`).
            *   **Output**: Signed `refreshToken` string.
            *   **Details**: Signed using HS256 and has a longer expiration (e.g., 7 days). It contains the `userId` claim.
        *   `tokenRepository.saveRefreshToken(refreshToken, internalUserEntity.userId)`:
            *   **Purpose**: Persists the `refreshToken` in the database. This allows for token revocation and ensures that refresh tokens can only be used once or are tied to a specific session.
            *   **Input**: `refreshToken` string and the `userId` associated with it.
            *   **Potential Errors**: `DatabaseOperationException`.
        *   `cookieService.createSessionCookie(accessToken)`:
            *   **Purpose**: Generates a secure `HttpOnly` cookie containing the `accessToken` for browser-based session management.
            *   **Input**: The `accessToken` string.
            *   **Output**: `HttpCookie` object configured with appropriate security flags.
            *   **Details**: The cookie is set with `HttpOnly` (prevents client-side script access), `Secure` (only sent over HTTPS), and `SameSite=Lax` (mitigates CSRF risks).
        *   `auditService.logLoginEvent(internalUserEntity.userId, "SUCCESS")`:
            *   **Purpose**: Records a successful login event for auditing and security monitoring. This is typically an asynchronous operation to avoid blocking the main request flow.
            *   **Input**: `userId` and event status.
        *   `exceptionHandler.handle(Exception)` (Global):
            *   **Purpose**: Catches and processes any exceptions thrown during the execution flow, ensuring a consistent error response format to the client and proper internal logging.
            *   **Input**: The thrown `Exception` object.
            *   **Output**: Standardized `ErrorResponseDTO` and an appropriate HTTP status code (e.g., 400 Bad Request, 401 Unauthorized, 500 Internal Server Error).

### 4. Key Operations

1.  **External Authorization Code Validation**: Verifies the authenticity and validity of the authorization code with the external Identity Provider. This is a critical security step to ensure the code hasn't been tampered with or is expired.
2.  **User Provisioning (Find or Create)**: Manages user accounts by either linking the external identity to an existing internal user or creating a new internal user record if it's their first login. This ensures all users have a consistent internal representation.
3.  **JWT Access Token Generation**: Creates a short-lived, signed JSON Web Token (JWT) that will be used for subsequent API calls, providing proof of authentication and authorization.
4.  **JWT Refresh Token Generation & Storage**: Creates a long-lived, signed JWT for refreshing expired access tokens without requiring the user to re-authenticate with the external IdP. This token is securely stored in the database for revocation purposes.
5.  **Secure Session Cookie Creation**: Generates an `HttpOnly` and `Secure` cookie containing the access token. This cookie simplifies session management for browser-based clients and enhances security by preventing client-side script access to the token.
6.  **Audit Logging**: Records successful login events for security monitoring, compliance, and debugging purposes.

### 5. Dependencies

*   **Data Models/DTOs**:
    *   `ExternalAuthCodeRequest`: Represents the incoming request payload.
    *   `TokenExchangeResponse`: Represents the successful response payload containing tokens and user details.
    *   `UserProfile`: Intermediate data structure carrying user information from the IdP.
    *   `InternalUserEntity`: Database entity representing an internal user.
    *   `ErrorResponseDTO`: Standardized error response structure.
*   **Services**:
    *   `externalIdentityProviderService`: Manages communication and validation with external IdPs.
    *   `userService`: Handles internal user account management (lookup, creation).
    *   `jwtService`: Responsible for generating and signing JWTs (access and refresh tokens).
    *   `cookieService`: Manages the creation and configuration of secure HTTP cookies.
    *   `auditService`: Handles logging of security-related events.
*   **Repositories**:
    *   `userRepository`: Interacts with the database for `InternalUserEntity` operations.
    *   `tokenRepository`: Persists and manages refresh tokens in the database.
*   **Libraries/Frameworks**:
    *   Spring Framework (for controller, services, dependency injection)
    *   Spring Security (for general security context, potentially OAuth2 client support)
    *   JWT Library (e.g., JJWT for token generation/validation)
    *   Database ORM (e.g., Hibernate/JPA for data persistence)
    *   Logging Framework (e.g., SLF4J, Logback)

### 6. Security Features

*   **JWT Token Security**:
    *   **Signing**: Both access and refresh tokens are cryptographically signed (e.g., using HS256) to ensure their integrity and authenticity, preventing tampering.
    *   **Expiration**: Access tokens are short-lived (e.g., 15 minutes) to minimize the impact of token compromise. Refresh tokens are longer-lived but managed securely.
    *   **Claims**: Tokens include essential user claims (e.g., `userId`, `roles`) but avoid sensitive information.
*   **Cookie Security**:
    *   `HttpOnly`: The `session_token` cookie is marked `HttpOnly`, preventing client-side JavaScript from accessing it, which mitigates XSS (Cross-Site Scripting) attacks.
    *   `Secure`: The cookie is marked `Secure`, ensuring it is only sent over HTTPS connections, protecting against eavesdropping.
    *   `SameSite=Lax`: The cookie is set with `SameSite=Lax`, which helps mitigate CSRF (Cross-Site Request Forgery) attacks by restricting when the browser sends the cookie with cross-site requests.
*   **State Parameter Validation**: Although not explicitly detailed in the assumed hierarchy, it's highly probable that the `state` parameter from the OAuth2/OIDC flow is validated to prevent CSRF attacks during the redirect from the IdP. This involves comparing the received `state` with a value stored in the user's session or a cookie.
*   **Input Validation**: All incoming request parameters (e.g., `authCode`, `state`) are rigorously validated to prevent injection attacks and ensure data integrity.
*   **Refresh Token Management**: Refresh tokens are stored in the database, allowing for server-side revocation (e.g., upon user logout or suspicion of compromise). This also enables single-use refresh token strategies.
*   **Audit Logging**: Detailed logs of login events provide an audit trail, crucial for detecting and responding to security incidents.

### 7. Error Handling

*   **Centralized Exception Handling**: A global `exceptionHandler` component is likely used to catch and process various types of exceptions, ensuring a consistent error response format.
*   **Error Types**:
    *   **`InvalidAuthCodeException`**: Occurs if the external authorization code is invalid, expired, or has been used previously. Returns an HTTP 400 Bad Request or 401 Unauthorized.
    *   **`IdPCommunicationException`**: Raised if there are issues communicating with the external Identity Provider (e.g., network timeout, IdP server error). Returns an HTTP 500 Internal Server Error.
    *   **`DatabaseOperationException`**: Occurs during issues with user or token persistence (e.g., database connection failure, unique constraint violation). Returns an HTTP 500 Internal Server Error.
    *   **`InputValidationException`**: If the request payload is malformed or missing required parameters. Returns an HTTP 400 Bad Request.
    *   **General `Exception`**: Catches any other unexpected runtime errors, returning an HTTP 500 Internal Server Error.
*   **Error Response Structure (Example)**:
    ```json
    {
      "timestamp": "2023-10-27T10:30:00Z",
      "status": 400,
      "error": "Bad Request",
      "message": "Invalid external authorization code provided."
    }
    ```
*   **Logging**: All errors are logged internally with appropriate severity levels (e.g., `ERROR` for critical failures, `WARN` for recoverable issues) to aid in debugging and monitoring. Sensitive details are masked in logs.

### 8. Performance Considerations

*   **Efficient Token Generation**: JWT generation is a computationally efficient operation, often leveraging pre-configured secret keys and optimized libraries.
*   **Asynchronous Audit Logging**: The `auditService.logLoginEvent` call is likely asynchronous (e.g., via a message queue or separate thread) to prevent it from blocking the main request flow and impacting response times.
*   **Optimized Database Operations**: User lookup (`userRepository.findByExternalId`) and refresh token storage (`tokenRepository.saveRefreshToken`) are designed to be efficient, potentially leveraging database indexing on `externalId` and `userId` columns.
*   **Minimizing Cookie Size**: The `session_token` cookie only contains the `accessToken` (a relatively small JWT), keeping cookie overhead minimal.
*   **Reduced Round Trips**: By generating both access and refresh tokens, the endpoint reduces the need for frequent re-authentications with the external IdP, improving user experience and system efficiency over time.

### 9. Usage Pattern

This endpoint is typically called immediately after a user successfully authenticates with an external Identity Provider (e.g., Google, Facebook, corporate SSO).

1.  **User Initiates Login**: A user clicks "Login with X" on the client application.
2.  **Redirect to IdP**: The client redirects the user's browser to the external IdP's authentication page.
3.  **IdP Authentication**: The user authenticates with the external IdP.
4.  **IdP Redirects Back**: The IdP redirects the user's browser back to our client application with an `authCode` and `state` parameter in the URL.
5.  **Client Calls `/api/v1/auth/token-exchange`**: The client application (e.g., a Single Page Application or mobile app) extracts the `authCode` (and `state`) from the URL and sends it in a `POST` request to this `/api/v1/auth/token-exchange` endpoint.
6.  **Server Processes Request**: The backend processes the `authCode` as described in the Call Hierarchy, issuing internal tokens and setting the `session_token` cookie.
7.  **Client Receives Tokens**: The client receives the `accessToken` and `refreshToken` in the JSON response, and the `session_token` cookie is automatically set by the browser.
8.  **Subsequent API Calls**: The client can then use the `accessToken` (either from the JSON response or automatically sent via the `session_token` cookie for browser clients) for subsequent authenticated API requests.

**Prerequisites**:
*   The client application must have successfully initiated an OAuth2/OIDC flow with an external Identity Provider and obtained a valid authorization code.
*   The external Identity Provider must be correctly configured in the backend's `externalIdentityProviderService`.

### 10. Additional Notes

*   **Refresh Token Revocation**: While the endpoint generates and stores refresh tokens, a separate mechanism or endpoint would be required for users to explicitly revoke their refresh tokens (e.g., logout from all devices).
*   **IdP Configuration**: Proper configuration of client IDs, secrets, redirect URIs, and scopes with the external IdP is crucial for the `externalIdentityProviderService` to function correctly.
*   **Scalability**: The design emphasizes stateless access tokens (JWTs) and a centralized refresh token management system, which generally scales well for high-traffic environments.
*   **Rate Limiting**: It is highly recommended to implement rate limiting on this endpoint to prevent brute-force attacks on external authorization codes or to mitigate denial-of-service attempts.