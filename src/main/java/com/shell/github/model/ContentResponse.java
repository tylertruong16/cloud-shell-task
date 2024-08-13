package com.shell.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ContentResponse {
    private String name;
    private String size;
    @JsonProperty("download_url")
    private String downloadUrl;
    private String sha;
    private String path;
    private String url = "";
}
