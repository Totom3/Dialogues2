package io.github.totom3.commons.binary;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Totom3
 */
public class BinaryDataBank {

    private final Map<String, Object> data;

    public BinaryDataBank() {
	this.data = new HashMap<>(10);
    }

    public Map<String, Object> allData() {
	return data;
    }

    public Object getData(String key) {
	return data.get(key);
    }

    public <T> T getData(String key, Class<T> clazz) {
	Object val = getData(key);
	if (val == null) {
	    throw new NullPointerException("Missing data '" + key + "' with class " + clazz.getName());
	}
	return clazz.cast(val);
    }

    public Object removeData(String key) {
	return data.remove(key);
    }

    public void removeAll() {
	data.clear();
    }

    public Object setData(String key, Object val) {
	return data.put(key, val);
    }

}
