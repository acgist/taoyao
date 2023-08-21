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
@Description(
    body = """
    {
        "diskspace": [
            {
                "path" : "存储路径",
                "free" : 存储空闲,
                "total": 存储总量
            },
            ...
        ],
        "maxMemory"    : 最大能用内存,
        "freeMemory"   : 空闲内存,
        "totalMemory"  : 已用内存,
        "osArch"       : "系统架构",
        "osName"       : "系统名称",
        "osVersion"    : "系统版本",
        "javaVmName"   : "虚拟机名称",
        "javaVersion"  : "虚拟机版本",
        "cpuProcessors": CPU核心数量
    }
    """,
    flow = "终端=>信令服务"
)
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
//      FileSystems.getDefault().getFileStores();
        Stream.of(File.listRoots()).forEach(v -> {
            diskspace.add(new Diskspace(v.getPath(), v.getFreeSpace(), v.getTotalSpace()));
        });
        info.put("diskspace", diskspace);
        // 内存
        final Runtime runtime = Runtime.getRuntime();
        info.put("maxMemory",   runtime.maxMemory());
        info.put("freeMemory",  runtime.freeMemory());
        info.put("totalMemory", runtime.totalMemory());
        info.put("maxMemoryGracefully",   FileUtils.formatSize(runtime.maxMemory()));
        info.put("freeMemoryGracefully",  FileUtils.formatSize(runtime.freeMemory()));
        info.put("totalMemoryGracefully", FileUtils.formatSize(runtime.totalMemory()));
        // 其他
        info.put("osArch", System.getProperty("os.arch"));
        info.put("osName", System.getProperty("os.name"));
        info.put("osVersion",     System.getProperty("os.version"));
        info.put("javaVmName",    System.getProperty("java.vm.name"));
        info.put("javaVersion",   System.getProperty("java.version"));
        info.put("cpuProcessors", runtime.availableProcessors());
        // 响应
        message.setBody(info);
        client.push(message);
    };

    @Getter
    @Setter
    public static final class Diskspace {

        /**
         * 路径
         */
        private final String path;
        /**
         * 空闲
         */
        private final Long free;
        /**
         * 总量
         */
        private final Long total;
        /**
         * 空闲
         */
        private final String freeGracefully;
        /**
         * 总量
         */
        private final String totalGracefully;
        
        public Diskspace(String path, Long free, Long total) {
            this.path  = path;
            this.free  = free;
            this.total = total;
            this.freeGracefully  = FileUtils.formatSize(free);
            this.totalGracefully = FileUtils.formatSize(total);
        }
        
    }
    
}
