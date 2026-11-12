package io.devforge.platform.core.architecture;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/projects/{projectId}/architecture/decisions")
public class ArchitectureDecisionController {
 private final ArchitectureDecisionService service;
 public ArchitectureDecisionController(ArchitectureDecisionService service){this.service=service;}
 @GetMapping public List<ArchitectureDecisionView> list(@PathVariable UUID projectId){return service.list(projectId);}
 @PostMapping public ArchitectureDecisionView create(@PathVariable UUID projectId,@RequestBody CreateRequest request)throws JsonProcessingException{return service.createDraft(projectId,request.snapshotVersion());}
 @PostMapping("/{decisionId}/transition") public ArchitectureDecisionView transition(@PathVariable UUID projectId,@PathVariable UUID decisionId,@RequestBody TransitionRequest request){return service.transition(projectId,decisionId,request.targetStatus(),request.comment());}
 public record CreateRequest(int snapshotVersion){}
 public record TransitionRequest(String targetStatus,String comment){}
}
