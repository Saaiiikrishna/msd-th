package com.mysillydreams.treasure.config;

import com.mysillydreams.treasure.domain.model.AudienceType;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.*;

/**
 * Custom Hibernate UserType for PostgreSQL enum types.
 * This handles the conversion between Java enums and PostgreSQL enum types with proper casting.
 */
public class AudienceTypeConverter implements UserType<AudienceType> {

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<AudienceType> returnedClass() {
        return AudienceType.class;
    }

    @Override
    public boolean equals(AudienceType x, AudienceType y) throws HibernateException {
        if (x == y) return true;
        if (x == null || y == null) return false;
        return x.equals(y);
    }

    @Override
    public int hashCode(AudienceType x) throws HibernateException {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public AudienceType nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        String value = rs.getString(position);
        if (value == null) {
            return null;
        }
        try {
            return AudienceType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new SQLException("Unknown AudienceType: " + value, e);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, AudienceType value, int index, SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            // Cast the string value to the PostgreSQL enum type
            st.setObject(index, value.name(), Types.OTHER);
        }
    }

    @Override
    public AudienceType deepCopy(AudienceType value) throws HibernateException {
        return value; // Enums are immutable
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(AudienceType value) throws HibernateException {
        return value == null ? null : value.name();
    }

    @Override
    public AudienceType assemble(Serializable cached, Object owner) throws HibernateException {
        if (cached == null) {
            return null;
        }
        try {
            return AudienceType.valueOf((String) cached);
        } catch (IllegalArgumentException e) {
            throw new HibernateException("Unknown AudienceType: " + cached, e);
        }
    }
}
