# Backend Deployment

## Required Environment

Set the production profile explicitly:

```text
SPRING_PROFILES_ACTIVE=prod
```

Required variables:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_PRIVATE_KEY_LOCATION
JWT_PUBLIC_KEY_LOCATION
CORS_ALLOWED_ORIGINS
```

Production private keys must never be committed. Provide JWT keys as external secrets, for example:

```text
JWT_PRIVATE_KEY_LOCATION=file:/run/secrets/jwt-private.pem
JWT_PUBLIC_KEY_LOCATION=file:/run/secrets/jwt-public.pem
```

## Optional Environment

```text
PORT=8080
JWT_ISSUER=eid-cricket-fest
JWT_ACCESS_TOKEN_TTL=15m
JWT_REFRESH_TOKEN_TTL=30d
WEBSOCKET_ALLOWED_ORIGINS=https://example.com
DB_POOL_MAX_SIZE=10
DB_POOL_MIN_IDLE=2
DB_CONNECTION_TIMEOUT_MS=10000
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=INFO
SPRINGDOC_ENABLED=false
FORWARD_HEADERS_STRATEGY=none
```

Auth rate-limit overrides:

```text
AUTH_RATE_LIMIT_ENABLED=true
AUTH_RATE_LIMIT_CACHE_MAX_SIZE=10000
AUTH_RATE_LIMIT_CACHE_TTL=2h
AUTH_LOGIN_IP_CAPACITY=60
AUTH_LOGIN_IP_REFILL_TOKENS=60
AUTH_LOGIN_IP_REFILL_PERIOD=1m
AUTH_LOGIN_IDENTITY_CAPACITY=10
AUTH_LOGIN_IDENTITY_REFILL_TOKENS=10
AUTH_LOGIN_IDENTITY_REFILL_PERIOD=1m
AUTH_REGISTER_IP_CAPACITY=10
AUTH_REGISTER_IP_REFILL_TOKENS=10
AUTH_REGISTER_IP_REFILL_PERIOD=1h
AUTH_REFRESH_IP_CAPACITY=60
AUTH_REFRESH_IP_REFILL_TOKENS=60
AUTH_REFRESH_IP_REFILL_PERIOD=1m
```

## Health

Use readiness for traffic routing:

```text
/actuator/health/readiness
```

Liveness is also available:

```text
/actuator/health/liveness
```

Health endpoints are public so infrastructure can call them without a JWT. Other Actuator internals are not exposed.

## Container

The backend listens on container port `8080` unless `PORT` overrides it.

Build locally:

```text
docker build -t eid-cricket-fest-backend:local .
```

The image runs the Spring Boot jar with Java 25 as a non-root user. The database is a separate PostgreSQL service, not part of the backend image.

## Notes

The current Caffeine auth rate limiter is process-local. A single backend instance is covered. Horizontal scaling will need distributed rate limiting as a separate production enhancement.
