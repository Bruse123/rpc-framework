package com.lhb.rpc.transport.command.response;

import com.lhb.rpc.enums.RpcResponseCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 响应信息
 *
 * @author BruseLin
 */
@Getter
@Setter
@ToString
public class RpcResponse<T> {

    private String requestId;

    private Integer responseCode;

    private String responseMsg;

    private T data;

    public static <T> RpcResponse<T> success(String requestId, T data) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setRequestId(requestId);
        response.setResponseCode(RpcResponseCode.SUCCESS.getCode());
        response.setResponseMsg(RpcResponseCode.SUCCESS.getMessage());
        if (data != null) {
            response.setData(data);
        }
        return response;
    }

    public static <T> RpcResponse<T> fail(String requestId, T data, RpcResponseCode rpcResponseCode) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setRequestId(requestId);
        response.setResponseCode(rpcResponseCode.getCode());
        response.setResponseMsg(rpcResponseCode.getMessage());
        if (data != null) {
            response.setData(data);
        }
        return response;
    }


}
