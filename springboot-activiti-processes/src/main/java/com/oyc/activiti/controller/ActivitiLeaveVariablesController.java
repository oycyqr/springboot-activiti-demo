package com.oyc.activiti.controller;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
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
 * @ClassName: ActivitiLeaveController
 * @Description: 离职申请
 * @Author oyc
 * @Date 2021/1/10 17:36
 * @Version 1.0
 */
@RestController
@RequestMapping("leaveVal")
public class ActivitiLeaveVariablesController {

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
     * 部署离职申请流程
     */
    @GetMapping("deploy")
    public ResponseEntity deployProcesses() {
        HashMap<String, Object> resultMap = new HashMap<>(8);
        //部署对象
        Deployment deployment = repositoryService.createDeployment()
                // bpmn文件
                .addClasspathResource("processes/leaveVal.bpmn")
                // 图片文件
                //.addClasspathResource("processes/leave.png")
                .name("离职申请流程1.0.2").key("leaveVal")
                .deploy();
        System.out.println("流程部署id:" + deployment.getId());
        System.out.println("流程部署名称:" + deployment.getName());
        resultMap.put("流程部署id:", deployment.getId());
        resultMap.put("流程部署名称-name:", deployment.getName());
        resultMap.put("流程部署key", deployment.getKey());
        resultMap.put("流程部署时间-deploymentTime", deployment.getDeploymentTime());
        return ResponseEntity.ok(resultMap);
    }

    /**
     * 删除流程发布
     *
     * @param deploymentId 流程定义Id
     * @return
     * @throws
     */
    @GetMapping("deleteDeployment")
    public ResponseEntity deleteDeployment(@RequestParam String deploymentId) {
        // 获得流程发布
        Deployment deployment = repositoryService
                .createDeploymentQuery()
                .deploymentId(deploymentId)
                .singleResult();
        //是否存在
        if (deployment == null) {
            return ResponseEntity.ok(String.format("流程定义：%s 不存在", deploymentId));
        }
        repositoryService.deleteDeployment(deploymentId);
        //级联删除,会删除所有运行中与之关联的流程、任务等内容
        //repositoryService.deleteDeployment(deploymentId,true);
        return ResponseEntity.ok(String.format("流程定义：%s 删除成功", deploymentId));
    }
    /**
     * 1 启动流程时设置流程变量
     * 2 办理任务时设置流程变量
     * 3 通过当前流程实例设置
     * 4 通过当前任务设置
     */
    /**
     * 启动离职申请流程--传入离职申请流程的key
     */
    @GetMapping("startVal")
    public ResponseEntity startProcessAndSetValByKey(@RequestParam String processesKey) {
        HashMap<String, Object> resultMap = new HashMap<>(8);
        // 定义流程变量
        Map<String, Object> variables = new HashMap<String, Object>();
        //变量名是president，变量值是oyc,变量名也可以是一个对象
        variables.put("president", "oyc");
        //启动流程并设置变量
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processesKey, variables);

