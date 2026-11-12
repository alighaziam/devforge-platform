package io.devforge.platform.core.quality;
import java.util.List;
public record QualityTestCase(String id,String category,String title,String objective,String priority,
 String sourcePath,List<String> steps,String expectedResult) {
 public QualityTestCase { steps=List.copyOf(steps); }
}
