/*
 * Copyright 2020-2024 Equinix, Inc
 * Copyright 2014-2024 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.helloworld;

import java.util.UUID;

import org.killbill.billing.entitlement.api.BaseEntitlementWithAddOnsSpecifier;
import org.killbill.billing.entitlement.api.EntitlementSpecifier;
import org.killbill.billing.entitlement.api.Subscription;
import org.killbill.billing.entitlement.api.SubscriptionApiException;
import org.killbill.billing.entitlement.plugin.api.EntitlementContext;
import org.killbill.billing.entitlement.plugin.api.EntitlementPluginApi;
import org.killbill.billing.entitlement.plugin.api.EntitlementPluginApiException;
import org.killbill.billing.entitlement.plugin.api.OnFailureEntitlementResult;
import org.killbill.billing.entitlement.plugin.api.OnSuccessEntitlementResult;
import org.killbill.billing.entitlement.plugin.api.PriorEntitlementResult;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.payment.api.PluginProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloWorldEntitlementPluginApi implements EntitlementPluginApi {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldEntitlementPluginApi.class);
    private final OSGIKillbillAPI killbillAPI;

    public HelloWorldEntitlementPluginApi(final OSGIKillbillAPI killbillAPI) {
        this.killbillAPI = killbillAPI;
    }


    @Override
    public PriorEntitlementResult priorCall(final EntitlementContext context, final Iterable<PluginProperty> properties) throws EntitlementPluginApiException {
        return null;
    }

    @Override
    public OnSuccessEntitlementResult onSuccessCall(final EntitlementContext context, final Iterable<PluginProperty> properties) throws EntitlementPluginApiException {
//        final UUID subId = getSubscriptionId(context);
//        final Subscription sub = killbillAPI.getSubscriptionApi().getSubscriptionForExternalKey(spec.getExternalKey(), false, context);
//        sub.
        return null;
    }

    @Override
    public OnFailureEntitlementResult onFailureCall(final EntitlementContext context, final Iterable<PluginProperty> properties) throws EntitlementPluginApiException {
        return null;
    }

    private UUID getSubscriptionId(final EntitlementContext context) {

        if (context.getBaseEntitlementWithAddOnsSpecifiers().iterator().hasNext()) {
            final BaseEntitlementWithAddOnsSpecifier bundleSpec = context.getBaseEntitlementWithAddOnsSpecifiers().iterator().next();
            if (bundleSpec.getEntitlementSpecifier().iterator().hasNext()) {
                final EntitlementSpecifier spec = bundleSpec.getEntitlementSpecifier().iterator().next();
                try {
                    final Subscription sub = killbillAPI.getSubscriptionApi().getSubscriptionForExternalKey(spec.getExternalKey(), false, context);
                    return sub.getId();
                } catch (final SubscriptionApiException e) {
                    logger.warn("Failed to get subscription for key {}", spec.getExternalKey(), e);
                }
            }
        }
        return null;
    }
}
