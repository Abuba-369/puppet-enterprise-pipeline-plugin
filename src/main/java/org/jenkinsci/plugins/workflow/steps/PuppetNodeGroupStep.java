package org.jenkinsci.plugins.workflow.steps;

import java.util.*;
import com.google.inject.Inject;
import hudson.Extension;
import hudson.Util;
import hudson.util.ListBoxModel;
import hudson.security.ACL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.apache.commons.lang.StringUtils;
import hudson.model.Run;
import hudson.model.Item;
import hudson.model.TaskListener;
import java.net.*;
import jenkins.model.Jenkins;
import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.plugins.plaincredentials.*;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.AncestorInPath;
import java.io.Serializable;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.google.gson.internal.LinkedTreeMap;

import org.jenkinsci.plugins.puppetenterprise.PuppetEnterpriseManagement;
import org.jenkinsci.plugins.puppetenterprise.models.PuppetNodeGroup;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetnodemanagerv1.NodeManagerException;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetnodemanagerv1.NodeGroupV1;
import org.jenkinsci.plugins.puppetenterprise.models.PEException;

public final class PuppetNodeGroupStep extends PuppetEnterpriseStep implements Serializable {

  private String name = null;
  private String environment = null;
  private Boolean environmentTrumps = null;
  private ArrayList<Object> rule = null;
  private HashMap classes = null;
  private HashMap variables = null;
  private Boolean delete = false;

  @DataBoundSetter private void setName(String name) {
    this.name = name;
  }

  @DataBoundSetter private void setDelete(Boolean deletel) {
    this.delete = delete;
  }

  @DataBoundSetter private void setEnvironment(String environment) {
    this.environment = environment;
  }

  @DataBoundSetter private void setEnvironmentTrumps(Boolean environmentTrumps) {
    this.environmentTrumps = environmentTrumps;
  }

  @DataBoundSetter private void setRule(ArrayList<Object> rule) {
    this.rule = rule;
  }

  @DataBoundSetter private void setClasses(HashMap classes) {
    this.classes = classes;
  }

  @DataBoundSetter private void setVariables(HashMap variables) {
    this.variables = variables;
  }

  public String getName() {
    return this.name;
  }

  public String getEnvironment() {
    return this.environment;
  }

  public Boolean getEnvironmentTrumps() {
    return this.environmentTrumps;
  }

  public ArrayList<Object> getRule() {
    return this.rule;
  }

  public HashMap getClasses() {
    return this.classes;
  }

  public HashMap getVariables() {
    return this.variables;
  }

  public Boolean getDelete() {
    return this.delete;
  }

  @DataBoundConstructor public PuppetNodeGroupStep() { }

  public static class PuppetNodeGroupStepExecution extends AbstractSynchronousStepExecution<Void> {

    @Inject private transient PuppetNodeGroupStep step;
    @StepContextParameter private transient Run<?, ?> run;
    @StepContextParameter private transient TaskListener listener;

    @Override protected Void run() throws Exception {
      PuppetNodeGroup group = new PuppetNodeGroup();
      group.setName(step.getName());
      group.setEnvironment(step.getEnvironment());
      group.setEnvironmentTrumps(step.getEnvironmentTrumps());
      group.setClasses(step.getClasses());
      group.setVariables(step.getVariables());
      group.setRule(step.getRule());

      String summary = "";

      try {
        group.setToken(step.getToken());
      } catch(java.lang.NullPointerException e) {
        summary = "Could not find Jenkins credential with ID: " + step.getCredentialsId() + "\n";
        StringBuilder message = new StringBuilder();

        message.append(summary);
        message.append("Please ensure the credentials exist in Jenkins. Note, the credentials description is not its ID\n");

        listener.getLogger().println(message.toString());
        throw new PEException(summary);
      }

      try {
        if (step.getDelete()) {
          group.delete();
        } else {
          group.set();
        }
      } catch(NodeManagerException e) {
        StringBuilder message = new StringBuilder();
        message.append("Puppet Node Manager Error\n");
        message.append("Kind:    " + e.getKind() + "\n");
        message.append("Message: " + e.getMessage() + "\n");

        throw new PEException(message.toString(), listener);
      }

      listener.getLogger().println("Node Group " + step.getName() + " successful.");

      return null;
    }

    private static final long serialVersionUID = 1L;
  }

  @Extension public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
    public DescriptorImpl() {
      super(PuppetNodeGroupStepExecution.class);
    }

    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context, @QueryParameter String source) {
      if (context == null || !context.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      return new StandardListBoxModel().withEmptySelection().withAll(
      CredentialsProvider.lookupCredentials(StringCredentials.class, context, ACL.SYSTEM, URIRequirementBuilder.fromUri(source).build()));
    }

    @Override public String getFunctionName() {
      return "puppetNodeGroup";
    }

    @Override public String getDisplayName() {
      return "Manage Node Groups";
    }
  }
}