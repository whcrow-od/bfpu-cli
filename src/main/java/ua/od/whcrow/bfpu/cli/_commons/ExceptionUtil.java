package ua.od.whcrow.bfpu.cli._commons;

import jakarta.annotation.Nonnull;
import ua.od.whcrow.bfpu.cli._commons.functions.e.SupplierE;

public final class ExceptionUtil {
	
	private ExceptionUtil() {
	}
	
	@SuppressWarnings("unchecked")
	public static <E extends Throwable> RuntimeException sneakyThrow(@Nonnull Throwable t)
			throws E {
		throw (E) t;
	}
	
	public static <E extends Throwable, T> T sneakySupply(@Nonnull SupplierE<T,E> supplier) {
		T result;
		try {
			result = supplier.get();
		} catch (Throwable e) {
			throw sneakyThrow(e);
		}
		return result;
	}
	
}
