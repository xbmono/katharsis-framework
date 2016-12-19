package io.katharsis.dispatcher.controller.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.katharsis.dispatcher.controller.BaseControllerTest;
import io.katharsis.dispatcher.controller.HttpMethod;
import io.katharsis.dispatcher.controller.Response;
import io.katharsis.queryspec.QuerySpec;
import io.katharsis.queryspec.internal.QueryParamsAdapter;
import io.katharsis.request.path.JsonPath;
import io.katharsis.request.path.ResourcePath;
import io.katharsis.resource.Document;
import io.katharsis.resource.Relationship;
import io.katharsis.resource.Resource;
import io.katharsis.resource.ResourceId;
import io.katharsis.resource.annotations.JsonApiResource;
import io.katharsis.resource.mock.models.Project;
import io.katharsis.resource.mock.models.ProjectPolymorphic;
import io.katharsis.resource.mock.repository.TaskToProjectRepository;
import io.katharsis.resource.mock.repository.UserToProjectRepository;
import io.katharsis.resource.registry.ResourceRegistry;
import io.katharsis.response.HttpStatus;
import io.katharsis.utils.ClassUtils;

public class RelationshipsResourcePostTest extends BaseControllerTest {

	private static final String REQUEST_TYPE = HttpMethod.POST.name();

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private UserToProjectRepository localUserToProjectRepository;

	@Before
	public void beforeTest() throws Exception {
		localUserToProjectRepository = new UserToProjectRepository();
		localUserToProjectRepository.removeRelations("project");
		localUserToProjectRepository.removeRelations("assignedProjects");
	}

	@Test
	public void onValidRequestShouldAcceptIt() {
		// GIVEN
		JsonPath jsonPath = pathBuilder.buildPath("tasks/1/relationships/project");
		ResourceRegistry resourceRegistry = mock(ResourceRegistry.class);
		RelationshipsResourcePost sut = new RelationshipsResourcePost(resourceRegistry, typeParser);

		// WHEN
		boolean result = sut.isAcceptable(jsonPath, REQUEST_TYPE);

		// THEN
		assertThat(result).isTrue();
	}

	@Test
	public void onNonRelationRequestShouldDenyIt() {
		// GIVEN
		JsonPath jsonPath = new ResourcePath("tasks");
		ResourceRegistry resourceRegistry = mock(ResourceRegistry.class);
		RelationshipsResourcePost sut = new RelationshipsResourcePost(resourceRegistry, typeParser);

		// WHEN
		boolean result = sut.isAcceptable(jsonPath, REQUEST_TYPE);

		// THEN
		assertThat(result).isFalse();
	}

	@Test
	public void onExistingResourcesShouldAddToOneRelationship() throws Exception {
		// GIVEN
		Document newTaskBody = new Document();
		newTaskBody.setData(createTask());

		JsonPath taskPath = pathBuilder.buildPath("/tasks");
		ResourcePost resourcePost = new ResourcePost(resourceRegistry, typeParser, OBJECT_MAPPER);

		// WHEN -- adding a task
		Response taskResponse = resourcePost.handle(taskPath, new QueryParamsAdapter(REQUEST_PARAMS), null, newTaskBody);

		// THEN
		assertThat(taskResponse.getDocument().getSingleData().getType()).isEqualTo("tasks");
		Long taskId = Long.parseLong(taskResponse.getDocument().getSingleData().getId());
		assertThat(taskId).isNotNull();

		/* ------- */

		// GIVEN
		Document newProjectBody = new Document();
		newProjectBody.setData(createProject());

		JsonPath projectPath = pathBuilder.buildPath("/projects");

		// WHEN -- adding a project
		Response projectResponse = resourcePost.handle(projectPath, new QueryParamsAdapter(REQUEST_PARAMS), null, newProjectBody);

		// THEN
		assertThat(projectResponse.getDocument().getSingleData().getType()).isEqualTo("projects");
		assertThat(projectResponse.getDocument().getSingleData().getId()).isNotNull();
		assertThat(projectResponse.getDocument().getSingleData().getAttributes().get("name").asText())
				.isEqualTo("sample project");
		Long projectId = Long.parseLong(projectResponse.getDocument().getSingleData().getId());
		assertThat(projectId).isNotNull();

		/* ------- */

		// GIVEN
		Document newTaskToProjectBody = new Document();
		newTaskToProjectBody.setData(createProject(Long.toString(projectId)));

		JsonPath savedTaskPath = pathBuilder.buildPath("/tasks/" + taskId + "/relationships/project");
		RelationshipsResourcePost sut = new RelationshipsResourcePost(resourceRegistry, typeParser);

		// WHEN -- adding a relation between task and project
		Response projectRelationshipResponse = sut.handle(savedTaskPath, new QueryParamsAdapter(REQUEST_PARAMS), null,
				newTaskToProjectBody);
		assertThat(projectRelationshipResponse).isNotNull();

		// THEN
		TaskToProjectRepository taskToProjectRepository = new TaskToProjectRepository();
		Project project = taskToProjectRepository.findOneTarget(taskId, "project", REQUEST_PARAMS);
		assertThat(project.getId()).isEqualTo(projectId);
	}

