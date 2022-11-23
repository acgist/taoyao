package com.acgist.taoyao.signal.media;

import java.util.List;

import com.acgist.taoyao.signal.media.router.MediaRouter;

public interface MediaRouterManager {

	void bindId();
	
	List<MediaRouter> from();
	
	List<MediaRouter> to();
	
	void fromRouteTo(String from, String to);
	
	void fromOrTo(String sn);
	
}
