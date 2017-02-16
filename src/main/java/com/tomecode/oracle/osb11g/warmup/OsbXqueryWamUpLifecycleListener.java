package com.tomecode.oracle.osb11g.warmup;

import java.security.PrivilegedExceptionAction;

import com.bea.wli.config.ConfigService;
import com.bea.wli.config.spi.ResourceLifecycleListener;

import weblogic.application.ApplicationLifecycleEvent;
import weblogic.application.ApplicationLifecycleListener;
import weblogic.logging.NonCatalogLogger;

/**
 * Application listener
 * 
 * @author Tome
 *
 */
public final class OsbXqueryWamUpLifecycleListener extends ApplicationLifecycleListener {

	private static final NonCatalogLogger logger = new NonCatalogLogger("OsbXqueryWarmUpListener");

	public final void postStart(ApplicationLifecycleEvent evt) {

		try {
			weblogic.security.Security.runAs(weblogic.security.SubjectUtils.getAnonymousUser(), new PrivilegedExceptionAction<Object>() {
				public Object run() throws Exception {

					ConfigService configService = ConfigService.getConfigService("XBus Kernel");
					boolean exists = false;
					for (ResourceLifecycleListener listener : configService.getResourceLifecycleListeners()) {
						if (listener instanceof XqueryWarmUp) {
							exists = true;
						}
					}
					if (!exists) {
						// register new listener
						configService.registerResourceLifecycleListener(new XqueryWarmUp());
						logger.info("OSB XQuery Warm-Up was registered in OSB!");
					}
					return null;
				}
			});
		} catch (Exception e) {
			logger.error("Faile registed OSB XQuery Warm-Up listener in OSB, reason: " + e.getMessage(), e);
		}
	}
}
