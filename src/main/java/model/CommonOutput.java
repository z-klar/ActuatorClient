package model;

import lombok.Data;

import java.util.ArrayList;

@Data
public class CommonOutput {

    private Object result;
    private ArrayList<String> infoMsg;
    private ArrayList<String> errorMsg;

    public CommonOutput() {
        infoMsg = new ArrayList<>();
        errorMsg = new ArrayList<>();
    }
}
