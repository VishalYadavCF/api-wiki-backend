This document provides a comprehensive overview of the `POST /api/v1/auth/exchange-token` API endpoint, designed for developers and architects to understand its functionality, underlying mechanisms, and integration points.

---

### API Endpoint Documentation: `POST /api/v1/auth/exchange-token`

---

#### 1. Endpoint Overview

*   **Endpoint Path**: `/api/v1/auth/exchange-token`
*   **HTTP Method**: `POST`
*   **Consumes**: `application/json` (expected input payload)
*   **Produces**: `application/json` (expected output payload)
*   **Controller Method**: `AuthController.exchangeToken`
*   **Purpose**: This endpoint serves as a crucial component of the authentication flow. Its primary function is to securely exchange an external authentication token (e.g., from an OAuth2 provider or an existing identity system) for internal session tokens (an Access Token and a Refresh Token) issued by this application. These internal tokens establish and maintain the user's session within the application, typically delivered via secure HTTP-only cookies.

#### 2. Request and Response

**Request Structure**:
The request body is a JSON object containing the external token.

*   **HTTP Method**: `POST`
*   **Example Request Body**:
    ```json
    {
        "externalToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiZW1haWwiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
    }
    ```
*   **Input Parameters**:
    *   `externalToken` (string, required): The JSON Web Token (JWT) or opaque token obtained from an external identity provider or authentication system. This token carries the user's identity information.

**Response Structure**:
Upon successful exchange, the API returns a success status with a JSON body containing the Access Token and the user's ID. Crucially, the Refresh Token and a separate Access Token are also set as secure HTTP-only cookies in the client's browser.

*   **Success Response (HTTP Status: 200 OK)**:
    ```json
    {
        "message": "Authentication successful",
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyX2lkIiwiZXhwIjoxNTE2MjM5MDgyfQ.Signature",
        "userId": "user_id_123"
    }
    ```
*   **Success Response Headers**:
    *   `Set-Cookie`: `refreshToken=<jwt_refresh_token>; Path=/; Expires=<date>; HttpOnly; Secure; SameSite=Lax`
    *   `Set-Cookie`: `accessToken=<jwt_access_token>; Path=/; Expires=<date>; HttpOnly; Secure; SameSite=Lax`
*   **Response Details**:
    *   `accessToken` (string): The short-lived JWT Access Token provided in the response body for immediate client-side use (e.g., for front-end frameworks that might need to read it, though the primary access token for API calls is cookie-based).
    *   `userId` (string): The unique identifier of the authenticated user within the application's system.
    *   `refreshToken` (cookie): A long-lived JWT stored as an `HttpOnly`, `Secure`, `SameSite=Lax` cookie. Used to obtain new Access Tokens when the current one expires.
    *   `accessToken` (cookie): A short-lived JWT stored as an `HttpOnly`, `Secure`, `SameSite=Lax` cookie. This is the primary token used by the client for subsequent authenticated API requests.

#### 3. Call Hierarchy

The `exchangeToken` endpoint orchestrates several key operations across different service layers to process the external token and establish an internal session.

