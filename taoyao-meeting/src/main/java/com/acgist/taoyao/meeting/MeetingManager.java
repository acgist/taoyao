package com.acgist.taoyao.meeting;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.acgist.taoyao.boot.service.IdService;

import lombok.extern.slf4j.Slf4j;

/**
 * 会议管理
 * 
 * @author acgist
 */
@Slf4j
@Service
public class MeetingManager {

	@Autowired
	private IdService idService;

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

	/**
	 * 创建会议
	 * 
	 * @param sn 创建会议终端标识
	 * 
	 * @return 会议信息
	 */
	public Meeting create(String sn) {
		final Meeting meeting = new Meeting();
		meeting.setId(this.idService.buildIdToString());
		meeting.setSns(new CopyOnWriteArrayList<>());
		meeting.setCreator(sn);
		meeting.addSn(sn);
		this.meetings.add(meeting);
		log.info("创建会议：{}", meeting.getId());
		return meeting;
	}
	
}
