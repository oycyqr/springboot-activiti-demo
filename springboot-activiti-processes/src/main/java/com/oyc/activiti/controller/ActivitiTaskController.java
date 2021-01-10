package com.oyc.activiti.controller;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.image.ProcessDiagramGenerator;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: ActivitiProcessesController
 * @Description: 任务相关控制器
 * @Author oyc
 * @Date 2021/1/10 17:36
 * @Version 1.0
 */
@RestController
@RequestMapping("task")
public class ActivitiTaskController {

    @Resource
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private HistoryService historyService;

    /**
     * 部署请假流程
     */
    @GetMapping("deploy")
    public ResponseEntity deployProcesses() {
        HashMap<String, Object> resultMap = new HashMap<>(8);
        //部署对象
        Deployment deployment = repositoryService.createDeployment()
                // bpmn文件
                .addClasspathResource("processes/holiday.bpmn")
                // 图片文件
                .addClasspathResource("processes/holiday.png")
                .key("holiday")
                .name("请假申请流程")
                .deploy();
        System.out.println("流程部署id:" + deployment.getId());
        System.out.println("流程部署名称:" + deployment.getName());
        resultMap.put("id", deployment.getId());
        resultMap.put("name", deployment.getName());
        resultMap.put("key", deployment.getKey());
        resultMap.put("dDeploymentTime", deployment.getDeploymentTime());
        return ResponseEntity.ok(resultMap);
    }

