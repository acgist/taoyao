package com.acgist.taoyao.signal.protocol.system;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.acgist.taoyao.boot.annotation.Description;
import com.acgist.taoyao.boot.annotation.Protocol;
import com.acgist.taoyao.boot.model.Message;
import com.acgist.taoyao.boot.utils.FileUtils;
import com.acgist.taoyao.signal.client.Client;
import com.acgist.taoyao.signal.client.ClientType;
import com.acgist.taoyao.signal.protocol.ProtocolClientAdapter;

import lombok.Getter;
import lombok.Setter;

/**
 * 系统信息信令
 * 
 * @author acgist
 */
@Protocol
@Description
public class SystemInfoProtocol extends ProtocolClientAdapter {

    public static final String SIGNAL = "system::info";
    
    public SystemInfoProtocol() {
        super("系统信息信令", SIGNAL);
    }

    @Override
    public void execute(String clientId, ClientType clientType, Client client, Message message, Map<String, Object> body) {
        final Map<String, Object> info = new HashMap<>();
        // 硬盘
        final List<Diskspace> diskspace = new ArrayList<>();
//      File.listRoots(); -> 不全
//      FileSystems.getDefault().getFileStores(); -> 重复
        Stream.of(File.listRoots()).forEach(v -> {
            diskspace.add(new Diskspace(v.getPath(), v.getTotalSpace(), v.getFreeSpace()));
        });
        info.put("diskspace", diskspace);
        // 内存
        final Runtime runtime = Runtime.getRuntime();
        info.put("maxMemory", runtime.maxMemory());
        info.put("freeMemory", runtime.freeMemory());
        info.put("totalMemory", runtime.totalMemory());
        info.put("maxMemoryGracefully", FileUtils.formatSize(runtime.maxMemory()));
        info.put("freeMemoryGracefully", FileUtils.formatSize(runtime.freeMemory()));
        info.put("totalMemoryGracefully", FileUtils.formatSize(runtime.totalMemory()));
        // 其他
        info.put("osName", System.getProperty("os.name"));
        info.put("osArch", System.getProperty("os.arch"));
        info.put("osVersion", System.getProperty("os.version"));
        info.put("javaVmName", System.getProperty("java.vm.name"));
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("cpuProcessors", runtime.availableProcessors());
        // 响应
        client.push(this.build(info));
    };

    @Getter
    @Setter
    public static final class Diskspace {

        /**
         * 路径
         */
        private final String path;
        /**
         * 总量
         */
        private final Long total;
        /**
         * 空闲
         */
        private final Long free;
        /**
         * 总量
         */
        private final String totalGracefully;
        /**
         * 空闲
         */
        private final String freeGracefully;
        
        public Diskspace(String path, Long total, Long free) {
            this.path = path;
            this.total = total;
            this.free = free;
            this.totalGracefully = FileUtils.formatSize(total);
            this.freeGracefully = FileUtils.formatSize(free);
        }
        
    }
    
}
