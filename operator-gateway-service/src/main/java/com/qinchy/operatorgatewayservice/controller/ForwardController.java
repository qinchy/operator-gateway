package com.qinchy.operatorgatewayservice.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.qinchy.operatorgatewayservice.bean.Response;
import com.qinchy.operatorgatewayservice.config.OperatorConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

@SuppressWarnings("ALL")
@RestController
@Slf4j
public class ForwardController {

    @Autowired
    private OperatorConfig operatorConfig;

    @Autowired
    private OkHttpClient client;

    @Value("${forward-json-template}")
    private String forwardJsonTemplate;

    @Value("${handler-expression}")
    private String handlerExpress;

    @PostMapping("/forward")
    public Response<Map<String, Object>> forward(@RequestBody Map<String, Object> request) throws Exception {

        log.info("转发JSON模板配置：{}", forwardJsonTemplate);
        log.info("需要执行的动作模板配置：{}", handlerExpress);

        log.info("接收到内部请求报文：{}", request);
        String receiver = (String) request.get("receiver");
        if (StringUtils.isEmpty(receiver)) {
            throw new RuntimeException("报文中的接收方为空");
        }
        String host = operatorConfig.getOperatorConfig().get(receiver).get("host");
        log.info("获取到 {} 的主机地址为：{}", receiver, host);

        // 将转发的JSON模板转换成文档上下文对象，后面对此对象进行操作。
        DocumentContext documentContext = JsonPath.parse(forwardJsonTemplate);
        log.info("转发的JSON文档为：{}", documentContext.jsonString());
        EvaluationContext evaluationContext = getEvaluationContext(request);
        Map<String, Object> result = buildSendMessage(documentContext, evaluationContext);

        // 构造返回对象
        Response.ResponseBuilder<Map<String, Object>> builder = Response.builder();

        builder.success(true);
        builder.desc("success");
        builder.code("0");

        builder.data(result);

        return builder.build();
    }

    /**
     * 构造变量值上下文
     *
     * @param request 请求的map
     * @return 返回变量上下文
     */
    private EvaluationContext getEvaluationContext(Map<String, Object> request) {
        EvaluationContext evaluationContext = new StandardEvaluationContext();

        String clientId = operatorConfig.getOperatorConfig().get((String) request.get("receiver")).get("clientid");
        evaluationContext.setVariable("clientId", clientId);

        String senderDateTime = DateFormatUtils.format(new Date(), "yyyy-MM-dd'T'HH:mm:ss");
        evaluationContext.setVariable("senderDateTime", senderDateTime);

        String messageSn = (String) request.get("msgSn");
        evaluationContext.setVariable("messageSn", messageSn);
        evaluationContext.setVariable("signatureSn", "signatureSn");
        evaluationContext.setVariable("encryptionSn", "encryptionSn");
        evaluationContext.setVariable("digitalEnvelope", "digitalEnvelope");
        evaluationContext.setVariable("iv", "iv");
        evaluationContext.setVariable("request", request);
        return evaluationContext;
    }

    /**
     * 构造发送给运营商的请求
     *
     * @param documentContext   模板上下文
     * @param evaluationContext 变量上下文
     * @return 发送给运营商的请求内容
     * @throws Exception 抛出异常
     */
    private Map buildSendMessage(DocumentContext documentContext, EvaluationContext evaluationContext) throws Exception {
        JSONArray jsonArray = JSON.parseArray(handlerExpress);
        if (jsonArray.size() == 0) {
            throw new Exception("配置为空");
        }

        //创建ExpressionParser解析表达式
        ExpressionParser parser = new SpelExpressionParser();
        Expression exp;
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            JsonPath jsonPath = JsonPath.compile(jsonObject.getString("jsonpath"));
            String action = jsonObject.getString("action");
            String valueOrExpress = jsonObject.getString("value");

            // 默认是字面值
            Object value = valueOrExpress;
            try {
                exp = parser.parseExpression(valueOrExpress);
                Class<?> valueType = exp.getValueType(evaluationContext);
                value = exp.getValue(evaluationContext, valueType);
            } catch (ParseException e) {
                log.error("解析SpEL表达式{}异常，值可能是字面文本值", valueOrExpress, e);
            } catch (SpelEvaluationException e) {
                log.error("取值SpEL表达式{}异常，值可能是字面文本值", valueOrExpress, e);
            }

            // 添加或者修改指定路径的值(父路径不存在时不添加)
            if ("modify".equalsIgnoreCase(action)) {
                documentContext.set(jsonPath, value);
            }
        }

        String result1 = doSyncPost("http://localhost:8080/echo", documentContext.jsonString());
        log.info("OkHttp同步请求结果：{}", result1);

        String result2 = doAsyncPost("http://localhost:8080/echo", documentContext.jsonString());
        log.info("OkHttp异步请求结果：{}", result2);

        return JSON.parseObject(documentContext.jsonString(), Map.class);
    }

    /**
     * 同步请求
     *
     * @param url  请求地址
     * @param json 请求json字符串
     * @return 响应结果
     * @throws IOException IO异常
     */
    String doSyncPost(String url, String json) throws IOException {
        okhttp3.RequestBody body = okhttp3.RequestBody.create(MediaType.parse("application/json"), json);
        Request request1 = new Request.Builder().url(url).post(body).build();

        try (okhttp3.Response response = client.newCall(request1).execute()) {
            return response.body().string();
        }
    }

    /**
     * 异步请求
     *
     * @param url  请求地址
     * @param json 请求json字符串
     * @return 响应结果
     */
    String doAsyncPost(String url, String json) {
        okhttp3.RequestBody body = okhttp3.RequestBody.create(MediaType.parse("application/json"), json);
        Request request2 = new Request.Builder().url(url).post(body).build();
        Call call = client.newCall(request2);

        final String[] result = {""};
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("请求失败：{}", e);
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                result[0] = response.body().string();
                log.info("响应结果：{}", result[0]);
            }
        });

        return result[0];
    }
}
