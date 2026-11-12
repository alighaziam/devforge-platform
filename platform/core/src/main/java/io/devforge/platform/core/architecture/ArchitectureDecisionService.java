package io.devforge.platform.core.architecture;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.devforge.platform.core.project.SpecificationSnapshotEntity;
import io.devforge.platform.core.project.SpecificationSnapshotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class ArchitectureDecisionService {
 private static final Map<String,Set<String>> TRANSITIONS=Map.of(
  "DRAFT",Set.of("PROPOSED","REJECTED"),"PROPOSED",Set.of("ACCEPTED","REJECTED","DRAFT"),
  "ACCEPTED",Set.of("SUPERSEDED"),"REJECTED",Set.of("DRAFT"),"SUPERSEDED",Set.of());
 private final SpecificationSnapshotRepository snapshots;
 private final ArchitectureDecisionRepository decisions;
 private final ArchitectureDecisionEventRepository events;
 private final ArchitectureAssessmentService assessments;
 private final ObjectMapper mapper=new ObjectMapper();
 public ArchitectureDecisionService(SpecificationSnapshotRepository s,ArchitectureDecisionRepository d,ArchitectureDecisionEventRepository e,ArchitectureAssessmentService a){snapshots=s;decisions=d;events=e;assessments=a;}

 @Transactional public ArchitectureDecisionView createDraft(UUID projectId,int version)throws JsonProcessingException{
  SpecificationSnapshotEntity snapshot=snapshots.findByProject_IdAndSnapshotVersion(projectId,version)
   .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Specification snapshot not found"));
  ArchitectureDecisionDraft draft=assessments.evaluate(projectId,version).adrDraft();
  Optional<ArchitectureDecisionEntity> existing=decisions.findBySnapshot_IdAndTitle(snapshot.getId(),draft.title());
  if(existing.isPresent())return view(existing.get());
  ArchitectureDecisionEntity entity=new ArchitectureDecisionEntity(UUID.randomUUID(),snapshot.getProject(),snapshot,draft.title(),draft.context(),draft.decision(),
   mapper.writeValueAsString(draft.alternatives()),mapper.writeValueAsString(draft.consequences()));
  decisions.saveAndFlush(entity);record(entity,null,"DRAFT","ADR draft generated from architecture assessment.");return view(entity);
 }
 @Transactional(readOnly=true) public List<ArchitectureDecisionView> list(UUID projectId){return decisions.findAllByProject_IdOrderByCreatedAtDesc(projectId).stream().map(this::view).toList();}
 @Transactional public ArchitectureDecisionView transition(UUID projectId,UUID decisionId,String targetStatus,String comment){
  String target=targetStatus==null?"":targetStatus.toUpperCase(Locale.ROOT);
  if(!TRANSITIONS.containsKey(target))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown ADR status: "+targetStatus);
  ArchitectureDecisionEntity entity=decisions.findByIdAndProject_Id(decisionId,projectId)
   .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Architecture decision not found"));
  String current=entity.getStatus();
  if(!TRANSITIONS.get(current).contains(target))throw new ResponseStatusException(HttpStatus.CONFLICT,"Invalid ADR transition: "+current+" -> "+target);
  String note=comment==null?"":comment.trim();
  if(("REJECTED".equals(target)||"SUPERSEDED".equals(target))&&note.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A comment is required for rejection or supersession.");
  if(note.length()>2000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"ADR comment exceeds 2000 characters.");
  entity.transitionTo(target);decisions.saveAndFlush(entity);record(entity,current,target,note);return view(entity);
 }
 private void record(ArchitectureDecisionEntity e,String from,String to,String comment){
  Authentication auth=SecurityContextHolder.getContext().getAuthentication();String actor=auth==null?"system":auth.getName();
  events.save(new ArchitectureDecisionEventEntity(UUID.randomUUID(),e,from,to,comment,actor));
 }
 private ArchitectureDecisionView view(ArchitectureDecisionEntity e){
  try{
   List<String> alternatives=mapper.readValue(e.getAlternativesJson(),mapper.getTypeFactory().constructCollectionType(List.class,String.class));
   List<String> consequences=mapper.readValue(e.getConsequencesJson(),mapper.getTypeFactory().constructCollectionType(List.class,String.class));
   List<ArchitectureDecisionEventView> history=events.findAllByDecision_IdOrderByCreatedAtAsc(e.getId()).stream().map(ArchitectureDecisionEventView::from).toList();
   return new ArchitectureDecisionView(e.getId(),e.getProject().getId(),e.getSnapshot().getId(),e.getSnapshot().getSnapshotVersion(),e.getTitle(),e.getContext(),e.getDecision(),alternatives,consequences,e.getStatus(),e.getVersion(),e.getCreatedAt(),e.getUpdatedAt(),history);
  }catch(JsonProcessingException ex){throw new IllegalStateException("Stored ADR content is invalid",ex);}
 }
}
