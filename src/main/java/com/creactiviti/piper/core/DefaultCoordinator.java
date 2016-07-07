package com.creactiviti.piper.core;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class DefaultCoordinator implements Coordinator {

  @Autowired private Messenger messenger;
  @Autowired private PipelineRepository pipelineRepository;
  @Autowired private JobRepository jobRepository;
  @Autowired private ApplicationEventPublisher eventPublisher;
  @Autowired private ContextRepository contextRepository;
 
  private Logger log = LoggerFactory.getLogger(getClass());
  
  @Override
  public Job start (String aPipelineId, Map<String, Object> aParameters) {
    Assert.notNull(aPipelineId,"pipelineId must not be null");
    
    Pipeline pipeline = pipelineRepository.findOne(aPipelineId);
    Assert.notNull(pipeline,String.format("Unkown pipeline: %s", aPipelineId));
    
    SimpleJob job = new SimpleJob(pipeline);
    job.setStatus(JobStatus.STARTED);
    log.debug("Job {} started",job.getId());
    jobRepository.save(job);
    
    Context context = new MutableContext(job.getId(), aParameters);
    contextRepository.save(context);
    
    run(job);
    
    return job;
  }
  
  private void run (Job aJob) {
    if(aJob.hasMoreTasks()) {
      Task nextTask = jobRepository.nextTask(aJob);
      messenger.send(nextTask.getNode(), nextTask);
    }
    else {
      jobRepository.updateStatus (aJob, JobStatus.COMPLETED);
      jobRepository.save(aJob);
      log.debug("Job {} completed successfully",aJob.getId());
    }
  }

  @Override
  public Job stop (String aJobId) {
    return null;
  }

  @Override
  public Job resume (String aJobId) {
    return null;
  }

  @Override
  public void complete (Task aTask) {
    log.debug("Completing task {}", aTask.getId());
    MutableTask task = new MutableTask(aTask);
    task.setStatus(TaskStatus.COMPLETED);
    Job job = jobRepository.findByTaskId (aTask.getId());
    Assert.notNull(job,String.format("No job found for task %s ",aTask.getId()));
    run(job);
  }

  @Override
  public void error (Task aTask) {
  }

  @Override
  public void on (Object aEvent) {
    eventPublisher.publishEvent (aEvent);    
  }

}
