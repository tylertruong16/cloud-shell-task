package com.shell.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@AllArgsConstructor
public class ShellStatusResponse {
    private String email;
    private Status status;

    public boolean cannotConnectToGoogle() {
        return StringUtils.equalsIgnoreCase(status.name(), Status.CANNOT_CONNECT_GOOGLE.name());
    }


    public enum Status {
        CANNOT_CONNECT_GOOGLE, SUCCESSFULLY, NEW, FAILED
    }

}
