package com.qinchy.operatorgatewayservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
public class EchoController {

    @PostMapping("/echo")
    public Map<String,Object> echo(@RequestBody Map<String,Object> request){
        log.info("echo接口接收到请求数据：{}", request);
        return request;
    }
}
