package com.acgist.taoyao.signal.service.impl;

import com.acgist.taoyao.boot.model.Header;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.signal.client.ClientSession;
import com.acgist.taoyao.signal.protocol.Protocol;
import com.acgist.taoyao.signal.service.SecurityService;

public class SecurityServiceImpl implements SecurityService {

	@Override
	public boolean authenticate(Message message, ClientSession session, Protocol protocol) {
		if(!session.authorized()) {
			return false;
		}
		final Header header = message.getHeader();
		final String sn = header.getSn();
		if(!sn.equals(session.sn())) {
			return false;
		}
		if(!protocol.authenticate(message)) {
			return false;
		}
		// 更多
		return true;
	}

}
