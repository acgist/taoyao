package com.acgist.taoyao.boot.service;

/**
 * ID生成
 * 
 * @author acgist
 */
public interface IdService {

	/**
	 * 生成十八位的ID：YYMMddHHmmss + sn + xxxx
	 * 
	 * @return ID
	 */
	Long id();

}
