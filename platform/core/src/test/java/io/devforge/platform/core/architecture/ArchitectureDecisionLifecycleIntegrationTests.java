package io.devforge.platform.core.architecture;
import io.devforge.platform.core.project.ProjectLifecycleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
@SpringBootTest @ActiveProfiles("test") @Transactional
class ArchitectureDecisionLifecycleIntegrationTests {
 @Autowired private ProjectLifecycleService projects;
 @Autowired private ArchitectureDecisionService decisions;
 @Test void createsDraftAndEnforcesAuditedTransitions()throws Exception{
  var project=projects.create("Billing","Billing platform");projects.createSnapshot(project.id(),spec(project.id()));
  var draft=decisions.createDraft(project.id(),1);assertThat(draft.status()).isEqualTo("DRAFT");assertThat(draft.history()).hasSize(1);
  var proposed=decisions.transition(project.id(),draft.id(),"PROPOSED","Ready for review");assertThat(proposed.status()).isEqualTo("PROPOSED");
  var accepted=decisions.transition(project.id(),draft.id(),"ACCEPTED","Reviewed and accepted");assertThat(accepted.status()).isEqualTo("ACCEPTED");assertThat(accepted.history()).hasSize(3);
  assertThatThrownBy(()->decisions.transition(project.id(),draft.id(),"PROPOSED","Reopen")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
 }
 @Test void requiresRationaleWhenRejecting()throws Exception{
  var project=projects.create("Identity","Identity platform");projects.createSnapshot(project.id(),spec(project.id()));
  var draft=decisions.createDraft(project.id(),1);
  assertThatThrownBy(()->decisions.transition(project.id(),draft.id(),"REJECTED"," ")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
 }
 private String spec(UUID id){return """
 { "specVersion":"1.0.0","project":{"id":"%s","name":"Billing","description":"Billing platform"},
 "requirements":{"functional":[],"qualityAttributes":[{"attribute":"maintainability","priority":"high"}],"constraints":[]},
 "model":{"entities":[],"relationships":[],"boundedContexts":[]},"architecture":{"style":"layered","decisions":[]},
 "technologySelections":{},"generation":{"profile":"recommended","outputLayout":"single-project"} }
 """.formatted(id);}
}