	@Test
	public void onExistingResourcesShouldAddToManyRelationship() throws Exception {
		// GIVEN
		Document newUserBody = new Document();
		Resource data = createUser();
		newUserBody.setData(data);

		JsonPath taskPath = pathBuilder.buildPath("/users");
		ResourcePost resourcePost = new ResourcePost(resourceRegistry, typeParser, OBJECT_MAPPER);

		// WHEN -- adding a user
		Response taskResponse = resourcePost.handle(taskPath, new QueryParamsAdapter(REQUEST_PARAMS), null, newUserBody);

		// THEN
		assertThat(taskResponse.getDocument().getSingleData().getType()).isEqualTo("users");
		Long userId = Long.parseLong(taskResponse.getDocument().getSingleData().getId());
		assertThat(userId).isNotNull();

		/* ------- */

		// GIVEN
		Document newProjectBody = new Document();
		data = createProject();
		newProjectBody.setData(data);
		data.setType("projects");

		JsonPath projectPath = pathBuilder.buildPath("/projects");

		// WHEN -- adding a project
		Response projectResponse = resourcePost.handle(projectPath, new QueryParamsAdapter(REQUEST_PARAMS), null, newProjectBody);

		// THEN
		assertThat(projectResponse.getDocument().getSingleData().getType()).isEqualTo("projects");
		assertThat(projectResponse.getDocument().getSingleData().getId()).isNotNull();
		assertThat(projectResponse.getDocument().getSingleData().getAttributes().get("name").asText())
				.isEqualTo("sample project");
		Long projectId = Long.parseLong(projectResponse.getDocument().getSingleData().getId());
		assertThat(projectId).isNotNull();

		/* ------- */

		// GIVEN
		Document newTaskToProjectBody = new Document();
		data = new Resource();
		newTaskToProjectBody.setData(Collections.singletonList(data));
		data.setType("projects");
		data.setId(projectId.toString());

		JsonPath savedTaskPath = pathBuilder.buildPath("/users/" + userId + "/relationships/assignedProjects");
		RelationshipsResourcePost sut = new RelationshipsResourcePost(resourceRegistry, typeParser);

		// WHEN -- adding a relation between user and project
		Response projectRelationshipResponse = sut.handle(savedTaskPath, new QueryParamsAdapter(REQUEST_PARAMS), null,
				newTaskToProjectBody);
		assertThat(projectRelationshipResponse).isNotNull();

		// THEN
		UserToProjectRepository userToProjectRepository = new UserToProjectRepository();
		Project project = userToProjectRepository.findOneTarget(userId, "assignedProjects", new QuerySpec(Project.class));
		assertThat(project.getId()).isEqualTo(projectId);
	}