        resultMap.put("id", processInstance.getId());
        resultMap.put("name", processInstance.getName());
        resultMap.put("deploymentId", processInstance.getDeploymentId());
        resultMap.put("processDefinitionId", processInstance.getProcessDefinitionId());
        resultMap.put("startUserId", processInstance.getStartUserId());
        resultMap.put("processDefinitionName", processInstance.getProcessDefinitionName());
        return ResponseEntity.ok(resultMap);
    }


    /**
     * 启动离职申请流程--传入离职申请流程的key
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
     * 根据流程key和用户名获取待办流程
     *
     * @param processDefinitionKey 流程key(holiday)
     * @param userName             用户名(zhangsan)
     */
    @GetMapping("groupTask")
    public ResponseEntity getGroupTaskByUserName(@RequestParam String processDefinitionKey, @RequestParam String userName) {
        ArrayList<Object> resultList = new ArrayList<>();
        List<Task> taskList = taskService.createTaskQuery()
                .processDefinitionKey(processDefinitionKey)
                //只查询该任务负责人的任务
                .taskCandidateUser(userName)
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
     * 根据任务id和用户名签收（拾取）任务
     *
     * @param taskId   任务id
     * @param userName 用户名(zhangsan)
     */
    @GetMapping("claimTask")
    public ResponseEntity claimTask(@RequestParam String taskId, @RequestParam String userName) {
        taskService.claim(taskId, userName);
        return ResponseEntity.ok(String.format("用户[%s] 签收任务 [%s] 完成", userName, taskId));
    }

    /**
     * 归还任务
     *
     * @param taskId 任务id
     */
    @GetMapping("returnTask")
    public ResponseEntity returnTask(@RequestParam String taskId) {
        taskService.claim(taskId, null);
        return ResponseEntity.ok(String.format("归还任务 [%s] 完成", taskId));
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
        // 定义流程变量
        Map<String, Object> variables = new HashMap<String, Object>();
        //变量名是president，变量值是oyc,变量名也可以是一个对象
        variables.put("president", "oyc");
        //完成任务并设置变量
        taskService.complete(taskId, variables);
        return ResponseEntity.ok(String.format("任务id为：%s 已经完成", taskId));
    }

    /**
     * @param processInstanceId 当前流程实例执行 id，通常设置为当前执行的流程实例,必须当前未结束 流程实例的执行 id，通常此 id 设置流程实例 的 id。
     * @return
     * @throws
     * @Description: 通过流程实例 id 设置全局变量，该流程实例必须未执行完成。
     */
    @GetMapping("setGlobalVariableByProcessInstanceId")
    public void setGlobalVariableByExecutionId(@RequestParam String processInstanceId) {
        //通过流程实例id 设置流程变量-单个值
        runtimeService.setVariable(processInstanceId, "president", "zhangqiang");
        Object president = runtimeService.getVariable(processInstanceId, "president");
        System.out.println("1流程实例变量-president：" + president);

        // 定义流程变量
        Map<String, Object> variables = new HashMap<String, Object>();
        //变量名是president，变量值是oyc,变量名也可以是一个对象
        variables.put("president", "oyc");
        variables.put("president1", "oyc1");

        //一次设置多个值
        runtimeService.setVariables(processInstanceId, variables);
        president = runtimeService.getVariable(processInstanceId, "president");
        System.out.println("2流程实例变量-president：" + president);
    }

    /**
     * @param taskId 当前待办任务id
     * @return
     * @throws 注意：任务id必须是当前待办任务id，act_ru_task中存在。如果该任务已结束，报错：
     * @Description: 通过当前任务设置
     */
    @GetMapping("setGlobalVariableByTaskId")
    public void setGlobalVariableByTaskId(@RequestParam String taskId) {
        TaskService taskService = processEngine.getTaskService();
        // 定义流程变量
        Map<String, Object> variables = new HashMap<String, Object>();
        //变量名是president，变量值是oyc,变量名也可以是一个对象
        variables.put("president", "oyc");
        variables.put("president1", "oyc1");
        //通过任务设置流程变量
        taskService.setVariable(taskId, "president", "president");
        Object president = taskService.getVariable(taskId, "president");
        System.out.println("1流程实例变量-president：" + president);
        //一次设置多个值
        taskService.setVariables(taskId, variables);
        president = taskService.getVariable(taskId, "president");
        System.out.println("2流程实例变量-president：" + president);
    }

    /**
     * 查询流程实例
     *
     * @param processDefinitionKey 流程定义key
     * @return
     * @throws
     */
    @GetMapping("queryProcessInstance")
    public ResponseEntity queryProcessInstance(@RequestParam String processDefinitionKey) {
        // 获得流程定义
        List<ProcessInstance> processInstanceList = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .list();
        ArrayList<Object> resultList = new ArrayList<>();
        for (ProcessInstance processInstance : processInstanceList) {
            HashMap<Object, Object> map = new HashMap<>();
            System.out.println("----------------------------");
            System.out.println("流程实例id：" + processInstance.getProcessInstanceId());
            System.out.println("所属流程定义id：" + processInstance.getProcessDefinitionId());
            System.out.println("是否执行完成：" + processInstance.isEnded());
            System.out.println("是否暂停：" + processInstance.isSuspended());
            System.out.println("当前活动标识 ： " + processInstance.getActivityId());

            map.put("流程实例id：", processInstance.getProcessInstanceId());
            map.put("所属流程定义id：", processInstance.getProcessDefinitionId());
            map.put("是否执行完成：", processInstance.isEnded());
            map.put("是否暂停：", processInstance.isSuspended());
            map.put("当前活动标识 ： ", processInstance.getActivityId());
            resultList.add(map);
        }
        return ResponseEntity.ok(resultList);
    }

    /**
     * 查询历史任务
     *
     * @param processDefinitionKey 流程定义key
     * @param processInstanceId    流程实例id
     * @return
     * @throws
     */
    @GetMapping("queryHistoricTask")
    public ResponseEntity queryHistoricTask(/*@RequestParam String processDefinitionKey,*/@RequestParam String processInstanceId) {

        List<HistoricTaskInstance> instancelist = historyService.createHistoricTaskInstanceQuery()
                //.processDefinitionKey(processDefinitionKey)
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceEndTime()
                .asc()
                .list();

        ArrayList<Object> resultList = new ArrayList<>();
        for (HistoricTaskInstance taskInstance : instancelist) {
            HashMap<Object, Object> map = new HashMap<>();
            System.out.println("----------------------------");
            System.out.println("id：" + taskInstance.getId());
            System.out.println("name：" + taskInstance.getName());
            System.out.println("assignee：" + taskInstance.getAssignee());
            System.out.println("processInstanceId：" + taskInstance.getProcessInstanceId());
            System.out.println("processDefinitionId：" + taskInstance.getProcessDefinitionId());
            System.out.println("endTime：" + taskInstance.getEndTime());

            map.put("id：", taskInstance.getId());
            map.put("name：", taskInstance.getName());
            map.put("assignee：", taskInstance.getAssignee());
            map.put("processInstanceId：", taskInstance.getProcessInstanceId());
            map.put("processDefinitionId：", taskInstance.getProcessDefinitionId());
            map.put("endTime：", taskInstance.getEndTime());
            resultList.add(map);
        }
        return ResponseEntity.ok(resultList);
    }

    /**
     * 查询历史活动
     * List<HistoricActivityInstance> list = processEngine.getHistoryService()
     * .createHistoricActivityInstanceQuery()
     * .processInstanceId(processInstanceId)
     * .list();
     * 查询历史任务
     * List<HistoricTaskInstance> list = processEngine.getHistoryService()
     * .createHistoricTaskInstanceQuery()
     * .processInstanceId(processInstanceId)
     * .list();
     *
     * @param processDefinitionKey
     * @param processInstanceId
     * @return
     */
    @GetMapping("queryHistoricActivity")
    public ResponseEntity queryHistoricActivity(@RequestParam String processInstanceId) {
        List<HistoricActivityInstance> instancelist = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceEndTime()
                .asc()
                .list();

        ArrayList<Object> resultList = new ArrayList<>();
        for (HistoricActivityInstance activityInstance : instancelist) {
            HashMap<Object, Object> map = new HashMap<>();
            System.out.println("----------------------------");
            System.out.println("id：" + activityInstance.getId());
            System.out.println("name：" + activityInstance.getActivityName());

            System.out.println("assignee：" + activityInstance.getAssignee());
            System.out.println("processInstanceId：" + activityInstance.getProcessInstanceId());
            System.out.println("processDefinitionId：" + activityInstance.getProcessDefinitionId());
            System.out.println("endTime：" + activityInstance.getEndTime());

            map.put("id：", activityInstance.getId());
            map.put("name：", activityInstance.getActivityName());
            map.put("assignee：", activityInstance.getAssignee());
            map.put("processInstanceId：", activityInstance.getProcessInstanceId());
            map.put("processDefinitionId：", activityInstance.getProcessDefinitionId());
            map.put("endTime：", activityInstance.getEndTime());
            resultList.add(map);
        }
        return ResponseEntity.ok(resultList);
    }


    /**
     * 挂起激活流程定义
     *
     * @param processDefinitionId 流程定义Id
     * @return
     * @throws
     */
    @GetMapping("suspendOrActivateProcessDefinition")
    public ResponseEntity suspendOrActivateProcessDefinition(@RequestParam String processDefinitionId) {
        // 获得流程定义
        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();
        //是否暂停
        boolean suspend = processDefinition.isSuspended();
        if (suspend) {
            //如果暂停则激活，这里将流程定义下的所有流程实例全部激活
            repositoryService.activateProcessDefinitionById(processDefinitionId, true, null);
            System.out.println("流程定义：" + processDefinitionId + "激活");
            return ResponseEntity.ok(String.format("流程定义：%s激活", processDefinitionId));

        } else {
            //如果激活则挂起，这里将流程定义下的所有流程实例全部挂起
            repositoryService.suspendProcessDefinitionById(processDefinitionId, true, null);
            System.out.println("流程定义：" + processDefinitionId + "挂起");
            return ResponseEntity.ok(String.format("流程定义：%s挂起", processDefinitionId));
        }
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
