package io.katharsis.resource.mock.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.katharsis.queryParams.QueryParams;
import io.katharsis.repository.RelationshipRepository;
import io.katharsis.resource.mock.models.Project;
import io.katharsis.resource.mock.models.Task;
import io.katharsis.resource.mock.repository.util.Relation;

public class ProjectToTaskRepository extends AbstractRelationShipRepository<Project> implements RelationshipRepository<Project, Long, Task, Long> {

    private final static ConcurrentMap<Relation<Project>, Integer> STATIC_REPOSITORY = new ConcurrentHashMap<>();

    public static void clear() {
        STATIC_REPOSITORY.clear();
    }

    @Override
    ConcurrentMap<Relation<Project>, Integer> getRepo() {
        return STATIC_REPOSITORY;
    }

    @Override
    public Task findOneTarget(Long sourceId, String fieldName, QueryParams queryParams) {
        Map<Relation<Project>, Integer> repo = getRepo();
        for (Relation<Project> relation : repo.keySet()) {
            if (relation.getSource().getId().equals(sourceId) &&
                    relation.getFieldName().equals(fieldName)) {
                Task task = new Task();
                task.setId((Long) relation.getTargetId());
                return task;
            }
        }
        return null;
    }

    @Override
    public Iterable<Task> findManyTargets(Long sourceId, String fieldName, QueryParams queryParams) {
        return null;
    }
}
