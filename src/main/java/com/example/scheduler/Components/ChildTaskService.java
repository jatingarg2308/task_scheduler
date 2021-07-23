package com.example.scheduler.Components;

import com.example.scheduler.Model.ChildTask;

import com.example.scheduler.Repository.MasterTaskRepository;
import com.squareup.okhttp.*;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
@Transactional
public class ChildTaskService {

    @Value("${scheduler.child_topic}")
    private String childTaskTopic;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Value("${scheduler.max_retry}")
    private int maxRetry;

    @Autowired
    private KafkaTemplate<String, ChildTask> childTaskKafkaTemplate;

    @Autowired
    private MasterTaskRepository masterTaskRepository;


    @KafkaListener(topics = "${scheduler.child_topic}", groupId = "${scheduler.group_batch}", containerFactory = "childConsumerFactory")
    public void bulkConsumeChildTask(@Payload List<ChildTask> childTaskList){
        Map<String, List<ChildTask>> scheduler = new ConcurrentHashMap<>();
        for(val childTask: childTaskList){
            List<ChildTask> tempChildTaskList = scheduler.getOrDefault(childTask.getParentId(), new ArrayList<>());
            tempChildTaskList.add(childTask);
            scheduler.put(childTask.getParentId(), tempChildTaskList);
        }
        scheduler.entrySet().parallelStream().forEach(schedule ->{
            for(val childTask: schedule.getValue()){
                this.consumeChildTask(childTask);
            }
        });
    }

    public void consumeChildTask(ChildTask childTask){
        val requestBody = childTask.getParams();
        if(requestBody != null){
            val formBody = RequestBody.create(JSON, requestBody);
            val request = new Request.Builder()
                    .url(childTask.getUrl())
                    .post(formBody)
                    .build();
            val client = new OkHttpClient();

            try {
                val response = client.newCall(request).execute();
                if(!response.isSuccessful()){
                    this.retryChildTask(childTask);
                }else{
                    this.markTaskSuccess(childTask);
                }
                response.body().close();
            } catch (Exception e) {
                retryChildTask(childTask);
            }
        }
    }

    private void markTaskSuccess(ChildTask childTask){
        val masterTask = masterTaskRepository.findTopById(childTask.getParentId());
        int taskRemaining = masterTask.getTaskRemaining() -1;
        if(!masterTask.getStatus().equals("failure")){
            masterTask.setTaskRemaining(taskRemaining);
            if(taskRemaining == 0){
                masterTask.setStatus("success");
            }
            masterTaskRepository.save(masterTask);
        }
    }

    private void retryChildTask(ChildTask task){
        int trial = task.getTrial();
        if(trial < maxRetry){
            task.setTrial(trial + 1);
            childTaskKafkaTemplate.send(childTaskTopic, task);
        }else{
            val masterTask = masterTaskRepository.findTopById(task.getParentId());
            masterTask.setStatus("failure");
            masterTaskRepository.save(masterTask);
        }
    }

}
