package io.devforge.platform.core.quality;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
public record QualityPlanResult(UUID projectId,UUID snapshotId,int snapshotVersion,Instant generatedAt,
 Map<String,Integer> summary,List<QualityTestCase> testCases) {
 public QualityPlanResult { summary=Map.copyOf(summary);testCases=List.copyOf(testCases); }
}
