package com.hp.octane.plugins.jenkins.bridge;

import com.google.inject.Inject;
import com.hp.octane.plugins.jenkins.client.JenkinsMqmRestClientFactory;
import com.hp.octane.plugins.jenkins.client.JenkinsMqmRestClientFactoryImpl;
import com.hp.octane.plugins.jenkins.configuration.ConfigurationListener;
import com.hp.octane.plugins.jenkins.configuration.ServerConfiguration;
import hudson.Extension;
import jenkins.model.Jenkins;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by gullery on 05/08/2015.
 * <p/>
 * Bridge Service meant to provide an abridged connectivity functionality
 * The only APIs to be exposed is the basic management of abridged clients
 */

@Extension
public class BridgesService implements ConfigurationListener {
	private static final Logger logger = Logger.getLogger(BridgesService.class.getName());
	private static final Object bridgeLocker = new Object();

	private static BridgesService extensionInstance;
	private JenkinsMqmRestClientFactory clientFactory;
	private BridgeClient bridgeClient;

	public static BridgesService getExtensionInstance() {
		if (extensionInstance == null) {
			List<BridgesService> extensions = Jenkins.getInstance().getExtensionList(BridgesService.class);
			if (extensions.isEmpty()) {
				throw new RuntimeException("BRIDGE: bridge service was not initialized properly");
			} else if (extensions.size() > 1) {
				throw new RuntimeException("BRIDGE: bridge service expected to be singleton, found " + extensions.size() + " instances");
			} else {
				extensionInstance = extensions.get(0);
			}
		}
		return extensionInstance;
	}

	public void updateBridge(ServerConfiguration conf) {
		boolean existing = false;
		if (conf == null || conf.password == null ||
				conf.location == null || conf.location.equals("") ||
				conf.sharedSpace == null || conf.sharedSpace.equals("") ||
				conf.username == null || conf.username.equals("")) {
			logger.warning("BRIDGE: bad configuration encountered, bridge will not be " + (bridgeClient == null ? "created" : "updated"));
		} else {
			synchronized (bridgeLocker) {
				if (bridgeClient != null) {
					bridgeClient.update(conf);
				} else {
					bridgeClient = new BridgeClient(conf, clientFactory);
				}
			}
		}
	}

	@Inject
	public void setMqmRestClientFactory(JenkinsMqmRestClientFactoryImpl clientFactory) {
		this.clientFactory = clientFactory;
	}

	@Override
	public void onChanged(ServerConfiguration conf, ServerConfiguration oldConf) {
		updateBridge(conf);
	}
}
