package com.acgist.taoyao.signal.party.room;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.MappingTarget;

/**
 * 房间状态映射
 * 
 * @author acgist
 */
@Mapper(componentModel = ComponentModel.SPRING)
public interface RoomStatusMapstruct {

    /**
     * 对象拷贝
     * 
     * @param source 原始对象
     * 
     * @return 目标对象
     */
    RoomStatus copy(RoomStatus source);
 
    /**
     * 对象拷贝
     * 
     * @param source 原始对象
     * @param target 目标对象
     */
    void copy(RoomStatus source, @MappingTarget RoomStatus target);
    
}
