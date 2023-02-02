package com.acgist.taoyao.boot.model;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.acgist.taoyao.boot.utils.JSONUtils;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(title = "模型", description = "模型")
@EqualsAndHashCode(callSuper = false, of = "id")
public abstract class Model implements Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(title = "标识", description = "标识")
	private Long id;
	/**
	 * 创建时间
	 */
	@Schema(title = "创建时间", description = "创建时间")
	private LocalDateTime createDate;
	/**
	 * 修改时间
	 */
	@Schema(title = "修改时间", description = "修改时间")
	private LocalDateTime modifyDate;

	@Override
	public String toString() {
		return JSONUtils.toJSON(this);
	}

}