    /**
     * 启动请假流程--传入请假流程的key
     */
    @GetMapping("start")
    public ResponseEntity startProcessByKey(@RequestParam String processesKey) {
        HashMap<String, Object> resultMap = new HashMap<>(8);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processesKey);
        resultMap.put("id", processInstance.getId());
        resultMap.put("name", processInstance.getName());
        resultMap.put("deploymentId", processInstance.getDeploymentId());
        resultMap.put("processDefinitionId", processInstance.getProcessDefinitionId());
        resultMap.put("startUserId", processInstance.getStartUserId());
        resultMap.put("processDefinitionName", processInstance.getProcessDefinitionName());
        return ResponseEntity.ok(resultMap);
    }

    /**
     * 根据流程key和用户名获取待办流程
     *
     * @param processDefinitionKey 流程key(holiday)
     * @param userName             用户名(zhangsan)
     */
    @GetMapping("task")
    public ResponseEntity getTaskByUserName(@RequestParam String processDefinitionKey, @RequestParam String userName) {
        ArrayList<Object> resultList = new ArrayList<>();
        List<Task> taskList = taskService.createTaskQuery()
                .processDefinitionKey(processDefinitionKey)
                //只查询该任务负责人的任务
                .taskAssignee(userName)
                .list();
        taskList.forEach(task -> {
            HashMap<String, Object> map = new HashMap<>(16);
            System.out.println(task);
            //任务ID
            map.put("id", task.getId());
            //任务名称
            map.put("name", task.getName());
            //任务委派人
            map.put("assignee", task.getAssignee());
            //任务创建时间
            map.put("createTime", task.getCreateTime());
            //任务描述
            map.put("description", task.getDescription());

            //任务对应得流程实例id
            map.put("processInstanceId", task.getProcessInstanceId());
            //任务对应得流程定义id
            map.put("processDefinitionId", task.getProcessDefinitionId());
            resultList.add(map);
        });
        return ResponseEntity.ok(resultList);
    }

    /**
     * 根据任务id完成任务
     *
     * @param taskId 任务id
     * @return
     * @throws
     */
    @GetMapping("completeTask")
    public ResponseEntity completeTaskById(@RequestParam String taskId) {
        taskService.complete(taskId);
        return ResponseEntity.ok(String.format("任务id为：%s 已经完成", taskId));
    }

    /**
     * 根据任务id委派
     *
     * @param taskId 任务id
     * @return
     * @throws
     */
    @GetMapping("asigneeTask")
    public ResponseEntity asigneeTaskById(@RequestParam String taskId) {
        taskService.complete(taskId);
        return ResponseEntity.ok(String.format("任务id为：%s 已经完成", taskId));
    }


    /**
     * 生成流程图
     */
    @RequestMapping("createProcessImg")
    public void createProcessImg(@RequestParam String processInstanceId, HttpServletResponse response) throws Exception {
        //获取历史流程实例
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        //根据流程定义获取输入流
        InputStream is = repositoryService.getProcessDiagram(processInstance.getProcessDefinitionId());
        BufferedImage bi = ImageIO.read(is);
        File file = new File(processInstanceId + "Img.png");
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(file);
        ImageIO.write(bi, "png", fos);
        fos.close();
        is.close();
        System.out.println("图片生成成功");
        List<Task> tasks = taskService.createTaskQuery().taskCandidateUser("userId").list();
        for (Task t : tasks) {
            System.out.println(t.getName());
        }
    }

    /**
     * 生成流程图
     */
    @RequestMapping("viewProcessImg")
    public void viewProcessImg(@RequestParam String processInstanceId, HttpServletResponse response) throws Exception {
        //获取历史流程实例
        try {
            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            OutputStream outputStream = response.getOutputStream();
            //根据流程定义获取输入流
            InputStream in = repositoryService.getProcessDiagram(processInstance.getProcessDefinitionId());
            IOUtils.copy(in, outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成流程图（高亮）
     */
    @RequestMapping("viewProcessImgHighLighted")
    public void viewProcessImgHighLighted(@RequestParam String processInstanceId, HttpServletResponse response) {
        try {
            byte[] processImage = getProcessImage(processInstanceId);
            OutputStream outputStream = response.getOutputStream();
            InputStream in = new ByteArrayInputStream(processImage);
            IOUtils.copy(in, outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] getProcessImage(String processInstanceId) throws Exception {
        //  获取历史流程实例
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if (historicProcessInstance == null) {
            throw new Exception();
        } else {
            // 获取流程定义
            ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) repositoryService
                    .getProcessDefinition(historicProcessInstance.getProcessDefinitionId());

            // 获取流程历史中已执行节点，并按照节点在流程中执行先后顺序排序
            List<HistoricActivityInstance> historicActivityInstanceList = historyService
                    .createHistoricActivityInstanceQuery().processInstanceId(processInstanceId)
                    .orderByHistoricActivityInstanceId().desc().list();
            // 已执行的节点ID集合
            List<String> executedActivityIdList = new ArrayList<>();
            @SuppressWarnings("unused") int index = 1;
            System.out.println("获取已经执行的节点ID");
            for (HistoricActivityInstance activityInstance : historicActivityInstanceList) {
                executedActivityIdList.add(activityInstance.getActivityId());
                index++;
            }
            // 获取流程图图像字符流
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());

            //已执行flow的集合
            List<String> executedFlowIdList = getHighLightedFlows(bpmnModel, historicActivityInstanceList);

            ProcessDiagramGenerator processDiagramGenerator = processEngine.getProcessEngineConfiguration().getProcessDiagramGenerator();
            InputStream imageStream = processDiagramGenerator.generateDiagram(bpmnModel, "png", executedActivityIdList, executedFlowIdList, "黑体", "黑体", "黑体", null, 1.0);

            byte[] buffer = new byte[imageStream.available()];
            imageStream.read(buffer);
            imageStream.close();
            return buffer;
        }
    }


    /**
     * 获取已经流转的线
     *
     * @param bpmnModel
     * @param historicActivityInstances
     * @return
     */
    private static List<String> getHighLightedFlows(BpmnModel bpmnModel, List<HistoricActivityInstance> historicActivityInstances) {
        // 高亮流程已发生流转的线id集合
        List<String> highLightedFlowIds = new ArrayList<>();
        // 全部活动节点
        List<FlowNode> historicActivityNodes = new ArrayList<>();
        // 已完成的历史活动节点
        List<HistoricActivityInstance> finishedActivityInstances = new ArrayList<>();

        for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
            FlowNode flowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(historicActivityInstance.getActivityId(), true);
            historicActivityNodes.add(flowNode);
            if (historicActivityInstance.getEndTime() != null) {
                finishedActivityInstances.add(historicActivityInstance);
            }
        }

        FlowNode currentFlowNode = null;
        FlowNode targetFlowNode = null;
        // 遍历已完成的活动实例，从每个实例的outgoingFlows中找到已执行的
        for (HistoricActivityInstance currentActivityInstance : finishedActivityInstances) {
            // 获得当前活动对应的节点信息及outgoingFlows信息
            currentFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(currentActivityInstance.getActivityId(), true);
            List<SequenceFlow> sequenceFlows = currentFlowNode.getOutgoingFlows();

            /**
             * 遍历outgoingFlows并找到已已流转的 满足如下条件认为已已流转： 1.当前节点是并行网关或兼容网关，则通过outgoingFlows能够在历史活动中找到的全部节点均为已流转 2.当前节点是以上两种类型之外的，通过outgoingFlows查找到的时间最早的流转节点视为有效流转
             */
            if ("parallelGateway".equals(currentActivityInstance.getActivityType()) || "inclusiveGateway".equals(currentActivityInstance.getActivityType())) {
                // 遍历历史活动节点，找到匹配流程目标节点的
                for (SequenceFlow sequenceFlow : sequenceFlows) {
                    targetFlowNode = (FlowNode) bpmnModel.getMainProcess().getFlowElement(sequenceFlow.getTargetRef(), true);
                    if (historicActivityNodes.contains(targetFlowNode)) {
                        highLightedFlowIds.add(targetFlowNode.getId());
                    }
                }
            } else {
                List<Map<String, Object>> tempMapList = new ArrayList<>();
                for (SequenceFlow sequenceFlow : sequenceFlows) {
                    for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                        if (historicActivityInstance.getActivityId().equals(sequenceFlow.getTargetRef())) {
                            Map<String, Object> map = new HashMap<>();
                            map.put("highLightedFlowId", sequenceFlow.getId());
                            map.put("highLightedFlowStartTime", historicActivityInstance.getStartTime().getTime());
                            tempMapList.add(map);
                        }
                    }
                }
                if (!CollectionUtils.isEmpty(tempMapList)) {
                    // 遍历匹配的集合，取得开始时间最早的一个
                    long earliestStamp = 0L;
                    String highLightedFlowId = null;
                    for (Map<String, Object> map : tempMapList) {
                        long highLightedFlowStartTime = Long.valueOf(map.get("highLightedFlowStartTime").toString());
                        if (earliestStamp == 0 || earliestStamp >= highLightedFlowStartTime) {
                            highLightedFlowId = map.get("highLightedFlowId").toString();
                            earliestStamp = highLightedFlowStartTime;
                        }
                    }
                    highLightedFlowIds.add(highLightedFlowId);
                }
            }
        }
        return highLightedFlowIds;
    }
}
