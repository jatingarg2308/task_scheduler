package com.example.scheduler.Model;

import lombok.Data;
import lombok.NonNull;

import javax.persistence.*;

@Entity
@Data
@Table(name="master_task")
public class MasterTask {

    private @Id String id;
    private @NonNull String url;
    private int taskRemaining;
    private String params;
    private String status;

    public MasterTask(){
        this.id = String.format("%s_%s", System.currentTimeMillis(), (int) (Math.random() * 10000));
        this.status = "in_progress";
    }
}