	@Test
	public void onDeletingToOneRelationshipShouldSetTheValue() throws Exception {
		// GIVEN
		Document newTaskBody = new Document();
		Resource data = createTask();
		newTaskBody.setData(data);

		JsonPath taskPath = pathBuilder.buildPath("/tasks");
		ResourcePost resourcePost = new ResourcePost(resourceRegistry, typeParser, OBJECT_MAPPER);

		// WHEN -- adding a task
		Response taskResponse = resourcePost.handle(taskPath, new QueryParamsAdapter(REQUEST_PARAMS), null, newTaskBody);

		// THEN
		assertThat(taskResponse.getDocument().getSingleData().getType()).isEqualTo("tasks");
		Long taskId = Long.parseLong(taskResponse.getDocument().getSingleData().getId());
		assertThat(taskId).isNotNull();

		/* ------- */

		// GIVEN
		Document newTaskToProjectBody = new Document();
		newTaskToProjectBody.setData(null);

		JsonPath savedTaskPath = pathBuilder.buildPath("/tasks/" + taskId + "/relationships/project");
		RelationshipsResourcePost sut = new RelationshipsResourcePost(resourceRegistry, typeParser);

		// WHEN -- adding a relation between user and project
		Response projectRelationshipResponse = sut.handle(savedTaskPath, new QueryParamsAdapter(REQUEST_PARAMS), null,
				newTaskToProjectBody);
		assertThat(projectRelationshipResponse).isNotNull();

		// THEN
		assertThat(projectRelationshipResponse.getHttpStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
		Project project = localUserToProjectRepository.findOneTarget(1L, "project", new QuerySpec(Project.class));
		assertThat(project).isNull();
	}

	@Test
	public void supportPolymorphicRelationshipTypes() {

		// GIVEN
		Document newTaskBody = new Document();
		Resource data = createTask();
		newTaskBody.setData(data);

		JsonPath taskPath = pathBuilder.buildPath("/tasks");

		ResourcePost resourcePost = new ResourcePost(resourceRegistry, typeParser, objectMapper);
		Response taskResponse = resourcePost.handle(taskPath, new QueryParamsAdapter(REQUEST_PARAMS), null, newTaskBody);
		assertThat(taskResponse.getDocument().getSingleData().getType()).isEqualTo("tasks");
		Long taskIdOne = Long.parseLong(taskResponse.getDocument().getSingleData().getId());
		assertThat(taskIdOne).isNotNull();
		taskResponse = resourcePost.handle(taskPath, new QueryParamsAdapter(REQUEST_PARAMS), null, newTaskBody);
		Long taskIdTwo = Long.parseLong(taskResponse.getDocument().getSingleData().getId());
		assertThat(taskIdOne).isNotNull();
		taskResponse = resourcePost.handle(taskPath, new QueryParamsAdapter(REQUEST_PARAMS), null, newTaskBody);
		Long taskIdThree = Long.parseLong(taskResponse.getDocument().getSingleData().getId());
		assertThat(taskIdOne).isNotNull();

		// Create ProjectPolymorphic object
		Document newProjectBody = new Document();
		data = new Resource();
		String type = ClassUtils.getAnnotation(ProjectPolymorphic.class, JsonApiResource.class).get().type();
		data.setType(type);
		data.getRelationships().put("task", new Relationship(new ResourceId(taskIdOne.toString(), "tasks")));
		data.getRelationships().put("tasks", new Relationship(
				Arrays.asList(new ResourceId(taskIdTwo.toString(), "tasks"), new ResourceId(taskIdThree.toString(), "tasks"))));
		newProjectBody.setData(data);
		JsonPath projectPolymorphicTypePath = pathBuilder.buildPath("/" + type);

		// WHEN
		Response projectResponse = resourcePost.handle(projectPolymorphicTypePath, new QueryParamsAdapter(REQUEST_PARAMS), null,
				newProjectBody);

		// THEN
		assertThat(projectResponse.getDocument().getSingleData().getType()).isEqualTo("projects-polymorphic");
		Long projectId = Long.parseLong(projectResponse.getDocument().getSingleData().getId());
		assertThat(projectId).isNotNull();
		Resource projectPolymorphic = projectResponse.getDocument().getSingleData();
		assertNotNull(projectPolymorphic.getRelationships().get("task").getSingleData());
		assertNotNull(projectPolymorphic.getRelationships().get("tasks").getCollectionData());
		assertEquals(2, projectPolymorphic.getRelationships().get("tasks").getCollectionData().size());

	}
}
