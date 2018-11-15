package us.kbase.groups.core.catalog;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.groups.util.Util.checkString;

import us.kbase.groups.core.exceptions.IllegalParameterException;
import us.kbase.groups.core.exceptions.MissingParameterException;

/** A method in the KBase catalog, often represented as a string like 'module.method', where
 * module is the module containing the method.
 * @author gaprice@lbl.gov
 *
 */
public class CatalogMethod implements Comparable<CatalogMethod> {

	private final CatalogModule module;
	private final String method;
	
	/** Create a method from a catalog method string, e.g. 'module.method'.
	 * @param moduleMethod the method string.
	 * @throws MissingParameterException if the module or method strings are null or
	 * whitespace only.
	 * @throws IllegalParameterException if the input string contains control characters or
	 * is not a valid method string.
	 */
	public CatalogMethod(final String moduleMethod)
			throws MissingParameterException, IllegalParameterException {
		checkString(moduleMethod, "module.method");
		final String[] split = moduleMethod.split("\\.");
		if (split.length != 2) {
			throw new IllegalParameterException("Illegal catalog method name: " +
					moduleMethod.trim());
		}
		this.module = new CatalogModule(split[0].trim());
		checkString(split[1], "catalog method");
		this.method = split[1].trim();
	}
	
	/** Create a method from a separate module and method.
	 * @param module the module.
	 * @param method the method.
	 * @throws MissingParameterException if the method string is null or white space only.
	 */
	public CatalogMethod(final CatalogModule module, final String method)
			throws MissingParameterException {
		checkNotNull(module, "module");
		checkString(method, "catalog method");
		this.module = module;
		this.method = method.trim();
	}

	/** Get the module.
	 * @return the module.
	 */
	public CatalogModule getModule() {
		return module;
	}

	/** Get the method. 
	 * @return the method.
	 */
	public String getMethod() {
		return method;
	}

	@Override
	public int compareTo(final CatalogMethod o) {
		final int c1 = module.compareTo(o.module);
		if (c1 != 0) {
			return c1;
		}
		return method.compareTo(o.method);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result + ((module == null) ? 0 : module.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		CatalogMethod other = (CatalogMethod) obj;
		if (method == null) {
			if (other.method != null) {
				return false;
			}
		} else if (!method.equals(other.method)) {
			return false;
		}
		if (module == null) {
			if (other.module != null) {
				return false;
			}
		} else if (!module.equals(other.module)) {
			return false;
		}
		return true;
	}
}
