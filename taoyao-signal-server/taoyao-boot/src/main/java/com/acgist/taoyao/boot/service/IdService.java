package com.acgist.taoyao.boot.service;

/**
 * 生成ID服务
 * 
 * @author acgist
 */
public interface IdService {

	/**
	 * 生成十六位的消息ID：ddHHmmss(8) + index(2) + xxxxxx(6)
	 * 只能用于生成消息ID：JS超过`Number.MAX_SAFE_INTEGER`丢失精度
	 * 
	 * @return ID
	 */
	long buildId();
	
	/**
	 * @return 终端索引
	 */
	long buildClientIndex();
	
	/**
	 * 生成三十二位唯一ID
	 * 
	 * @return ID
	 */
	String buildUuid();
	
}
