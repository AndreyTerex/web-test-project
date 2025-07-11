package dao.impl;

import dao.JsonFileDao;
import dao.ResultDao;
import entity.Result;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import exceptions.DataAccessException;
import lombok.extern.slf4j.Slf4j;

/**
 * Data access object for managing test result entities.
 */
@Slf4j
public class ResultDaoImpl implements ResultDao {
    private final JsonFileDao<Result> baseDao;
    private final Map<UUID, Result> resultMap;
    private final Map<UUID, List<Result>> resultsByUserIdMap;
    private final Map<UUID, List<Result>> resultsByTestIdMap;

    public ResultDaoImpl(JsonFileDao<Result> baseDao) {
        this.baseDao = baseDao;
        resultMap = new ConcurrentHashMap<>();
        resultsByUserIdMap = new ConcurrentHashMap<>();
        resultsByTestIdMap = new ConcurrentHashMap<>();
        loadResultsInCache();
    }

    private void loadResultsInCache() {
        List<Result> all = baseDao.findAll();
        for (Result result : all) {
            resultMap.put(result.getId(), result);
            resultsByUserIdMap.computeIfAbsent(result.getUserId(), k -> new CopyOnWriteArrayList<>()).add(result);
            resultsByTestIdMap.computeIfAbsent(result.getTestId(), k -> new CopyOnWriteArrayList<>()).add(result);
        }
        log.info("Results cache refreshed");
    }


    /**
     * Saves a result and updates all caches.
     */
    public void save(Result result) throws DataAccessException {
        log.debug("Saving new result for testId: {} and userId: {}", result.getTestId(), result.getUserId());
        baseDao.add(result);
        resultMap.put(result.getId(), result);
        resultsByTestIdMap.computeIfAbsent(result.getTestId(), k -> new CopyOnWriteArrayList<>()).add(result);
        resultsByUserIdMap.computeIfAbsent(result.getUserId(), k -> new CopyOnWriteArrayList<>()).add(result);
    }

    /**
     * Returns all results for a given user id.
     */
    public List<Result> getAllResultsByUserId(UUID id) {
        return resultsByUserIdMap.getOrDefault(id, Collections.emptyList());
    }

    /**
     * Returns all results for a given test id.
     */
    public List<Result> getAllResultsByTestId(UUID id) {
        return resultsByTestIdMap.getOrDefault(id, Collections.emptyList());
    }


    /**
     * Finds a result by its id.
     */
    public Optional<Result> findById(UUID resultId) {
        return Optional.ofNullable(resultMap.get(resultId));
    }

    /**
     * Returns the total number of results.
     */
    public Integer getCount() {
        return resultMap.size();
    }
}
