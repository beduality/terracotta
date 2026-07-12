# Proposal: Authentication Workflows

**Date**: 2025-07-10  
**Status**: Draft  
**Related**: Hangar Provider, External YAML Configuration

## Summary

Implement robust authentication handling for each platform:
- Modrinth: Static API key (no refresh)
- CurseForge: Static API key (no refresh)
- Hangar: JWT token with automatic refresh

## Problem Statement

Different platforms require different authentication patterns:

| Platform | Auth Type | Token Refresh | Notes |
|----------|-----------|---------------|-------|
| Modrinth | PAT | No | Static key in header |
| CurseForge | API Key | No | Static key in header |
| Hangar | JWT | Yes | Time-bound token |

Currently, only Modrinth is implemented with static token handling. Hangar requires a time-bound token with automatic refresh.

## Proposed Implementation

### 1. Authentication Manager Interface

```kotlin
interface AuthManager {
    val scheme: AuthScheme
    
    suspend fun getAuthHeader(): String
    suspend fun ensureValid()
    suspend fun invalidate()
}

enum class AuthScheme {
    STATIC_PAT,
    STATIC_API_KEY,
    TIME_BOUND_JWT
}
```

### 2. Static PAT Authentication (Modrinth, CurseForge)

```kotlin
class StaticPatAuthManager(private val token: String) : AuthManager {
    override val scheme = AuthScheme.STATIC_PAT
    
    override suspend fun getAuthHeader(): String {
        return "Bearer $token"  // Modrinth uses Bearer
    }
    
    override suspend fun ensureValid() {
        // No-op for static tokens
    }
    
    override suspend fun invalidate() {
        // No-op for static tokens
    }
}

class StaticApiKeyAuthManager(private val token: String) : AuthManager {
    override val scheme = AuthScheme.STATIC_API_KEY
    
    override suspend fun getAuthHeader(): String {
        return token  // CurseForge uses raw key
    }
    
    override suspend fun ensureValid() {
        // No-op for static tokens
    }
    
    override suspend fun invalidate() {
        // No-op for static tokens
    }
}
```

### 3. JWT Token with Automatic Refresh (Hangar)

