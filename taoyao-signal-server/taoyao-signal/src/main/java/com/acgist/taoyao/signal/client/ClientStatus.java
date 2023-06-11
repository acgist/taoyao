package com.acgist.taoyao.signal.client;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.acgist.taoyao.boot.config.Constant;
import com.acgist.taoyao.boot.utils.MapUtils;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 终端状态
 * 
 * @author acgist
 */
@Getter
@Setter
@Schema(title = "终端状态", description = "终端状态")
public class ClientStatus {

    @Schema(title = "终端IP", description = "终端IP")
    private String ip;
    @Schema(title = "终端名称", description = "终端名称")
    private String name;
    @Schema(title = "终端ID", description = "终端ID")
    private String clientId;
    @Schema(title = "终端类型", description = "终端类型")
    private ClientType clientType;
    @Schema(title = "纬度", description = "纬度")
    private Double latitude;
    @Schema(title = "经度", description = "经度")
    private Double longitude;
    @Schema(title = "湿度", description = "湿度")
    private Double humidity;
    @Schema(title = "温度", description = "温度")
    private Double temperature;
	@Schema(title = "信号强度（0~100）", description = "信号强度（0~100）")
	private Integer signal;
	@Schema(title = "电池电量（0~100）", description = "电池电量（0~100）")
	private Integer battery;
	@Schema(title = "是否发生告警", description = "是否发生告警")
	private Boolean alarming;
	@Schema(title = "是否正在充电", description = "是否正在充电")
	private Boolean charging;
	@Schema(title = "终端是否正在录像", description = "终端是否正在录像")
	private Boolean clientRecording;
	@Schema(title = "服务端是否正在录像", description = "服务端是否正在录像")
	private Boolean serverRecording;
	@Schema(title = "最后心跳时间", description = "最后心跳时间")
	private LocalDateTime lastHeartbeat;
	@Schema(title = "终端状态", description = "其他扩展终端状态")
	private Map<String, Object> status = new HashMap<>();
	@Schema(title = "终端配置", description = "其他扩展终端配置")
	private Map<String, Object> config = new HashMap<>();
	
	/**
	 * 拷贝属性
	 * 
	 * @param body 消息主体
	 */
	public void copy(Map<String, Object> body) {
        this.setLatitude(MapUtils.getDouble(body, Constant.LATITUDE));
        this.setLongitude(MapUtils.getDouble(body, Constant.LONGITUDE));
        this.setHumidity(MapUtils.getDouble(body, Constant.HUMIDITY));
        this.setTemperature(MapUtils.getDouble(body, Constant.TEMPERATURE));
        this.setSignal(MapUtils.getInteger(body, Constant.SIGNAL));
        this.setBattery(MapUtils.getInteger(body, Constant.BATTERY));
        this.setAlarming(MapUtils.getBoolean(body, Constant.ALARMING));
        this.setCharging(MapUtils.getBoolean(body, Constant.CHARGING));
        this.setClientRecording(MapUtils.getBoolean(body, Constant.CLIENT_RECORDING));
        this.status(MapUtils.get(body, Constant.STATUS));
        this.config(MapUtils.get(body, Constant.CONFIG));
        this.setLastHeartbeat(LocalDateTime.now());
	}
	
	/**
	 * 拷贝状态
	 * 
	 * @param map 状态
	 */
	public void status(Map<String, Object> map) {
	    if(map == null) {
	        return;
	    }
	    map.forEach(this.status::put);
	}
	
	/**
	 * 拷贝配置
	 * 
	 * @param map 配置
	 */
	public void config(Map<String, Object> map) {
	    if(map == null) {
	        return;
	    }
	    map.forEach(this.config::put);
	}
	
}
