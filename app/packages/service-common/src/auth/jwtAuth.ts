import type { FastifyRequest, FastifyReply } from "fastify";
import { createRemoteJWKSet, jwtVerify, type JWTPayload } from "jose";

/**
 * JWT claims expected in the token payload
 */
export interface JwtClaims extends JWTPayload {
    /**
     * Project ID from the JWT
     */
    projectId?: string;
    
    /**
     * Execution ID from the JWT
     */
    executionId?: string;
    
    /**
     * Scopes/permissions granted by the token
     */
    scope?: string[];
}

/**
 * Authentication middleware that validates JWT tokens using the backend's JWKS endpoint.
 * This middleware extracts the JWT from the Authorization header, validates it using
 * the backend's public keys, and attaches the decoded payload to the request.
 */
export class JwtAuthMiddleware {
    private jwks: ReturnType<typeof createRemoteJWKSet>;
    private issuer: string;
    
    /**
     * Creates a new JWT authentication middleware.
     * 
     * @param backendUrl - Base URL of the backend (e.g., "http://localhost:8080")
     * @param issuer - Expected issuer in JWT tokens (default: "mdeo-backend")
     */
    constructor(backendUrl: string, issuer: string = "mdeo-backend") {
        // Create JWKS client that will fetch and cache public keys from the backend
        this.jwks = createRemoteJWKSet(new URL(`${backendUrl}/api/auth/jwks`));
        this.issuer = issuer;
    }
    
    /**
     * Fastify hook that validates JWT tokens.
     * This can be used as a preHandler hook for routes that need authentication.
     * 
     * @param request - Fastify request object
     * @param reply - Fastify reply object
     * @throws Will send 401 response if authentication fails
     * 
     * @example
     * ```typescript
     * const authMiddleware = new JwtAuthMiddleware("http://localhost:8080");
     * fastify.addHook("preHandler", authMiddleware.authenticate.bind(authMiddleware));
     * ```
     */
    async authenticate(request: FastifyRequest, reply: FastifyReply): Promise<void> {
        try {
            const authHeader = request.headers.authorization;
            
            if (!authHeader) {
                return reply.status(401).send({ error: "Missing Authorization header" });
            }
            
            if (!authHeader.startsWith("Bearer ")) {
                return reply.status(401).send({ error: "Invalid Authorization header format. Expected 'Bearer <token>'" });
            }
            
            const token = authHeader.substring(7);
            
            // Verify the JWT using the backend's public keys
            const { payload } = await jwtVerify<JwtClaims>(token, this.jwks, {
                issuer: this.issuer,
                algorithms: ["RS256"]
            });
            
            // Attach the validated claims to the request for use in route handlers
            (request as any).jwtClaims = payload;
        } catch (error) {
            // Security: Log errors internally but don't expose details to client
            // This prevents information leakage about JWT internals
            if (error instanceof Error) {
                request.log.warn(`JWT verification failed: ${error.message}`);
            } else {
                request.log.warn(`JWT verification failed with unknown error`);
            }
            return reply.status(401).send({ error: "Authentication failed" });
        }
    }
    
    /**
     * Helper method to get JWT claims from an authenticated request.
     * This should only be called in routes protected by the authenticate middleware.
     * 
     * @param request - Fastify request object
     * @returns The JWT claims, or undefined if not authenticated
     * 
     * @example
     * ```typescript
     * const claims = JwtAuthMiddleware.getClaims(request);
     * if (claims) {
     *     console.log("Project ID:", claims.projectId);
     * }
     * ```
     */
    static getClaims(request: FastifyRequest): JwtClaims | undefined {
        return (request as any).jwtClaims;
    }
    
    /**
     * Checks if the JWT token has a specific scope/permission.
     * 
     * @param request - Fastify request object
     * @param scope - The required scope to check for
     * @returns true if the token has the scope, false otherwise
     * 
     * @example
     * ```typescript
     * if (JwtAuthMiddleware.hasScope(request, "file-data:read")) {
     *     // User has permission to read file data
     * }
     * ```
     */
    static hasScope(request: FastifyRequest, scope: string): boolean {
        const claims = JwtAuthMiddleware.getClaims(request);
        if (!claims || !claims.scope) {
            return false;
        }
        return claims.scope.includes(scope);
    }
}
