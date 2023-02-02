package com.acgist.taoyao.boot.model;

import java.util.function.Supplier;

/**
 * 可修改的Optional
 * 
 * @author acgist
 */
public final class ModifyOptional<T> {

	/**
	 * 值
	 */
	private T t;
	/**
	 * 生产者
	 */
	private Supplier<T> supplier;
	
	private ModifyOptional(T t, Supplier<T> supplier) {
		this.t = t;
		this.supplier = supplier;
	}

	public static final <T> ModifyOptional<T> of(T t) {
		return new ModifyOptional<>(t, null);
	}
	
	public static final <T> ModifyOptional<T> of(Supplier<T> supplier) {
		return new ModifyOptional<>(null, supplier);
	}
	
	/**
	 * @return 值
	 */
	public T get() {
		return this.t;
	}
	
	/**
	 * @return 值
	 */
	public T build() {
		if(this.t != null) {
			return this.t;
		}
		this.t = this.supplier.get();
		return this.t;
	}
	
}
