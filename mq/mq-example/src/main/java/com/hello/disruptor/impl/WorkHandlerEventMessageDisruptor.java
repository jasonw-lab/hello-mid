package com.hello.disruptor.impl;

import com.hello.disruptor.EventMessageDisruptor;
import com.hello.disruptor.EventMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkHandlerEventMessageDisruptor extends EventMessageDisruptor {

  @Override
  protected void handleEvent() {
    disruptor.handleEventsWithWorkerPool(
      new EventMessageHandler("work-handler-01"),
      new EventMessageHandler("work-handler-02")
    );
  }
}
