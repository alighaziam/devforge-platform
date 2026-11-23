package io.devforge.platform.core.model;

import io.devforge.platform.core.project.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModelImportServiceTests {

    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final SqlDdlModelImporter sqlImporter = mock(SqlDdlModelImporter.class);
    private final OpenApiModelImporter openApiImporter = mock(OpenApiModelImporter.class);
    private final ModelImportService service = new ModelImportService(projects, sqlImporter, openApiImporter);

    @Test
    void dispatchesOpenApiFormatAndPreservesImporterWarnings() throws Exception {
        UUID projectId = UUID.randomUUID();
        String document = "{\"openapi\":\"3.1.0\"}";
        Map<String, Object> model = Map.of("entities", List.of(Map.of("name", "Customer", "attributes", List.of())),
                "relationships", List.of(), "indexes", List.of());
        when(projects.existsById(projectId)).thenReturn(true);
        when(openApiImporter.importOpenApi(document))
                .thenReturn(new OpenApiModelImporter.ImportResult(model, List.of("OpenAPI paths were not imported.")));

        ModelImportResult result = service.importModel(projectId, "openapi", document);

        assertThat(result.model()).isEqualTo(model);
        assertThat(result.warnings()).contains(
                "Import is a preview only; review and merge the model into the Project Specification before saving.",
                "OpenAPI paths were not imported.");
    }

    @Test
    void rejectsUnknownProjectsOversizedPayloadsAndUnsupportedFormats() {
        UUID projectId = UUID.randomUUID();
        when(projects.existsById(projectId)).thenReturn(false);
        assertThatThrownBy(() -> service.importModel(projectId, "openapi", "{}"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        UUID existingProject = UUID.randomUUID();
        when(projects.existsById(existingProject)).thenReturn(true);
        String oversized = "x".repeat(524_289);
        assertThatThrownBy(() -> service.importModel(existingProject, "openapi", oversized))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);

        assertThatThrownBy(() -> service.importModel(existingProject, "yaml", "{}"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