```kotlin
class JwtAuthManager(
    private val apiKey: String,
    private val api: HangarApiClient,
    private val refreshMargin: Long = 60_000  // 60 seconds before expiry
) : AuthManager {
    private val lock = Mutex()
    private var jwt: String? = null
    private var expiresAt: Long = 0
    
    override val scheme = AuthScheme.TIME_BOUND_JWT
    
    override suspend fun getAuthHeader(): String {
        ensureValid()
        return "Bearer $jwt!!"
    }
    
    override suspend fun ensureValid() {
        if (needsRefresh()) {
            lock.suspendCancellableCoroutine { cont ->
                refresh()
                    .invokeOnCompletion { cont.resume(it) }
            }
        }
    }
    
    override suspend fun invalidate() {
        jwt = null
        expiresAt = 0
    }
    
    private suspend fun needsRefresh(): Boolean {
        if (jwt == null) return true
        return System.currentTimeMillis() > (expiresAt - refreshMargin)
    }
    
    private suspend fun refresh(): Result<Unit> {
        return try {
            val response = api.post("/api/v1/authenticate") {
                parameter("apiKey", apiKey)
            }
            
            jwt = response.jwt
            expiresAt = response.expiresAt
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 4. Authentication Manager Factory

```kotlin
object AuthManagerFactory {
    fun create(
        platform: String,
        token: String?
    ): AuthManager? {
        return when (platform.lowercase()) {
            "modrinth" -> token?.let { StaticPatAuthManager(it) }
            "curseforge" -> token?.let { StaticApiKeyAuthManager(it) }
            "hangar" -> token?.let { JwtAuthManager(it, HangarApiClient()) }
            else -> null
        }
    }
}
```

### 5. Integration with API Client

```kotlin
class HangarApiClient(
    private val baseUrl: String,
    private val authManager: AuthManager
) {
    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
        install(Auth) {
            bearer {
                loadTokens {
                    BearerTokens(authManager.getAuthHeader(), "")
                }
            }
        }
    }
    
    suspend fun <T> get(path: String, block: RequestBuilder.() -> Unit = {}): T {
        return client.get("$baseUrl$path", block)
    }
    
    suspend fun <T> post(path: String, block: RequestBuilder.() -> Unit = {}): T {
        return client.post("$baseUrl$path", block)
    }
}
```

### 6. Background Token Refresh (Optional Enhancement)

For very long-running processes, consider background refresh:

```kotlin
class RefreshingJwtAuthManager(
    apiKey: String,
    api: HangarApiClient
) : AuthManager {
    private val tokenManager = JwtAuthManager(apiKey, api)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var refreshJob: Job? = null
    
    init {
        scheduleBackgroundRefresh()
    }
    
    private fun scheduleBackgroundRefresh() {
        refreshJob?.cancel()
        
        refreshJob = scope.launch {
            while (true) {
                delay(calculateRefreshDelay())
                tokenManager.refresh()
            }
        }
    }
    
    private fun calculateRefreshDelay(): Long {
        // Calculate delay based on current token expiry
        val expiry = tokenManager.expiresAt
        val now = System.currentTimeMillis()
        return maxOf(60_000, expiry - now - 60_000)  // 60s buffer
    }
}
```

## Token Refresh Strategies

### Strategy 1: Lazy Refresh (Recommended for most cases)
- Check token validity before each request
- Refresh only if needed
- Simple, low memory overhead

### Strategy 2: Background Refresh (Recommended for long-running tools)
- Background coroutine refreshes token periodically
- No delays during active requests
- More complex, higher memory overhead

### Strategy 3: Hybrid (Recommended for CLI tools)
- Lazy refresh for interactive usage
- Background refresh only if token is within 5 minutes of expiry

## Error Handling

```kotlin
class AuthException(message: String, val statusCode: Int? = null) : Exception(message)

class JwtAuthManager(...) {
    override suspend fun refresh(): Result<Unit> {
        return try {
            val response = api.post("/api/v1/authenticate") {
                parameter("apiKey", apiKey)
            }
            
            jwt = response.jwt
            expiresAt = response.expiresAt
            
            Result.success(Unit)
        } catch (e: ApiException) {
            when (e.statusCode) {
                401 -> Result.failure(AuthException("Invalid API key", 401))
                429 -> Result.failure(AuthException("Rate limited", 429))
                else -> Result.failure(e)
            }
        }
    }
}
```

## Testing Strategy

### Unit Tests
- Static PAT token header generation
- Static API key header generation
- JWT token refresh
- Token expiry calculation
- Background refresh scheduling

### Integration Tests
- Modrinth: Test with valid/invalid PAT
- CurseForge: Test with valid/invalid API key
- Hangar: Test JWT flow with valid/invalid API key

### Mock Tests
- Mock API responses for token exchange
- Test token refresh on expiry
- Test error handling for invalid tokens

## Migration Path

1. ✅ Define AuthManager interface
2. 🔄 Implement StaticPatAuthManager (Modrinth)
3. 🔄 Implement StaticApiKeyAuthManager (CurseForge)
4. 🔄 Implement JwtAuthManager (Hangar)
5. 🔄 Update API clients to use AuthManager
6. 🔄 Add authentication tests
7. 🔄 Document authentication setup

## Next Steps

1. Define AuthManager interface
2. Implement static PAT handler
3. Implement JWT handler with refresh
4. Integrate with API clients
5. Add tests
6. Document authentication setup

## References

- [Modrinth Authentication](https://docs.modrinth.com/api/authentication)
- [Hangar API Authentication](https://hangar.papermc.io/api/authentication)
- [CurseForge Authentication](https://www.curseforge.com/docs/api)
