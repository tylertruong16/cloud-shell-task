package com.shell.github.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GitHubConfig {
    private String apiUrl = "";
    private String apiKey = "";

}