*   **`AuthController.exchangeToken(String externalToken, HttpServletResponse response)`**
    *   **Role**: The primary entry point for the API call. It receives the external token from the client, coordinates the authentication process, and constructs the final HTTP response, including setting cookies.
    *   **Invokes**:
        *   **`JwtService.validateExternalToken(externalToken)`**
            *   **Role**: Responsible for parsing, validating the signature, checking the expiration, and verifying the issuer of the provided `externalToken`. It ensures the token is genuine and untampered.
            *   **Input**: `externalToken` (String)
            *   **Output**: A `DecodedJWT` object containing the claims (e.g., subject, email) from the external token.
        *   **`UserService.findOrCreateUser(decodedJWT.getSubject(), decodedJWT.getClaim("email"))`**
            *   **Role**: Based on the validated external token's claims (typically the subject and email), this service method checks if a user already exists in the application's database. If not, it creates a new user profile.
            *   **Input**: User identifier (e.g., `subject` from `decodedJWT`), user email (e.g., `email` claim from `decodedJWT`).
            *   **Output**: A `User` entity representing the authenticated or newly created user.
        *   **`JwtService.generateAccessToken(user.getUserId())`**
            *   **Role**: Creates a new, short-lived JWT that serves as the internal Access Token for subsequent authenticated API requests.
            *   **Input**: `user.getUserId()` (String)
            *   **Output**: `accessToken` (String) - the generated JWT string.
        *   **`JwtService.generateRefreshToken(user.getUserId())`**
            *   **Role**: Creates a new, long-lived JWT that serves as the internal Refresh Token. This token is used to acquire new Access Tokens without requiring re-authentication via the external system.
            *   **Input**: `user.getUserId()` (String)
            *   **Output**: `refreshToken` (String) - the generated JWT string.
        *   **`CookieUtils.addSecureHttpOnlyCookie(response, "refreshToken", refreshToken, refreshExpiry)`**
            *   **Role**: A utility method to securely add the generated `refreshToken` to the HTTP response as a cookie. It configures the cookie with `HttpOnly`, `Secure`, and `SameSite` flags.
            *   **Input**: `HttpServletResponse` object, cookie name ("refreshToken"), `refreshToken` string, `refreshExpiry` (long - cookie expiration time).
            *   **Output**: Modifies the `HttpServletResponse` to include the `Set-Cookie` header.
        *   **`CookieUtils.addSecureHttpOnlyCookie(response, "accessToken", accessToken, accessExpiry)`**
            *   **Role**: Similar to the above, this utility method adds the generated `accessToken` to the HTTP response as a cookie, configured with `HttpOnly`, `Secure`, and `SameSite` flags.
            *   **Input**: `HttpServletResponse` object, cookie name ("accessToken"), `accessToken` string, `accessExpiry` (long - cookie expiration time).
            *   **Output**: Modifies the `HttpServletResponse` to include the `Set-Cookie` header.
        *   **`AuthController.buildAuthSuccessResponse(accessToken, user.getUserId())`**
            *   **Role**: Constructs the final `ResponseEntity` containing the success message, the `accessToken` (for the response body), and the `userId`.
            *   **Input**: `accessToken` (String), `user.getUserId()` (String)
            *   **Output**: `ResponseEntity<?>` ready to be sent as the API response.

#### 4. Key Operations

1.  **External Token Validation**: The incoming `externalToken` is rigorously validated to ensure its authenticity, integrity, and validity period. This is critical for trusting the external identity assertion.
2.  **User Provisioning/Lookup**: The system intelligently handles user accounts: if the user identified by the external token exists in the application's database, their profile is retrieved; otherwise, a new user account is provisioned.
3.  **Internal Token Generation**: Two distinct JSON Web Tokens (JWTs) are generated:
    *   **Access Token**: A short-lived token for authorizing subsequent requests to protected API resources.
    *   **Refresh Token**: A long-lived token used to securely obtain new Access Tokens without requiring the user to re-authenticate from the external system.
4.  **Secure Cookie Management**: Both the Access and Refresh Tokens are delivered to the client as highly secure HTTP-only cookies. This prevents client-side JavaScript access, reduces XSS risks, and ensures tokens are sent automatically with subsequent requests.
5.  **Response Construction**: The endpoint crafts a clear success response, providing the user's ID and a copy of the Access Token in the JSON body, while simultaneously setting the secure cookies.

#### 5. Dependencies

*   **Request/Response Entities**:
    *   `ExchangeTokenRequest` (or similar DTO for `externalToken` input)
    *   `AuthResponse` (or similar DTO for `accessToken`, `userId` output)
*   **Services**:
    *   `JwtService`: Handles all JWT-related operations (validation, generation).
    *   `UserService`: Manages user data (lookup, creation).
    *   `CookieUtils`: Utility class for secure cookie handling.
*   **Database Entities/Tables**:
    *   `User` (or `ApplicationUser`): Stores user profiles and their unique identifiers.
*   **Libraries/Frameworks**:
    *   Spring Framework (for controller, services, dependency injection).
    *   Spring Security (inferred for security context and `HttpServletResponse` handling).
    *   JWT Library (e.g., Auth0 Java JWT, JJWT) for token parsing and generation.
    *   `javax.servlet.http.HttpServletResponse` (for direct cookie manipulation).
    *   Logging Utilities (e.g., SLF4J, Logback) for internal logging.

#### 6. Security Features

*   **JWT Security**:
    *   **External Token Validation**: Ensures the `externalToken` is valid, digitally signed, and untampered, preventing unauthorized access through forged tokens.
    *   **Internal Token Signing and Expiration**: Both generated Access and Refresh Tokens are cryptographically signed to prevent tampering and include expiration times to limit their validity and mitigate risks from token theft.
