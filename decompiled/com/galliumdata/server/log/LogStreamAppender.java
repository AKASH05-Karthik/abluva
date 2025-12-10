/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 *  org.apache.logging.log4j.core.Filter
 *  org.apache.logging.log4j.core.Layout
 *  org.apache.logging.log4j.core.LogEvent
 *  org.apache.logging.log4j.core.appender.AbstractAppender
 *  org.apache.logging.log4j.core.config.Property
 *  org.apache.logging.log4j.core.config.plugins.Plugin
 *  org.apache.logging.log4j.core.config.plugins.PluginAttribute
 *  org.apache.logging.log4j.core.config.plugins.PluginElement
 *  org.apache.logging.log4j.core.config.plugins.PluginFactory
 */
package com.galliumdata.server.log;

import java.io.Serializable;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name="LogStreamAppender", category="Core", elementType="appender")
public class LogStreamAppender
extends AbstractAppender {
    protected Deque<LogEvent> events = new ConcurrentLinkedDeque<LogEvent>();
    protected static LogStreamAppender instance;
    private static final int EVENT_QUEUE_SIZE = 1000;
    private static int eventQueueSize;
    private static final Logger log;

    protected LogStreamAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
    }

    @PluginFactory
    public static LogStreamAppender createAppender(@PluginAttribute(value="name") String name, @PluginElement(value="Filter") Filter filter, @PluginElement(value="Layout") Layout layout) {
        if (instance != null) {
            throw new RuntimeException("Did not expect to create more than one instance of LogStreamAppender");
        }
        instance = new LogStreamAppender(name, filter, (Layout<? extends Serializable>)layout, false, null);
        return instance;
    }

    public void append(LogEvent logEvent) {
        while (this.events.size() >= eventQueueSize) {
            this.events.remove();
        }
        this.events.add(logEvent.toImmutable());
    }

    public static LogStreamAppender getInstance() {
        return instance;
    }

    public Deque<LogEvent> getEventQueue() {
        return this.events;
    }

    public int getQueueSize() {
        return eventQueueSize;
    }

    public void setQueueSize(int newSize) {
        if (newSize < 0 || newSize > 10000) {
            log.debug("We were asked to set the log size to a nonsensical number: {}, no action taken", (Object)newSize);
            return;
        }
        if (newSize < eventQueueSize) {
            while (this.events.size() > newSize) {
                this.events.remove();
            }
        }
        eventQueueSize = newSize;
    }

    static {
        eventQueueSize = 1000;
        log = LogManager.getLogger((String)"galliumdata.core");
    }
}
