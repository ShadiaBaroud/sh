package requitur.content;

import java.util.Arrays;

import requitur.ChangedEntity;
import requitur.TraceElement;

public class TraceElementContent extends Content {

   private final String clazz, method;

	private final boolean isStatic;

	private final String[] parameterTypes;

	private final int depth;

	// private String source;

	public TraceElementContent(final TraceElement element) {
		this.clazz = element.getClazz();
		this.method = element.getMethod();
		this.parameterTypes = element.getParameterTypes();
		this.depth = element.getDepth();
		this.isStatic = element.isStatic();
	}

	public TraceElementContent(
		final String clazz,
		final String method,
		final String[] parameterTypes,
		final int depth
	) {
		this.clazz = clazz;
		this.method = method;
		this.depth = depth;
		this.parameterTypes = parameterTypes;
		this.isStatic = false;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof TraceElementContent) {
			final TraceElementContent other = (TraceElementContent) obj;

			if (((TraceElementContent) obj).getMethod().equals("main")) {
				System.out.println(Arrays.toString(parameterTypes));
			}

			return other.getClazz().equals(this.getClazz()) &&
					other.getMethod().equals(this.getMethod()) &&
					Arrays.equals(other.getParameterTypes(), this.getParameterTypes());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return clazz.hashCode() + method.hashCode();
	}

	@Override
	public String toString() {
		final StringBuilder result = new StringBuilder();
		result.append(clazz);
		result.append("#");
		result.append(method);

		if (parameterTypes.length != 0) {
			result.append("(");
			result.append(Arrays.deepToString(parameterTypes));
			result.append(")");
		}

		return result.toString();
	}

	public String getClazz() {
		return clazz;
	}

	public String getMethod() {
		return method;
	}
	
	public String getPackagelessClazz() {
		return clazz.substring(clazz.lastIndexOf('.') + 1);
   }

	public String getSimpleClazz() {
		if (clazz.contains(ChangedEntity.CLAZZ_SEPARATOR)) {
			return clazz.substring(clazz.lastIndexOf(ChangedEntity.CLAZZ_SEPARATOR) + 1);
		}
		return clazz.substring(clazz.lastIndexOf('.') + 1);
	}

	public int getDepth() {
		return depth;
	}

	public String[] getParameterTypes() {
		return parameterTypes;
	}

   public boolean isInnerClassCall() {
      return clazz.contains(ChangedEntity.CLAZZ_SEPARATOR);
   }

   public String getOuterClass() {
      return clazz.substring(0, clazz.lastIndexOf(ChangedEntity.CLAZZ_SEPARATOR));
   }

   public String getPackage() {
	   return clazz.contains(".") ? clazz.substring(0, clazz.lastIndexOf('.')) : "";
   }

	// public String getSource() {
	// return source;
	// }
	//
	// public void setSource(final String source) {
	// this.source = source;
	// }
}