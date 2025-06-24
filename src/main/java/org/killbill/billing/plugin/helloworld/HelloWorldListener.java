/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2020 The Billing Project, LLC
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.catalog.api.PlanPhasePriceOverride;
import org.killbill.billing.catalog.api.PlanPhaseSpecifier;
import org.killbill.billing.catalog.api.TierPriceOverride;
import org.killbill.billing.catalog.api.TieredBlockPriceOverride;
import org.killbill.billing.catalog.api.UsagePriceOverride;
import org.killbill.billing.catalog.api.UsageType;
import org.killbill.billing.catalog.api.boilerplate.PlanPhasePriceOverrideImp;
import org.killbill.billing.catalog.api.boilerplate.TierPriceOverrideImp;
import org.killbill.billing.catalog.api.boilerplate.TieredBlockPriceOverrideImp;
import org.killbill.billing.catalog.api.boilerplate.UsagePriceOverrideImp;
import org.killbill.billing.entitlement.api.BaseEntitlementWithAddOnsSpecifier;
import org.killbill.billing.entitlement.api.Entitlement;
import org.killbill.billing.entitlement.api.EntitlementApiException;
import org.killbill.billing.entitlement.api.EntitlementSpecifier;
import org.killbill.billing.entitlement.api.boilerplate.BaseEntitlementWithAddOnsSpecifierImp;
import org.killbill.billing.invoice.plugin.api.InvoiceFormatterFactory;
import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillClock;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher;
import org.killbill.billing.plugin.api.PluginCallContext;
import org.killbill.billing.plugin.api.PluginTenantContext;
import org.killbill.billing.plugin.api.core.PluginEntitlementSpecifier;
import org.killbill.billing.plugin.api.core.PluginPlanPhasePriceOverride;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloWorldListener implements OSGIKillbillEventDispatcher.OSGIKillbillEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldListener.class);

    private final OSGIKillbillAPI osgiKillbillAPI;

    private final ServiceTracker<InvoiceFormatterFactory, InvoiceFormatterFactory> invoiceFormatterTracker;

    private final Properties configProperties;

    final OSGIKillbillClock clock;

    public HelloWorldListener(final OSGIKillbillAPI killbillAPI, final ServiceTracker<InvoiceFormatterFactory, InvoiceFormatterFactory> invoiceFormatterTracker, final Properties configProperties, final OSGIKillbillClock clock) {
        this.osgiKillbillAPI = killbillAPI;
        this.invoiceFormatterTracker = invoiceFormatterTracker;
        this.configProperties = configProperties;
        this.clock = clock;
    }

    private static final String defaultLocale = "en_US";

    @Override
    public void handleKillbillEvent(final ExtBusEvent killbillEvent) {
        logger.info("Received event {} for object id {} of type {}",
                    killbillEvent.getEventType(),
                    killbillEvent.getObjectId(),
                    killbillEvent.getObjectType());

        final TenantContext context = new PluginTenantContext(killbillEvent.getAccountId(), killbillEvent.getTenantId());
        final CallContext callContext = new PluginCallContext("hello-world-plugin", clock.getClock().getUTCNow(), killbillEvent.getAccountId(), killbillEvent.getTenantId());
        switch (killbillEvent.getEventType()) {
            //
            // Handle ACCOUNT_CREATION and ACCOUNT_CHANGE only for demo purpose and just print the account
            //
            case ACCOUNT_CREATION:
                try {
                    final Account account = osgiKillbillAPI.getAccountUserApi().getAccountById(killbillEvent.getAccountId(), context);
                    logger.info("Account information: " + account);
                    createSubscription(account, callContext);
                } catch (final AccountApiException e) {
                    logger.warn("Unable to find account", e);
                } catch (final EntitlementApiException e) {
                    logger.warn("Unable to create entitelment", e);
                }
                break;

            // Nothing
            default:
                break;

        }
    }

    private void createSubscription(final Account account, final CallContext callContext) throws EntitlementApiException {

        //base
        final EntitlementSpecifier baseSpec = new PluginEntitlementSpecifier.Builder().withPlanPhaseSpecifier(new PlanPhaseSpecifier("standard-monthly-in-arrear")).build();

        //addon with recurring price override of 1.60 and usage price override of 1.25
        TieredBlockPriceOverride tieredBlockPriceOverride = new TieredBlockPriceOverrideImp.Builder<>().withUnitName("bullets").withSize(new BigDecimal(1)).withPrice(new BigDecimal(1.25)).withCurrency(Currency.USD).withMax(new BigDecimal(1000)).build();
        TierPriceOverride tierPriceOverride = new TierPriceOverrideImp.Builder<>().withTieredBlockPriceOverrides(List.of(tieredBlockPriceOverride)).build();
        UsagePriceOverride usagePriceOverride = new UsagePriceOverrideImp.Builder<>().withName("ao3-usage").withUsageType(UsageType.CONSUMABLE).withTierPriceOverrides(List.of(tierPriceOverride)).build();
        PlanPhasePriceOverride addonOverride = new PlanPhasePriceOverrideImp.Builder<>().withPhaseName("ao3-in-arrear-evergreen").withRecurringPrice(new BigDecimal("1.60")).withUsagePriceOverrides(List.of(usagePriceOverride)).withCurrency(Currency.USD).build();
        final EntitlementSpecifier addonSpec = new PluginEntitlementSpecifier.Builder().withPlanPhaseSpecifier(new PlanPhaseSpecifier("ao3-in-arrear")).withOverrides(List.of(addonOverride)).build();

        //bundle
        final BaseEntitlementWithAddOnsSpecifier cartSpecifier = new BaseEntitlementWithAddOnsSpecifierImp.Builder<>().withEntitlementSpecifier(List.of(baseSpec, addonSpec)).build();

        osgiKillbillAPI.getSecurityApi().login("admin", "password");
        //create bundle
        final List<UUID> allEntitlements = osgiKillbillAPI.getEntitlementApi().createBaseEntitlementsWithAddOns(account.getId(), List.of(cartSpecifier), true, Collections.emptyList(), callContext);

        //Comment the code below and  Retrieve addon subscription via https://killbill.github.io/slate/subscription.html#retrieve-a-subscription-by-id
        // verify that the prices section lists the overridden price: 1.6 recurring and 1.25 usage
//        "prices": [
//        {
//            "planName": "ao3-in-arrear-51",
//                "phaseName": "ao3-in-arrear-51-evergreen",
//                "phaseType": "EVERGREEN",
//                "fixedPrice": null,
//                "recurringPrice": 1.6,
//                "usagePrices": [
//            {
//                "usageName": "ao3-usage",
//                    "usageType": "CONSUMABLE",
//                    "billingMode": "IN_ARREAR",
//                    "tierBlockPolicy": "ALL_TIERS",
//                    "tierPrices": [
//                {
//                    "blockPrices": [
//                    {
//                        "unitName": "bullets",
//                            "size": 1,
//                            "price": 1.25,
//                            "max": 1000
//                    }
//              ]
//                }
//          ]
//            }
//      ]
//        }
//  ],


        //schedule plan change for addon one month from now
        final UUID addonEntId = allEntitlements.get(1);
        final Entitlement addonEnt = osgiKillbillAPI.getEntitlementApi().getEntitlementForId(addonEntId, false, callContext);

        //plan change with recurring price override of 1.75 and usage price override of 1.35
        tieredBlockPriceOverride = new TieredBlockPriceOverrideImp.Builder<>().withUnitName("bullets").withSize(new BigDecimal(1)).withPrice(new BigDecimal(1.35)).withCurrency(Currency.USD).withMax(new BigDecimal(1000)).build();
        tierPriceOverride = new TierPriceOverrideImp.Builder<>().withTieredBlockPriceOverrides(List.of(tieredBlockPriceOverride)).build();
        usagePriceOverride = new UsagePriceOverrideImp.Builder<>().withName("ao3-usage").withUsageType(UsageType.CONSUMABLE).withTierPriceOverrides(List.of(tierPriceOverride)).build();
        addonOverride = new PlanPhasePriceOverrideImp.Builder<>().withPhaseName("ao3-in-arrear-evergreen").withRecurringPrice(new BigDecimal("1.75")).withUsagePriceOverrides(List.of(usagePriceOverride)).withCurrency(Currency.USD).build();

        final EntitlementSpecifier addonSpec2 = new PluginEntitlementSpecifier.Builder().withPlanPhaseSpecifier(new PlanPhaseSpecifier("ao3-in-arrear")).withOverrides(List.of(addonOverride)).build();
        addonEnt.changePlanWithDate(addonSpec2, clock.getClock().getUTCToday().plusMonths(1), Collections.emptyList(), callContext);

        //Retrieve addon subscription via https://killbill.github.io/slate/subscription.html#retrieve-a-subscription-by-id and
        // verify that the prices section lists the overridden price
        //This show 2 recurring prices: 1.60 and 1.75 and 2 usage prices 1.25 and 1.35:

//        "prices": [
//        {
//            "planName": "ao3-in-arrear-51",
//                "phaseName": "ao3-in-arrear-51-evergreen",
//                "phaseType": "EVERGREEN",
//                "fixedPrice": null,
//                "recurringPrice": 1.6,
//                "usagePrices": [
//            {
//                "usageName": "ao3-usage",
//                    "usageType": "CONSUMABLE",
//                    "billingMode": "IN_ARREAR",
//                    "tierBlockPolicy": "ALL_TIERS",
//                    "tierPrices": [
//                {
//                    "blockPrices": [
//                    {
//                        "unitName": "bullets",
//                            "size": 1,
//                            "price": 1.25,
//                            "max": 1000
//                    }
//              ]
//                }
//          ]
//            }
//      ]
//        },
//        {
//            "planName": "ao3-in-arrear-53",
//                "phaseName": "ao3-in-arrear-53-evergreen",
//                "phaseType": "EVERGREEN",
//                "fixedPrice": null,
//                "recurringPrice": 1.75,
//                "usagePrices": [
//            {
//                "usageName": "ao3-usage",
//                    "usageType": "CONSUMABLE",
//                    "billingMode": "IN_ARREAR",
//                    "tierBlockPolicy": "ALL_TIERS",
//                    "tierPrices": [
//                {
//                    "blockPrices": [
//                    {
//                        "unitName": "bullets",
//                            "size": 1,
//                            "price": 1.35,
//                            "max": 1000
//                    }
//              ]
//                }
//          ]
//            }
//      ]
//        }
//  ]
        osgiKillbillAPI.getSecurityApi().logout();

    }

}
