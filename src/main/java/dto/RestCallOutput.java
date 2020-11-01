package dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RestCallOutput {

    private Object OutputData;
    private String ErrorMsg;
    private String DataMsg;
    private String InfoMsg;
    private int ResultCode;

    public RestCallOutput(Object obj, String err, String data,
                          String info, int code) {
        OutputData = obj;
        ErrorMsg = err;
        DataMsg = data;
        InfoMsg = info;
        ResultCode = code;
    }
}
