package com.hello;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
/** */
@SpringBootTest
public class JavaTest {

  private String getHoge() {
    return "123";
  }

  @Test
  public void test1() throws Exception {
    String password = "123";

    // Optionalオブジェクトを生成するofメソッドは、引数がnullだとNullPointerExceptionを投げる
    Optional<String> hogeOpt = Optional.of(getHoge());

    if (hogeOpt.isPresent()) { // 事前にわざわざ値の存在をチェックしている
      // 値を取得するgetメソッドは、値が存在していない場合実行時例外を投げる(NoSuchElementException)
      String hoge = hogeOpt.get();

      System.out.println(hoge);
    }

    return;
  }

  //  @Test
  public void test2() throws Exception {
    String str = "nonNull";
    Optional<String> nonNullString = Optional.of(str);

    System.out.println(nonNullString.isPresent());
    System.out.println(nonNullString.get());
    //    System.out.println(Optional.of(null));//ava.lang.NullPointerException

    str = null;
    Optional<String> nullableString = Optional.ofNullable(str);

    System.out.println(nullableString.isPresent()); // false
    System.out.println(nullableString.orElse("other")); // other

    str = null;
    nullableString = Optional.ofNullable(str);
    log.info(String.valueOf(nullableString.isPresent())); // false
    log.info(nullableString.orElse("other"));
    //    System.out.println(nullableString.get()); //other
    log.info(nullableString.orElse(null));

    return;
  }
}
