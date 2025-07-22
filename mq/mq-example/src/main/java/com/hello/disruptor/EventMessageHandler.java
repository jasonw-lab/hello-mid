package com.hello.disruptor;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * <h1>事件处理器, 即消费者</h1>
 * */
@Slf4j
public class EventMessageHandler
  implements EventHandler<EventMessage>, WorkHandler<EventMessage> {

  private final String handlerName;

  public EventMessageHandler(String handlerName) {
    this.handlerName = handlerName;
  }

  /**
   * <h2>EventHandler 独立消费者, 每一个消费者都消费所有的消息</h2>
   * */
  @Override
  public void onEvent(EventMessage event, long sequence, boolean endOfBatch)
    throws Exception {
    log.info("....");
  }

  /**
   * <h2>WorkHandler 共同消费者, 不会重复消费消息信息</h2>
   * */
  @Override
  public void onEvent(EventMessage event) throws Exception {
    log.info("....");
  }
}
