package com.dvorkin.json;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by dvorkin on 15.10.2017.
 */
public class ExtensionCommand {

    @JsonProperty("command")
    private String command;

    @JsonProperty("options")
    private String[] options;

    public String getCommand() {
        return command;
    }

    public String[] getOptions() {
        return options;
    }

}
