package recipe.lang.utils;

import java.util.Arrays;

public class Tuple<T> {

	private T[] values;
	
	public Tuple( T ... values ) {
		this.values = values;
	}
	
	public int size() {
		return values.length;
	}
	
	public T get( int i ) {
		return values[i];
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(values);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Tuple) {
			return Arrays.equals(this.values, ((Tuple) obj).values);
		}
		return false;
	}

	@Override
	public String toString() {
		return Arrays.toString(values);
	}
	
	
}