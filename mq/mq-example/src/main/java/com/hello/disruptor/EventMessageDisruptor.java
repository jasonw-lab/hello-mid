package com.hello.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * <h1>event message disruptor 工具</h1>
 * */
@Slf4j
public abstract class EventMessageDisruptor
  implements InitializingBean, DisposableBean {

  /** 事件转换器, 用于设置消息内容 */
  private static final EventTranslatorOneArg<EventMessage, Object> TRANSLATOR =
    (message, sequence, obj) -> message.setObj(obj);
  protected Disruptor<EventMessage> disruptor;
  private RingBuffer<EventMessage> ringBuffer;
  private static final EventMessageFactory factory = new EventMessageFactory();

  /**
   * <h2>生产者, 发布消息</h2>
   * */
  public void onData(Object obj) {
    ringBuffer.publishEvent(TRANSLATOR, obj);
  }

  protected abstract void handleEvent();

  /**
   * <h2>初始化方法</h2>
   * */
  @Override
  public void afterPropertiesSet() throws Exception {
    final int bufferSize = 1024 * 1024;
    // 实例化 disruptor
    disruptor =
    new Disruptor<>(
      factory,
      bufferSize,
      DaemonThreadFactory.INSTANCE,
      ProducerType.SINGLE,
      new BlockingWaitStrategy()
    );
    // 设置事件处理器
    handleEvent();
    // 异常处理器
    disruptor.setDefaultExceptionHandler(new EventMessageExceptionHandler());
    // 启动 disruptor 实现生产和消费
    disruptor.start();
    // 初始化 ringBuffer
    ringBuffer = disruptor.getRingBuffer();
  }

  /**
   * <h2>bean 销毁之前需要执行的销毁方法</h2>
   * */
  @Override
  public void destroy() throws Exception {
    try {
      disruptor.shutdown(1, TimeUnit.MINUTES);
    } catch (TimeoutException ex) {
      log.error(".....");
    }
  }
}
