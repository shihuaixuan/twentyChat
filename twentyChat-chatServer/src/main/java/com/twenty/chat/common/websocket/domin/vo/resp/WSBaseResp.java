package com.twenty.chat.common.websocket.domin.vo.resp;

import lombok.Data;

@Data
public class WSBaseResp<T> {

    /**
     * @see com.twenty.chat.common.websocket.domin.enums.WSRespTypeEnum
     */
    private Integer type;

    private T data;

}
