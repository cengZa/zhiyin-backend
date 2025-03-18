package com.lcj.zhiyin.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class JwtUtil {

    // JWT 密钥(实际项目中应从配置文件加载)
    // 对称加密（HMAC）密钥不能太短，无法满足 HS256 / HS384 / HS512 等算法的安全要求。根据 RFC7518 规范，HMAC-SHA 类算法的密钥至少要 256 bits（32 bytes） 才算安全。
    private static final String secretString = "ZhiYinSuperSecretKeyForJwtAtLeast256Bits";
    private static final SecretKey key = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));

    // Token 过期时间 => 1h
    private static final long EXPIRE_TIME = 1 * 60 * 60 * 1000;
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    /**
     * 生成 JWT Token
     */
    public static String generateToken(String userAccount, String role) {
//        Map<String, Object> payload = new HashMap<>();
//        payload.put("userAccount", userAccount);

//        long exp = Instant.now().getEpochSecond() + EXPIRE_TIME;

//        payload.put("exp", exp);
        return Jwts.builder()
//                .claims(payload)
                .subject(userAccount)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .signWith(key)
                .compact();
    }

    /**
     * 解析 JWT Token 并返回用户账号
     */
    public static Claims validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
//                    .setSigningKey(key)
                    .verifyWith(key)
//                    .verifyWith((PublicKey) key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            log.info("claims: {}", claims);
            return claims;
        } catch (JwtException e) {
            // 任何解析异常均视为 Token 无效
            throw new RuntimeException("Token 无效或已过期", e);
        }
    }
}
