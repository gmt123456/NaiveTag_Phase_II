package top.minecode.dao.task;

import org.springframework.stereotype.Repository;
import top.minecode.domain.task.ThirdLevelTaskState;
import top.minecode.domain.user.User;
import top.minecode.domain.user.Worker;
import top.minecode.po.DataBase;
import top.minecode.po.ThirdLevelTaskPO;
import top.minecode.po.ThirdLevelTaskResultPO;
import top.minecode.po.WorkerFilterPO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created on 2018/4/4.
 * Description:
 *
 * @author iznauy
 */

@Repository
public class WorkerTaskDao {

    public List<ThirdLevelTaskPO> getAccessibleTaskList(User user) {

        // 获取用户当前排名比例
        double ratio = getUserRatio(user);

        List<ThirdLevelTaskPO> allThirdLevelTaskPOS = DataBase.thirdLevelTaskPOList.getThirdLevelTaskPOS();
        List<WorkerFilterPO> workerFilterPOS = DataBase.workerFilterPOList.getWorkerFilterList();

        // 根据任务状态、用户排名是否足够接、用户是否已经接过或者完成过进行筛选，筛选出可以接的任务
        return allThirdLevelTaskPOS.stream().filter(e -> e.getState() == ThirdLevelTaskState.doing)
                .filter( e -> workerFilterPOS.stream()
                    .filter( t -> t.getId().equals(e.getWorkerFilterId()))
                        .findFirst().get().getWorkerRankRatio() >= ratio
                ).filter(e -> !(e.getFinishedWorkerIds().contains(user.getId())
                    || e.getCurrentDoingWorkerIds().contains(user.getId())))
                .collect(Collectors.toList());
    }

    private double getUserRatio(User user) {
        Integer rank = DataBase.rankPO.getRankByName(user.getName());
        int totalWorkerAmount = DataBase.workerPOList.getNextWorkerId() - 1;
        return rank == null ? 1.0 : rank * 1.0 / totalWorkerAmount;
    }

    public List<ThirdLevelTaskPO> searchingTaskByKey(User user, String key) {

        // 所有的任务
        List<ThirdLevelTaskPO> allThirdLevelTaskPOS = DataBase.thirdLevelTaskPOList.getThirdLevelTaskPOS();


        // 字符串匹配
        // 不会柔性匹配算法，就完全匹配了..
        return allThirdLevelTaskPOS.stream().filter(e -> e.getState() == ThirdLevelTaskState.doing)
                .filter(e -> e.getTaskName().contains(key))
                .filter(e -> !(e.getFinishedWorkerIds().contains(user.getId())
                        || e.getCurrentDoingWorkerIds().contains(user.getId())))
                .collect(Collectors.toList());
    }

    public ThirdLevelTaskResultPO loadTaskResultByUserAndTaskId(User user, int taskId) {
        List<ThirdLevelTaskResultPO> resultPOS = DataBase.thirdLevelTaskResultPOList
                .getThirdLevelTaskResultPOS();
        return resultPOS.stream().filter(e -> e.getDoerId().equals(user.getId())
                && e.getThirdLevelTaskId().equals(taskId)).findFirst().orElse(null);
    }

    public ThirdLevelTaskPO loadTaskByTaskId(int taskId) {
        List<ThirdLevelTaskPO> thirdLevelTaskPOS = DataBase.thirdLevelTaskPOList.getThirdLevelTaskPOS();
        return thirdLevelTaskPOS.stream().filter(e -> e.getId().equals(taskId))
                .findFirst().orElse(null);
    }

    public boolean canAcceptTask(User user, int taskId) {
        ThirdLevelTaskPO taskPO = loadTaskByTaskId(taskId);
        double ratio = getUserRatio(user);
        if (taskPO == null)
            return false;
        Double lowBound =  DataBase.workerFilterPOList.getWorkerFilterList().stream()
                .filter(e -> e.getId().equals(taskPO.getWorkerFilterId())).findFirst()
                .orElse(null).getWorkerRankRatio();
        return lowBound == null || lowBound > ratio;

    }


}