*   **Cookie Security**:
    *   **`HttpOnly`**: Tokens stored in cookies are inaccessible via client-side JavaScript, significantly reducing the risk of Cross-Site Scripting (XSS) attacks.
    *   **`Secure`**: Cookies are only transmitted over HTTPS, protecting them from interception by man-in-the-middle attacks.
    *   **`SameSite=Lax`**: Provides protection against Cross-Site Request Forgery (CSRF) by preventing the browser from sending cookies with cross-site requests, except for top-level navigations.
*   **Input Validation**: Although not explicitly shown, it's assumed that the `externalToken` undergoes basic validation (e.g., non-null, correct format) before `JwtService.validateExternalToken` is called.
*   **CORS Handling**: The endpoint must be configured to appropriately handle Cross-Origin Resource Sharing (CORS) to allow legitimate client applications from different domains to access it securely.

#### 7. Error Handling

*   **Invalid External Token**: If the `JwtService.validateExternalToken` fails (e.g., invalid signature, expired token, malformed token), the API will typically return an HTTP 401 Unauthorized or 400 Bad Request status code with a descriptive error message in the response body.
*   **User Management Errors**: Issues during user lookup or creation (e.g., database connectivity problems) would result in an HTTP 500 Internal Server Error.
*   **Internal Token Generation Errors**: Failures during the generation of internal JWTs would also likely lead to an HTTP 500 status.
*   **Logging**: All significant errors are logged internally with sufficient detail (e.g., stack traces, error codes) for debugging and monitoring purposes.
*   **Error Response Structure**: For most errors, a consistent JSON error response structure is returned to the client, typically including an `errorCode` and a `message`. Example:
    ```json
    {
        "error": "Unauthorized",
        "message": "Invalid or expired external token.",
        "statusCode": 401
    }
    ```

#### 8. Performance Considerations

*   **Efficient JWT Operations**: The `JwtService` is designed to perform token validation and generation efficiently, leveraging optimized libraries to minimize CPU overhead.
*   **Database Lookup Optimization**: The `UserService.findOrCreateUser` operation should be optimized with proper database indexing on user identifiers (e.g., `subject`, `email`) to ensure fast lookups and inserts.
*   **Minimization of Overhead**:
    *   **Cookie Size**: While JWTs can be large, efforts are made to keep claims minimal to reduce HTTP header size for each subsequent request.
    *   **Response Time**: The overall latency of the endpoint is a critical performance metric, monitored to ensure a smooth user experience during authentication.
*   **Metrics Collection**: It's highly probable that metrics are collected for this endpoint (e.g., request count, latency, error rates) to monitor its performance and health in production.

#### 9. Usage Pattern

This endpoint is typically called as the **second step in an authentication flow** that leverages an external identity provider or a pre-existing token from another system.

1.  **Prerequisite**: The client application (e.g., a Single Page Application, mobile app) first authenticates the user successfully with an external Identity Provider (e.g., Google, Okta, ADFS) or receives an authentication token from another trusted service.
2.  **Invocation**: The client then sends the `externalToken` obtained from this initial authentication step to the `/api/v1/auth/exchange-token` endpoint.
3.  **Session Establishment**: Upon successful processing, the application issues its own internal Access and Refresh Tokens as secure cookies. These cookies are automatically managed by the browser and sent with subsequent requests to the application's protected APIs, establishing the user's session.
4.  **Subsequent Calls**: The client no longer needs to interact with the external token; all subsequent API calls are authorized using the internal Access Token stored in the cookie. When the Access Token expires, the client can use the Refresh Token to obtain a new Access Token.

This pattern is ideal for integrating with third-party authentication services, facilitating Single Sign-On (SSO), or migrating users from legacy systems by exchanging their existing tokens for application-specific sessions.

#### 10. Additional Notes

*   **Token Lifetimes**: The Access Token is typically short-lived (e.g., 15-30 minutes) for security, while the Refresh Token is long-lived (e.g., days, weeks) to reduce re-authentication frequency.
*   **Session Management**: This endpoint primarily establishes a *stateless* session based on JWTs. The server does not maintain session state explicitly; rather, it validates the tokens on each request.
*   **Scalability**: The stateless nature of JWTs makes this endpoint highly scalable, as any server instance can validate the tokens independently without needing to query a centralized session store.
*   **Extensibility**: The design allows for easy integration with different external identity providers by simply adapting the `JwtService.validateExternalToken` method to handle various external token formats or validation mechanisms.