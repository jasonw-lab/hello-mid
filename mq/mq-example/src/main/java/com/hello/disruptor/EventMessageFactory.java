package com.hello.disruptor;

import com.lmax.disruptor.EventFactory;

/**
 * <h1>事件模型工程类, 用于生产事件消息</h1>
 * */
public class EventMessageFactory implements EventFactory<EventMessage> {

  @Override
  public EventMessage newInstance() {
    return new EventMessage();
  }
}
