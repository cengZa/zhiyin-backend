package com.lcj.zhiyin.filter;

import com.lcj.zhiyin.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        log.info("得到了header中的Authorization = {}", header);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = JwtUtil.validateToken(token);
                String userAccount = claims.getSubject();
                String role = claims.get("role", String.class);
                log.info("从token中解析出用户 = {}, 角色 = {}", userAccount, role);

                // 检查 Redis 中是否存在该 Token
                String redisKey = "jwt:token:" + token;
                if(redisTemplate.opsForValue().get(redisKey) == null){
                    // 如果在 Redis 中不存在，则认为 Token 已注销或失效
                    throw new RuntimeException("Token 已注销或失效");
                }

                // 构造权限列表
                List<GrantedAuthority> authorities = new ArrayList<>();
                if("ADMIN".equals(role)){
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                }else{
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                }

                // 构造一个认证对象并存入 SecurityContextHolder
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userAccount, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // Token 无效则清除上下文，可记录日志
                log.warn("token 无效, 清除上下文");
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
