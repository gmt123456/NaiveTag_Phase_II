package top.minecode.service.taskmanage;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.minecode.dao.statistic.RequesterStatisticDao;
import top.minecode.dao.task.requester.RequesterTaskDao;
import top.minecode.dao.task.requester.TaskDeliveryDao;
import top.minecode.dao.user.UserDao;
import top.minecode.domain.task.TaskConfig;
import top.minecode.domain.task.TaskInfo;
import top.minecode.domain.task.requester.NewTaskInfo;
import top.minecode.domain.task.requester.RequesterTaskDetails;
import top.minecode.domain.task.requester.RequesterTaskInfo;
import top.minecode.domain.task.requester.TaskParticipant;
import top.minecode.domain.user.User;
import top.minecode.exception.InvalidFileStructureException;
import top.minecode.po.FirstLevelTaskPO;
import top.minecode.po.SecondLevelTaskPO;
import top.minecode.utils.unzip.ZipHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created on 2018/4/8.
 * Description:
 * @author Liao
 */
@Service
public class RequesterTaskService {

    private RequesterTaskDao requesterTaskDao;
    private RequesterStatisticDao requesterStatisticDao;
    private TaskDeliveryDao taskDeliveryDao;
    private UserDao userDao;

    @Autowired
    public void setRequesterStatisticDao(RequesterStatisticDao requesterStatisticDao) {
        this.requesterStatisticDao = requesterStatisticDao;
    }

    @Autowired
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    @Autowired
    public void setTaskDeliveryDao(TaskDeliveryDao taskDeliveryDao) {
        this.taskDeliveryDao = taskDeliveryDao;
    }

    @Autowired
    public void setRequesterTaskDao(RequesterTaskDao requesterTaskDao) {
        this.requesterTaskDao = requesterTaskDao;
    }

    public List<RequesterTaskInfo> getTasksInfo(int ownerId) {

        List<FirstLevelTaskPO> tasks = requesterTaskDao.getTasks(ownerId);
        Map<Integer, Double> tasksProcess = requesterStatisticDao.getTaskTotalProcess(ownerId);

        return tasks.stream().map(po -> new RequesterTaskInfo(po, tasksProcess.get(po.getId())))
                .collect(Collectors.toList());
    }

    public String getResult(int taskId) {
        return requesterTaskDao.getTaskResultPath(taskId);
    }

    public boolean isDone(int taskId) {
        return requesterTaskDao.isDone(taskId);
    }

    public List<RequesterTaskDetails> getTaskDetails(int taskId) {
        // Get task information and participants
        Map<Integer, Double> processes = requesterStatisticDao.getSecondLevelTaskProcess(taskId);
        Map<Integer, List<TaskParticipant>> participants = requesterTaskDao.getParticipants(taskId);
        Map<Integer, TaskInfo> taskInfoMap = requesterTaskDao.secondLevelTaskInfo(taskId);
        // Combine them to RequesterTaskDetails list
        List<RequesterTaskDetails> details = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : processes.entrySet()) {
            int secondLvTaskId = entry.getKey();
            details.add(new RequesterTaskDetails(entry.getValue(),
                    participants.get(secondLvTaskId), taskInfoMap.get(secondLvTaskId)));
        }

        return details;
    }

    public void saveFile(MultipartFile dataSet, MultipartFile taskJson, String dataDirectory, NewTaskInfo newTaskInfo) throws IOException, InvalidFileStructureException {
        // Save the file
        String dataSetPath = dataDirectory + dataSet.getOriginalFilename();
        String taskJsonPath = dataDirectory + "task.json";
        // Handle duplicated situation
        File dataSetFile = handelDuplicated(new File(dataSetPath));
        File taskJsonFile = handelDuplicated(new File(taskJsonPath));
        // Make sure the directory already exists todo: may not necessary
        File dir = dataSetFile.getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Make data directory failed");
        }
        dataSet.transferTo(dataSetFile);
        taskJson.transferTo(taskJsonFile);

        // Add first level task
        FirstLevelTaskPO flTask = taskDeliveryDao.addFirstLevelTask(dataDirectory, newTaskInfo);
        String imageFilePath = unZip(dataDirectory, dataSetFile.getPath());

        // Check File's Structure
        if (!validFileStructure(imageFilePath))
            throw new InvalidFileStructureException();

        TaskConfig taskConfig = new TaskConfig(taskJsonPath);
        // Calculate how many third level tasks a second level task should keep
        double score = newTaskInfo.getScore() / taskConfig.size();
        for (TaskInfo info : taskConfig) {
            SecondLevelTaskPO secondLevelTask = taskDeliveryDao.addSecondLevelTask(info, flTask, score);
            taskDeliveryDao.addThirdLevelTask(imageFilePath, secondLevelTask, info, flTask);
        }
    }

    public void minusPoints(User user, double score) {
        userDao.updateUserScore(user, -score);
    }

    private File handelDuplicated(File file) {
        Random random = new Random();
        while (file.exists()) {
            String filePath = file.getPath();
            int dotIndex = filePath.lastIndexOf(".");
            file = new File(filePath.substring(0, dotIndex) + random.nextInt() + filePath.substring(dotIndex));
        }
        return file;
    }

    @NotNull
    private String unZip(String dataDirectory, String dataSetPath) throws IOException {
        // Unzip the file
        ZipHelper zipper = new ZipHelper();
        String unZipPath = dataDirectory + "images";
        return zipper.unZip(dataSetPath, unZipPath);
    }

    private boolean validFileStructure(String filePath) {
        File file = new File(filePath);
        File[] files = file.listFiles();
        if (files == null)
            return false;
        for (File subDir : files) {
            if (subDir.getName().equals("data"))
                return true;
        }
        return false;
    }

    public int getNewTaskSequenceNumber(int ownerId) {
        return requesterTaskDao.getTasks(ownerId).size() + 1;
    }
}
