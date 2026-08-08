package com.acgist.taoyao.signal.client.gb;

import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientAdapter;
import com.acgist.taoyao.signal.client.gb.GbServer.GbDeviceClient;

public class GbClient extends ClientAdapter<GbDeviceClient> {

    public GbClient(long timeout, GbDeviceClient instance) {
        super(timeout, instance);
    }

    @Override
    public void push(Message message) {
        this.instance.push(message);
    }

    @Override
    protected String getClientIP(GbDeviceClient instance) {
        return instance.getHost();
    }

}
