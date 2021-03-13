package recipe.lang.store;

public class Attribute<T> {
    private String name;
    private Class<T> classType;

	public Attribute(String name, Class<T> classType) {
		this.name = name;
		this.classType = classType;
	}

	public String getName() {
		return name;
	}

	public Class<T> getAttributeType() {
		return classType;
	}

	public boolean check(Object o) {
		return classType.isInstance(o);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Attribute<?>) {
			Attribute<?> other = (Attribute<?>) obj;
			return this.name.equals(other.name) && this.classType.equals(other.classType);
		}
		return false;
	}

	@Override
	public String toString() {
		return name + "<" + classType.getCanonicalName() + ">";
	}

	public T castTo(Object o) {
		return classType.cast(o);
	}

	public boolean isValidValue(Object value) {
		return classType.isInstance(value);
	}
}
