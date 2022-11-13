package com.acgist.taoyao.boot.service;

/**
 * ID生成器
 * 
 * @author acgist
 */
public interface IdService {

	/**
	 * 生成十九位的ID：YYMMddHHmmss(12) + sn(1) + xxxxxx(6)
	 * 
	 * @return ID
	 */
	long buildId();
	
	/**
	 * @see #buildId()
	 * 
	 * @return ID
	 */
	String buildIdToString();

}
