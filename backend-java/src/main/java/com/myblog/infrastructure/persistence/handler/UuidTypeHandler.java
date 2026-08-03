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
 * MyBatis type handler that maps a {@link UUID} to the PostgreSQL {@code uuid} column.
 * Required because every entity extends {@code BaseEntity} which declares a UUID primary key,
 * and the JDBC driver does not provide a built-in binding for {@code java.util.UUID}.
 */
@MappedJdbcTypes(JdbcType.OTHER)
@MappedTypes(UUID.class)
public class UuidTypeHandler extends BaseTypeHandler<UUID> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType) throws SQLException {
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

    private static UUID toUuid(String s) {
        return s == null ? null : UUID.fromString(s);
    }
}
