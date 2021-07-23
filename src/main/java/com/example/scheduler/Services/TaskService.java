package com.example.scheduler.Services;

import com.example.scheduler.Model.ChildTask;
import com.example.scheduler.Model.MasterTask;
import com.example.scheduler.Repository.MasterTaskRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class TaskService {

    @Value("${scheduler.child_topic}")
    private String childTaskTopic;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private MasterTaskRepository masterTaskRepository;

    @Autowired
    private KafkaTemplate<String, ChildTask> childTaskKafkaTemplate;


    public void saveMasterTask(MasterTask masterTask) throws JsonProcessingException {
        mapper.readValue(masterTask.getParams(), Map.class);
        this.consumeMasterTask(masterTask);
    }

    public void bulkSaveMasterTask(List<MasterTask> masterTaskList) throws JsonProcessingException {
        for(val masterTask: masterTaskList){
            this.saveMasterTask(masterTask);
        }
    }

    public void consumeMasterTask(MasterTask masterTask) {
        try{
            val requestBody = masterTask.getParams();
            val formBody = RequestBody.create(JSON, requestBody);
            val request = new Request.Builder()
                    .url(masterTask.getUrl())
                    .post(formBody)
                    .build();
            val client = new OkHttpClient();
            val response = client.newCall(request).execute();
            if(!response.isSuccessful()){
                System.out.println("Error while executing Master Task");
            }else{
                val responseBody = response.body().string();
                saveChildTask(responseBody, masterTask);
            }
            response.body().close();
        } catch (IOException | NullPointerException e) {
            System.out.printf("Error while executing Master Task %n", e.getMessage());
        }
    }

    private void saveChildTask(String listChildTask, MasterTask masterTask) throws JsonProcessingException {
        val childTaskList = mapper.readValue(listChildTask, ChildTask[].class);
        masterTask.setTaskRemaining(childTaskList.length);
        masterTaskRepository.save(masterTask);

        Arrays.stream(childTaskList).parallel().forEach(childTask -> {
            try {
                childTask.setParentId(masterTask.getId());
                mapper.readValue(childTask.getParams(), Map.class);
                childTaskKafkaTemplate.send(childTaskTopic, childTask);
            } catch (JsonProcessingException e) {
                System.out.printf("Error while executing Master Task %n", e.getMessage());
                masterTask.setStatus("failure");
                masterTaskRepository.save(masterTask);
            }
        });
    }

}
