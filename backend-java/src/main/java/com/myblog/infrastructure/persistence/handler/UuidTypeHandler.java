package com.myblog.infrastructure.persistence.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * MyBatis 类型处理器：将 {@link UUID} 映射到 PostgreSQL 的 {@code uuid} 列。
 * 所有实体都继承 {@code BaseEntity}、以 UUID 为主键，而 JDBC 驱动没有内置
 * {@code java.util.UUID} 的绑定方式，因此需要此处理器。
 */
@MappedJdbcTypes(JdbcType.OTHER)
@MappedTypes(UUID.class)
public class UuidTypeHandler extends BaseTypeHandler<UUID> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType) throws SQLException {
        // 通过 PGobject 显式声明类型为 uuid，避免驱动按未知类型绑定失败
        PGobject o = new PGobject();
        o.setType("uuid");
        o.setValue(parameter.toString());
        ps.setObject(i, o);
    }

    @Override
    public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toUuid(rs.getString(columnName));
    }

    @Override
    public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toUuid(rs.getString(columnIndex));
    }

    @Override
    public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toUuid(cs.getString(columnIndex));
    }

    /** 将数据库读出的字符串还原为 UUID，null 透传 */
    private static UUID toUuid(String s) {
        return s == null ? null : UUID.fromString(s);
    }
}
