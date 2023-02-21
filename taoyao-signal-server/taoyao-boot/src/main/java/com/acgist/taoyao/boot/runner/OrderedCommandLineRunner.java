package com.acgist.taoyao.boot.runner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;

/**
 * Ordered + CommandLineRunner
 * 
 * @author acgist
 */
public interface OrderedCommandLineRunner extends Ordered, CommandLineRunner {

    @Override
    default int getOrder() {
        return 0;
    }
    
}
