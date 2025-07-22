package com.hello.disruptor;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * <h1>事件消息, 事件数据模型</h1>
 * */
@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class EventMessage implements Serializable {

  private Object obj;
}
