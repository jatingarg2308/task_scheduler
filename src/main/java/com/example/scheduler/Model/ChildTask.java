package com.example.scheduler.Model;

import lombok.Data;
import lombok.NonNull;

@Data
public class ChildTask {

    private String parentId;
    private int trial;
    private @NonNull String url;
    private String params;

    public ChildTask(){}

    public ChildTask(String url){
        this.url = url;
    }
}