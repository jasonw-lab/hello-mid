package com.hello.disruptor;

import com.lmax.disruptor.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * <h1>disruptor 异常处理器</h1>
 * */
@Slf4j
public class EventMessageExceptionHandler
  implements ExceptionHandler<EventMessage> {

  @Override
  public void handleEventException(
    Throwable ex,
    long sequence,
    EventMessage event
  ) {
    log.error("....");
  }

  @Override
  public void handleOnStartException(Throwable ex) {
    log.error("....");
  }

  @Override
  public void handleOnShutdownException(Throwable ex) {
    log.error("....");
  }
}
