package com.acgist.taoyao.signal.media;

import java.util.List;

import com.acgist.taoyao.signal.media.router.Router;

public interface RouterManager {

	void bindId();
	
	List<Router> from();
	
	List<Router> to();
	
	void fromRouteTo(String from, String to);
	
	void fromOrTo(String sn);
	
}
