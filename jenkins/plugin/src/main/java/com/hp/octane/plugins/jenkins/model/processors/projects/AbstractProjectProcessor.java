package com.hp.octane.plugins.jenkins.model.processors.projects;

import com.hp.octane.plugins.jenkins.model.pipelines.StructurePhase;
import com.hp.octane.plugins.jenkins.model.processors.builders.AbstractBuilderProcessor;
import com.hp.octane.plugins.jenkins.model.processors.builders.BuildTriggerProcessor;
import com.hp.octane.plugins.jenkins.model.processors.builders.MultiJobBuilderProcessor;
import com.hp.octane.plugins.jenkins.model.processors.builders.ParameterizedTriggerProcessor;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStep;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import org.jenkinsci.plugins.conditionalbuildstep.ConditionalBuilder;
import org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: gullery
 * Date: 09/01/15
 * Time: 00:59
 * To change this template use File | Settings | File Templates.
 */

public abstract class AbstractProjectProcessor {
	private static final Logger logger = Logger.getLogger(AbstractProjectProcessor.class.getName());

	private List<StructurePhase> internals = new ArrayList<StructurePhase>();
	private List<StructurePhase> postBuilds = new ArrayList<StructurePhase>();

	protected void processBuilders(List<Builder> builders, AbstractProject project) {
		this.processBuilders(builders, project, "");
	}

	protected void processBuilders(List<Builder> builders, AbstractProject project, String phasesName) {
		for (Builder builder : builders) {
			builderClassValidator(builder, project, phasesName);
		}
	}


	protected void builderClassValidator(Builder builder,AbstractProject project, String phasesName) {
		AbstractBuilderProcessor builderProcessor = null;
		if (builder.getClass().getName().equals("org.jenkinsci.plugins.conditionalbuildstep.ConditionalBuilder"))
		{
			ConditionalBuilder conditionalBuilder = (ConditionalBuilder)builder;
			for(BuildStep currentBuildStep : conditionalBuilder.getConditionalbuilders()) {
				builderClassValidator((Builder) currentBuildStep, project, phasesName);
			}
		}
		else if (builder.getClass().getName().equals("org.jenkinsci.plugins.conditionalbuildstep.singlestep.SingleConditionalBuilder")) {
			SingleConditionalBuilder singleConditionalBuilder = (SingleConditionalBuilder)builder;
			 builderClassValidator((Builder)singleConditionalBuilder.getBuildStep(), project, phasesName);
		}
		else if (builder.getClass().getName().equals("hudson.plugins.parameterizedtrigger.TriggerBuilder"))
		{
			builderProcessor = new ParameterizedTriggerProcessor(builder, project, phasesName);
		}
		else if (builder.getClass().getName().equals("com.tikal.jenkins.plugins.multijob.MultiJobBuilder"))
		{
			builderProcessor = new MultiJobBuilderProcessor(builder);
		}

		if(builderProcessor != null) {
			internals.addAll(builderProcessor.getPhases());
		}else {
			logger.info("not yet supported build (internal) action: " + builder.getClass().getName());
		}
	}



	@SuppressWarnings("unchecked")
	protected void processPublishers(AbstractProject project) {
		AbstractBuilderProcessor builderProcessor;
		List<Publisher> publishers = project.getPublishersList();
		for (Publisher publisher : publishers) {
			builderProcessor = null;
			if (publisher.getClass().getName().equals("hudson.tasks.BuildTrigger")) {
				builderProcessor = new BuildTriggerProcessor(publisher, project);
			} else if (publisher.getClass().getName().equals("hudson.plugins.parameterizedtrigger.BuildTrigger")) {
				builderProcessor = new ParameterizedTriggerProcessor(publisher, project, "");
			}
			if (builderProcessor != null) {
				postBuilds.addAll(builderProcessor.getPhases());
			} else {
				logger.info("not yet supported publisher (post build) action: " + publisher.getClass().getName());
			}
		}
	}

	public StructurePhase[] getInternals() {
		return internals.toArray(new StructurePhase[internals.size()]);
	}

	public StructurePhase[] getPostBuilds() {
		return postBuilds.toArray(new StructurePhase[postBuilds.size()]);
	}

	public static AbstractProjectProcessor getFlowProcessor(AbstractProject project) {
		AbstractProjectProcessor flowProcessor = null;
		if (project.getClass().getName().equals("hudson.model.FreeStyleProject")) {
			flowProcessor = new FreeStyleProjectProcessor(project);
		} else if (project.getClass().getName().equals("hudson.matrix.MatrixProject")) {
			flowProcessor = new MatrixProjectProcessor(project);
		} else if (project.getClass().getName().equals("hudson.maven.MavenModuleSet")) {
			flowProcessor = new MavenProjectProcessor(project);
		} else if (project.getClass().getName().equals("com.tikal.jenkins.plugins.multijob.MultiJobProject")) {
			flowProcessor = new MultiJobProjectProcessor(project);
		} else {
			flowProcessor = new UnsupportedProjectProcessor();
		}
		return flowProcessor;
	}
}
