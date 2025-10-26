package ua.od.whcrow.bfpu.cli._commons.functions.e;

@FunctionalInterface
public interface SupplierE<T, E extends Throwable> {
	
	T get()
			throws E;
	
}
