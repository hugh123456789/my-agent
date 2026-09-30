package com.agent.codeagent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@SpringBootTest
class CodeAgentApplicationTests {

    @Test
    void contextLoads() {
        List<String> strings = Arrays.asList("abc", "def", "gkh", "abc");
        //返回符合条件的stream
        Stream<String> stringStream = strings.stream().filter(s -> "abc".equals(s));
        //计算流符合条件的流的数量
        long count = stringStream.count();
        System.out.println(count);

        //forEach遍历->打印元素
        strings.stream().forEach(System.out::println);
    }

}
