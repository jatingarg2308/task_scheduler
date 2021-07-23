package com.example.scheduler.Controller;

import com.example.scheduler.Model.ChildTask;
import com.example.scheduler.Model.MasterTask;
import com.example.scheduler.Services.TaskService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class MasterTaskController {

    @Autowired
    private TaskService taskService;

    @PostMapping("/")
    public ResponseEntity<Map<String, String>> registerMasterTask(@RequestBody MasterTask masterTask){
        Map<String, String> response = new HashMap<>();
        try {
            taskService.saveMasterTask(masterTask);
            response.put("message", "Master task registered successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (JsonProcessingException e) {
            response.put("message", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, String>> bulkRegisterMasterTask(@RequestBody List<MasterTask> masterTaskList){
        Map<String, String> response = new HashMap<>();
        try {
            taskService.bulkSaveMasterTask(masterTaskList);
            response.put("message", "Master task registered successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (JsonProcessingException e) {
            response.put("message", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/partition")
    public ResponseEntity<List<ChildTask>> partitionLogic(@RequestBody String params) throws JsonProcessingException {
        List<ChildTask> childTasks = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        val actualParams = mapper.readValue(params, Map.class);
        val number = (int)actualParams.getOrDefault("number", 10);
        for(int i = 0; i <= number; i+= 3){
            ChildTask childTask = new ChildTask("http://localhost:8080/childExec");
            actualParams.put("from", i);
            actualParams.put("to", Math.min(i+3, number));
            val finalParams = mapper.writeValueAsString(actualParams);
            childTask.setParams(finalParams);
            childTasks.add(childTask);
        }
        return new ResponseEntity<>(childTasks, HttpStatus.OK);
    }

    @PostMapping("/childExec")
    public ResponseEntity<String> printfinaList(@RequestBody String params) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        val actualParams = mapper.readValue(params, Map.class);
        for(int i=(int)actualParams.getOrDefault("from", 0); i<(int)actualParams.getOrDefault("to", 0); i++){
            System.out.println(params);
        }
        return new ResponseEntity<>("Success", HttpStatus.OK);
    }

}
