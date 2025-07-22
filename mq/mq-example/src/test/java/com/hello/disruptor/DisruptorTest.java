package com.hello.disruptor;

import com.hello.disruptor.impl.EventHandlerEventMessageDisruptor;
import com.hello.disruptor.impl.WorkHandlerEventMessageDisruptor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@SpringBootTest(classes = com.hello.Application.class)
@ExtendWith(SpringExtension.class)
public class DisruptorTest {

  @Autowired
  //EventHandlerモデルは、1つのイベントを全ての消費者にブロードキャストします。

  private EventHandlerEventMessageDisruptor disruptor01;

  @Autowired
  //
  //WorkHandlerモデルは、1つのイベントをいずれか1つの消費者に割り当て、作業を分担させます。
  private WorkHandlerEventMessageDisruptor disruptor02;

  @Test
  public void testDisruptorPushData() {
    disruptor01.onData("01");
    disruptor02.onData("01");
  }
}
