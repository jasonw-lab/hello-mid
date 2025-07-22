package com.hello.disruptor.impl;

import com.hello.disruptor.EventMessageDisruptor;
import com.hello.disruptor.EventMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventHandlerEventMessageDisruptor extends EventMessageDisruptor {

  @Override
  protected void handleEvent() {
    disruptor.handleEventsWith(
      new EventMessageHandler("event-handler-01"),
      new EventMessageHandler("event-handler-02")
    );
  }
}
