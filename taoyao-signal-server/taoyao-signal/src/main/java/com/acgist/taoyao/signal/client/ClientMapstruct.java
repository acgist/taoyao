package com.acgist.taoyao.signal.client;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = ComponentModel.SPRING)
public interface ClientMapstruct {

    /**
     * 对象拷贝
     * 
     * @param source 原始对象
     * 
     * @return 目标对象
     */
    ClientStatus copy(ClientStatus source);
 
    /**
     * 对象拷贝
     * 
     * @param source 原始对象
     * @param target 目标对象
     */
    void copy(ClientStatus source, @MappingTarget ClientStatus target);
    
}
