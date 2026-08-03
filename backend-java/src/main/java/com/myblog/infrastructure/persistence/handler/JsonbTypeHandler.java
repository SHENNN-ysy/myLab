package com.myblog.infrastructure.persistence.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.common.json.JacksonObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis type handler that serialises an arbitrary POJO to PostgreSQL {@code jsonb}.
 * Lives next to the entities that reference it.
 */
@MappedJdbcTypes(JdbcType.OTHER)
@MappedTypes(Object.class)
public class JsonbTypeHandler extends BaseTypeHandler<Object> {

    private static final ObjectMapper OM = JacksonObjectMapper.get();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {
        try {
            PGobject o = new PGobject();
            o.setType("jsonb");
            o.setValue(OM.writeValueAsString(parameter));
            ps.setObject(i, o);
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    @Override
    public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return read(rs.getString(columnName));
    }

    @Override
    public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return read(rs.getString(columnIndex));
    }

    @Override
    public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return read(cs.getString(columnIndex));
    }

    private Object read(String s) throws SQLException {
        if (s == null) {
            return null;
        }
        try {
            return OM.readValue(s, Object.class);
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }
}
