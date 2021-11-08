/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */
package com.l7tech.custom.delaythreadsafe;

import com.l7tech.policy.assertion.ext.CustomAssertionStatus;
import com.l7tech.policy.assertion.ext.ServiceInvocation;
import com.l7tech.policy.assertion.ext.message.CustomPolicyContext;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.concurrent.TimeUnit.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.ScheduledFuture;

public class DelayServiceInvocation extends ServiceInvocation {
    private static final Logger logger = Logger.getLogger(DelayAssertion.class.getName());
    

    @Override
    public CustomAssertionStatus checkRequest(final CustomPolicyContext policyContext) {
        
        if (!(customAssertion instanceof DelayAssertion)) {
            logger.log(Level.SEVERE,
                    "customAssertion must be of type [{%s}], but it is of type [{%s}] instead",
                    new Object[]{
                            DelayAssertion.class.getSimpleName(),
                            customAssertion.getClass().getSimpleName()});

            auditWarn(String.format("customAssertion must be of type [{%s}], but it is of type [{%s}] instead",
                    DelayAssertion.class.getSimpleName(), customAssertion.getClass().getSimpleName()));

            return CustomAssertionStatus.FAILED;
        }
        
        final DelayAssertion delayAssertion = (DelayAssertion) customAssertion;

        final String[] variablesUsed = delayAssertion.getVariablesUsed();

        // any invalid context variables will lead to blank delayMilliSec value which getValidDelayOrNone will fail.
        final Map<String, Object> vars = policyContext.getVariableMap(variablesUsed);
        final String delayMilliSec = policyContext.expandVariable(delayAssertion.getDelayMilliSec(), vars);
        final Optional<Long> delay = DelayAssertion.getValidDelayOrNone(delayMilliSec);
        if (delay.isPresent()) {
            try {
                final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                Callable<String> wait = new Callable<String>() {
                    public String call() {
                        return "done";
                    }
                };
                auditWarn("Wait " + scheduler.schedule(wait, delay.get(), MILLISECONDS).get());
                scheduler.shutdown();
            } catch (Exception e) {
                auditWarn("Exception caught: " + e.getMessage());
                return CustomAssertionStatus.FALSIFIED;
            }
        } else {
            return CustomAssertionStatus.FALSIFIED;
        }
        return CustomAssertionStatus.NONE;
    }
}

