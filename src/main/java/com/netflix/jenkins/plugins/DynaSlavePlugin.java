package com.netflix.jenkins.plugins;

import hudson.Plugin;
import hudson.Util;
import hudson.model.Descriptor.FormException;
import hudson.model.Node;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.acegisecurity.context.SecurityContextHolder;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Exposes an entry point to add a new slave.
 * Based upon the Jenkins Swarm Plugin
 */
public class DynaSlavePlugin extends Plugin {
    private static final Logger LOG = Logger.getLogger(DynaSlavePlugin.class.getName());

    private String defaultPrefix = "dynaslave";
    private String defaultLabels = "";
    //private String defaultRemoteSlaveUser = "jenkins";
    private String defaultRemoteSlaveUser = "vagrant";
    private String defaultBaseLauncherCommand = "/apps/jenkins/tools/start-remote-dynaslave";
    private String defaultIdleTerminationMinutes = "30";

    public String getDefaultPrefix() {
        return defaultPrefix;
    }

    public String getDefaultLabels() {
        return defaultLabels;
    }

    public String getDefaultBaseLauncherCommand() {
        return defaultBaseLauncherCommand;
    }

    public String getDefaultRemoteSlaveUser() {
        return defaultRemoteSlaveUser;
    }

    public String getDefaultIdleTerminationMinutes() {
        return defaultIdleTerminationMinutes;
    }

    /**
     * Adds a new slave.
     */
    public void doCreateSlave(StaplerRequest req, StaplerResponse rsp, @QueryParameter String name,
                              @QueryParameter String description, @QueryParameter int executors,
                              @QueryParameter String remoteFsRoot, @QueryParameter String labels,
                              @QueryParameter String hostname)
            throws IOException, FormException {
        // bypass the regular security check
        SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
        try {
            final Jenkins jenkins = Jenkins.getInstance();

            if (defaultPrefix != null && !defaultPrefix.isEmpty()) {
                name = defaultPrefix + "-" + name;
            }

            if (description == null) {
                description = "";
            }

            labels = defaultLabels + " " + Util.fixNull(labels);

            //bug does not recogonize the global variable value
            System.out.println("#######################################################################");
            System.out.println("Going to register the dynamic slave node with Jenkins server");
            System.out.println("defaultPrefix = " + defaultPrefix );
            System.out.println("defaultLabels = " + defaultLabels );
            System.out.println("defaultRemoteSlaveUser = " + defaultRemoteSlaveUser );
            System.out.println("defaultBaseLauncherCommand = " + defaultBaseLauncherCommand );
            System.out.println("defaultIdleTerminationMinutes = " + defaultIdleTerminationMinutes );
            defaultRemoteSlaveUser = "vagrant";
            System.out.println("defaultRemoteSlaveUser: after the hack = " + defaultRemoteSlaveUser );
            System.out.println("#######################################################################");

            DynaSlave slave = new DynaSlave(name, "Dynamic slave at " + hostname + ": " + description,
                    remoteFsRoot, String.valueOf(executors), labels, hostname, defaultRemoteSlaveUser,
                    defaultBaseLauncherCommand, defaultIdleTerminationMinutes);

            synchronized (jenkins) {
                Node n = jenkins.getNode(name);
                if (n != null) jenkins.removeNode(n);
                jenkins.addNode(slave);
            }
        } catch (FormException e) {
            LOG.log(Level.WARNING, "Unable to create dynaslave:", e);

        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    public void configure(StaplerRequest req, JSONObject formData)
            throws FormException, ServletException, IOException {

        defaultPrefix = formData.optString("defaultPrefix");
        defaultLabels = formData.optString("defaultLabels");
        defaultRemoteSlaveUser = formData.optString("defaultRemoteSlaveUser");
        defaultBaseLauncherCommand = formData.optString("defaultBaseLauncherCommand");
        defaultIdleTerminationMinutes = formData.optString("defaultIdleTerminationMinutes");
        save();
    }

    @Override
    public void start() throws IOException {
        load();
    }
}
