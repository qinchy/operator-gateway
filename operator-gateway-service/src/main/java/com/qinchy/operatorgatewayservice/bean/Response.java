package com.qinchy.operatorgatewayservice.bean;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Response<T> {
    private Boolean success;

    private String code;

    private String desc;

    private T data;

}
