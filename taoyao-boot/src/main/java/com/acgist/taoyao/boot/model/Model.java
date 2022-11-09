package com.acgist.taoyao.boot.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.acgist.taoyao.boot.utils.JSONUtils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Model
 * 
 * @author acgist
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, of = "id")
public abstract class Model implements Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	private Long id;
	/**
	 * 创建时间
	 */
	private LocalDateTime createDate;
	/**
	 * 修改时间
	 */
	private LocalDateTime modifyDate;

	@Override
	public String toString() {
		return JSONUtils.toJSON(this);
	}

}
