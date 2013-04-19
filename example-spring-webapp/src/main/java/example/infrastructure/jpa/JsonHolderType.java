package example.infrastructure.jpa;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.StringType;
import org.hibernate.type.TextType;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Objects;

import example.domain.shared.json.JsonHolder;
import example.domain.shared.json.JsonSerializationService;

@Component
public class JsonHolderType implements CompositeUserType {

	/**
	 * JSON serialization service is injected to the static field, Hibernate instantiates the instance when Spring
	 * context is not ready yet. As long as the component is a singleton, Spring is able to inject the dependency later.
	 */
	private static JsonSerializationService jsonSerializationService;

	@Autowired
	void setJsonSerializationService(JsonSerializationService jsonSerializationService) {
		JsonHolderType.jsonSerializationService = jsonSerializationService;
	}

	@Override
	public String[] getPropertyNames() {
		return new String[] { "type", "data" };
	}

	@Override
	public Type[] getPropertyTypes() {
		return new Type[] { StringType.INSTANCE, TextType.INSTANCE };
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getPropertyValue(Object component, int propertyIndex) throws HibernateException {
		if (component == null) {
			return null;
		}

		JsonHolder jsonHolder = (JsonHolder) component;
		if (jsonHolder.hasValue()) {
			return null;
		}

		switch (propertyIndex) {
		case 0:
			return jsonHolder.getValueType();
		case 1:
			return jsonSerializationService.toJson(jsonHolder.getValue());
		default:
			throw new HibernateException("Invalid property index [" + propertyIndex + "]");
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void setPropertyValue(Object component, int propertyIndex, Object value) throws HibernateException {
		if (component == null) {
			return;
		}

		JsonHolder json = (JsonHolder) component;

		switch (propertyIndex) {
		case 0:
			json.setType((String) value);
			break;
		case 1:
			json.setValue(jsonSerializationService.fromJson((String) value, json.getType()));
			break;
		default:
			throw new HibernateException("Invalid property index [" + propertyIndex + "]");
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {

		String type = StringType.INSTANCE.nullSafeGet(rs, names[0], session);
		String data = TextType.INSTANCE.nullSafeGet(rs, names[1], session);

		if (type == null && data == null) {
			return null;
		} else {
			Object json = jsonSerializationService.fromJson(data, type);
			return new JsonHolder(json);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session)
			throws HibernateException, SQLException {
		if (value == null) {
			StringType.INSTANCE.set(st, null, index, session);
			TextType.INSTANCE.set(st, null, index + 1, session);
		} else {
			JsonHolder jsonHolder = (JsonHolder) value;
			StringType.INSTANCE.set(st, jsonHolder.getValueType(), index, session);
			TextType.INSTANCE.set(st, jsonSerializationService.toJson(jsonHolder.getValue()), index + 1, session);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<JsonHolder> returnedClass() {
		return JsonHolder.class;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		return Objects.equal(x, y);
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		return Objects.hashCode(x);
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Object value, SessionImplementor session) throws HibernateException {
		return (Serializable) value;
	}

	@Override
	public Object assemble(Serializable cached, SessionImplementor session, Object owner) throws HibernateException {
		return cached;
	}

	@Override
	public Object replace(Object original, Object target, SessionImplementor session, Object owner)
			throws HibernateException {
		return original;
	}

}
