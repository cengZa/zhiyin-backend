package com.lcj.zhiyin.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Objects;

public class JsonTypeHandler<T> extends BaseTypeHandler<T> {

    private static final Logger log = LoggerFactory.getLogger(JsonTypeHandler.class);

    private static final ObjectMapper objectMapper = new ObjectMapper(); // 统一使用单例

    private final Class<T> clazz;

    public JsonTypeHandler(Class<T> clazz) {
        if (Objects.isNull(clazz)) {
            throw new IllegalArgumentException("Class type cannot be null");
        }
        this.clazz = clazz;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        try {
            String json = objectMapper.writeValueAsString(parameter);
            ps.setString(i, json);
        } catch (JsonProcessingException e) {
            log.error("JSON serialization error: {}", parameter, e);
            throw new SQLException("Error serializing object to JSON", e);
        }
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJson(json);
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJson(json);
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJson(json);
    }

    private T parseJson(String json) throws SQLException {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON deserialization error: {}", json, e);
            throw new SQLException("Error deserializing JSON", e);
        }
    }
}
