package com.acgist.taoyao.signal.service.impl;

import org.apache.commons.lang3.StringUtils;

import com.acgist.taoyao.boot.config.SecurityProperties;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.protocol.Protocol;
import com.acgist.taoyao.signal.service.SecurityService;
import com.acgist.taoyao.signal.service.UsernamePasswordService;

public class SecurityServiceImpl implements SecurityService {

	private final SecurityProperties securityProperties;
	private final UsernamePasswordService usernamePasswordService;
	
	public SecurityServiceImpl(SecurityProperties securityProperties, UsernamePasswordService usernamePasswordService) {
        this.securityProperties = securityProperties;
        this.usernamePasswordService = usernamePasswordService;
    }

    @Override
	public boolean authenticate(String username, String password) {
	    if(Boolean.FALSE.equals(this.securityProperties.getEnabled())) {
	        return true;
	    }
	    if(this.usernamePasswordService == null) {
	        return
	            StringUtils.equals(this.securityProperties.getUsername(), username) &&
	            StringUtils.equals(this.securityProperties.getPassword(), password);
	    } else {
	        return this.usernamePasswordService.authenticate(username, password);
	    }
	}
	
	@Override
	public boolean authenticate(Client client, Message message, Protocol protocol) {
	    return client.authorized() && protocol.authenticate(message);
	}

}
