package top.minecode.dao.task.requester;

import top.minecode.po.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * Created on 2018/4/11.
 * Description:
 * @author Liao
 */
public enum TaskMaps {
    INSTANCE;

    private Map<Integer, List<Integer>> oneTwoMap;
    private Map<Integer, List<Integer>> twoThreeMap;
    private Map<Integer, List<Integer>> oneThreeMap;

    TaskMaps() {
        updateMaps();
    }

    public List<Integer> getSecondTasks(int firstTaskId) {
        return oneTwoMap.get(firstTaskId);
    }

    public Map<Integer, List<ThirdLevelTaskPO>> oneThreeIdObjMap(int ownerId) {
        List<Integer> firstLevelTasks = getUserFirstLevelTaskIds(ownerId);
        Function<Integer, List<ThirdLevelTaskPO>> mapper = idThirdLevelTaskMapper(firstLevelTasks, oneThreeMap);

        return transformIdToObject(firstLevelTasks, mapper);
    }

    public Map<Integer, List<ThirdLevelTaskPO>> twoThreeIdObjMap(int firstLevelTaskId) {
        List<Integer> secondLevelTasks = getSecondTasks(firstLevelTaskId);

        return transformIdToObject(secondLevelTasks, idThirdLevelTaskMapper(secondLevelTasks, twoThreeMap));
    }

    private <T> Map<Integer, List<T>> transformIdToObject(List<Integer> parentIds, Function<Integer, List<T>> mapper) {
        return parentIds.stream().collect(Collectors.toMap(Function.identity(), mapper));
    }

    private Function<Integer, List<ThirdLevelTaskPO>> idThirdLevelTaskMapper(List<Integer> parentIds,
                                                                             Map<Integer, List<Integer>> relations) {
        Table<ThirdLevelTaskPO> thirdLevelTasks = TableFactory.thirdLevelTaskTable();

        return id -> relations.get(id).stream()
                .map(thirdId -> thirdLevelTasks.getPOBy(thirdId, ThirdLevelTaskPO::getId))
                .collect(Collectors.toList());
    }

    private List<Integer> getUserFirstLevelTaskIds(int ownerId) {
        return TableFactory.firstLevelTaskTable()
                .getAttributesBy(ownerId, FirstLevelTaskPO::getOwnerId, FirstLevelTaskPO::getId);
    }

    private void updateMaps() {
        Table<SecondLevelTaskPO> secondLevelTasks = TableFactory.secondLevelTaskTable();
        Table<ThirdLevelTaskPO> thirdLevelTasks = TableFactory.thirdLevelTaskTable();
        // Update first-second level tasks map
        oneTwoMap = secondLevelTasks.getAll()
                .stream().collect(groupingBy(SecondLevelTaskPO::getFirstLevelTaskId,
                        mapping(SecondLevelTaskPO::getId, toList())));

        // Update second-third level tasks map
        twoThreeMap = thirdLevelTasks.getAll()
                .stream().collect(groupingBy(ThirdLevelTaskPO::getSecondLevelTaskId,
                        mapping(ThirdLevelTaskPO::getId, toList())));

        // Update first-third level tasks map
        oneThreeMap = new HashMap<>();
        for (Map.Entry<Integer, List<Integer>> twoThreeEntry : twoThreeMap.entrySet()) {
            SecondLevelTaskPO secondLevelTask = secondLevelTasks
                    .getPOBy(twoThreeEntry.getKey(), SecondLevelTaskPO::getId);
            int firstLevelTaskId = secondLevelTask.getFirstLevelTaskId();
            if (oneThreeMap.get(firstLevelTaskId) == null) {
                oneThreeMap.put(firstLevelTaskId, twoThreeEntry.getValue());
            } else {
                oneThreeMap.get(firstLevelTaskId).addAll(twoThreeEntry.getValue());
            }
        }
    }
}
