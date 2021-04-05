package com.example.gulimall.common;

import com.google.common.collect.Maps;
import com.sun.javafx.tk.Toolkit;
/*import org.activiti.engine.*;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;*/
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import sun.java2d.pipe.RegionSpanIterator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

@SpringBootTest
class CommonApplicationTests {

    @Test
    void contextLoads() {
    }

    /**
     * 初始化数据库
     * @param args
     */
   /* {
        ProcessEngineConfiguration configuration = ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration();
        // 2、做配置
        configuration.setJdbcUrl("jdbc:mysql://localhost:3306/activiti?createDatabaseIfNotExist=true&serverTimezone=UTC");
        configuration.setJdbcDriver("com.mysql.cj.jdbc.Driver");
        configuration.setJdbcUsername("root");
        configuration.setJdbcPassword("root");
// 配置建表策略
        configuration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
// 3、创建ProcessEngine
        ProcessEngine processEngine = configuration.buildProcessEngine();
        System.out.println("processEngine" + processEngine);
    }*/
   /* public static void main(String[] args){
        ProcessEngineConfiguration configuration = ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration();
        // 2、做配置
        configuration.setJdbcUrl("jdbc:mysql://localhost:3306/activiti?createDatabaseIfNotExist=true&serverTimezone=UTC");
        configuration.setJdbcDriver("com.mysql.cj.jdbc.Driver");
        configuration.setJdbcUsername("root");
        configuration.setJdbcPassword("root");
// 配置建表策略
        configuration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
// 3、创建ProcessEngine
        ProcessEngine processEngine = configuration.buildProcessEngine();
        //4.部署流程
        *//*RepositoryService repositoryService = processEngine.getRepositoryService();
        Deployment deployment = repositoryService.createDeployment()
                .name("请假流程1")
                .addClasspathResource("bpmn.bpmn")
                .addClasspathResource("bpmn.png")
                .deploy();
        System.out.println(deployment.getName());*//*
        *//*RepositoryService repositoryService = processEngine.getRepositoryService();
        Deployment deployment = repositoryService.createDeployment()
                .name("请假流程1")
                .addZipInputStream(new ZipInputStream(this.getClass().getResourceAsStream("/bpmn.zip")))
                .deploy();
        System.out.println(deployment.getName());*//*
        *//*//*查询流程部署信息
        RepositoryService repositoryService = processEngine.getRepositoryService();
        repositoryService.createDeploymentQuery()
                //查询条件
                .deploymentName("")
                .deploymentKeyLike("")
                //排序
                .orderByTenantId()
                .orderByDeploymentId()
                .desc()
                //结果
                //.singleResult()
                //记数
                .count();*//*
       *//* //查询流程定义信息
        RepositoryService repositoryService = processEngine.getRepositoryService();
        repositoryService.createProcessDefinitionQuery()
                //查询条件
                .processDefinitionCategory("")
                .processDefinitionKeyLike("")
                //排序
                .orderByProcessDefinitionVersion()
                .desc()
                //结果
                //.singleResult()
                //.list()
                //记数
                .count();*//*
        RepositoryService repositoryService = processEngine.getRepositoryService();

        //启动流程
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById("myProcess_1:1:7504");
       // ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("myProcess_1");
        System.out.println(processInstance.getName());
        //查询张三任务
        TaskService taskService = processEngine.getTaskService();
        List<Task> list = taskService.createTaskQuery().taskAssignee("张三").list();
        for (Task task:list){
            System.out.println(task.getId());
        }
        //办理任务
        TaskService taskService1 = processEngine.getTaskService();
        taskService1.complete("2504");
        System.out.println("任务完成");
        Map<String, Object> map = Maps.newHashMap();
        map.put("days",3);
        //用户提交请假申请的任务id号
        String taskId = "1001";
        //完成任务的时候，同时设置流程变量
        taskService1.complete(taskId,map);

    }*/
}
