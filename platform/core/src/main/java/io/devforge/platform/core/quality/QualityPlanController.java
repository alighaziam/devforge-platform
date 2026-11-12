package io.devforge.platform.core.quality;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/projects/{projectId}/quality")
public class QualityPlanController {
 private final QualityPlanService plans;
 public QualityPlanController(QualityPlanService plans){this.plans=plans;}
 @PostMapping("/plan") public QualityPlanResult create(@PathVariable UUID projectId,@RequestBody PlanRequest request)throws JsonProcessingException {
  return plans.createPlan(projectId,request.snapshotVersion());
 }
 public record PlanRequest(int snapshotVersion){}
}
