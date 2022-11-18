package com.acgist.taoyao.meeting;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * 会议管理
 * 
 * @author acgist
 */
@Slf4j
@Service
public class MeetingManager {

	/**
	 * 会议列表
	 */
	private List<Meeting> meetings = new CopyOnWriteArrayList<>();
	
	/**
	 * @return 所有会议列表
	 */
	public List<Meeting> meetings() {
		return this.meetings;
	}
	
	/**
	 * @param id 会议标识
	 * 
	 * @return 会议信息
	 */
	public Meeting meeting(String id) {
		return this.meetings.stream()
			.filter(v -> v.getId().equals(id))
			.findFirst()
			.orElse(null);
	}

	/**
	 * @param id 会议标识
	 * 
	 * @return 会议所有终端标识
	 */
	public List<String> sns(String id) {
		final Meeting meeting = this.meeting(id);
		return meeting == null ? List.of() : meeting.getSns();
	}
	
}
