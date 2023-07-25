package com.acgist.taoyao.boot.model;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = ComponentModel.SPRING)
public interface MessageMapstruct {

    /**
     * 对象拷贝
     * 
     * @param source 原始对象
     * 
     * @return 目标对象
     */
    Header copy(Header source);
    
    /**
     * 对象拷贝
     * 
     * @param source 原始对象
     * @param target 目标对象
     */
    void copy(Header source, @MappingTarget Header target);
    
    /**
     * 对象拷贝
     * 
     * @param source 原始对象
     * 
     * @return 目标对象
     */
    Message copy(Message source);
 
    /**
     * 对象拷贝
     * 
     * @param source 原始对象
     * @param target 目标对象
     */
    void copy(Message source, @MappingTarget Message target);
    
}
