package com.acgist.taoyao.boot.model;

import java.io.Serializable;

/**
 * 消息头部
 *
 * @author acgist
 */
public class Header implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息版本
     */
    private String v;
    /**
     * 消息标识
     */
    private Long id;
    /**
     * 信令标识
     */
    private String signal;

    @Override
    public Header clone() {
        return new Header(this.v, this.id, this.signal);
    }

    public Header() {
    }

    public Header(String v, Long id, String signal) {
        this.v = v;
        this.id = id;
        this.signal = signal;
    }

    public String getV() {
        return v;
    }

    public void setV(String v) {
        this.v = v;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSignal() {
        return signal;
    }

    public void setSignal(String signal) {
        this.signal = signal;
    }
}
