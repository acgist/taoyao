package com.acgist.taoyao.signal.client.gb;

import java.util.TooManyListenersException;

import javax.sip.InvalidArgumentException;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.TransportNotSupportedException;

/**
 * 国标服务接口
 */
public interface IGbServer {

    /**
     * 加载服务配置
     * 
     * @throws ObjectInUseException
     * @throws PeerUnavailableException
     * @throws InvalidArgumentException
     * @throws TooManyListenersException
     * @throws TransportNotSupportedException
     */
    void init() throws ObjectInUseException, PeerUnavailableException, InvalidArgumentException, TooManyListenersException , TransportNotSupportedException;

    /**
     * 注册上级服务
     */
    void registerServer();

}
