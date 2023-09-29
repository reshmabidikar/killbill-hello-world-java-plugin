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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.joda.time.DateTime;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.catalog.api.CatalogApiException;
import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher;
import org.killbill.billing.plugin.api.PluginCallContext;
import org.killbill.billing.plugin.api.PluginTenantContext;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.ConfigException.IO;

public class HelloWorldListener implements OSGIKillbillEventDispatcher.OSGIKillbillEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldListener.class);

    private final OSGIKillbillAPI osgiKillbillAPI;

    public HelloWorldListener(final OSGIKillbillAPI killbillAPI) {
        this.osgiKillbillAPI = killbillAPI;
    }

    @Override
    public void handleKillbillEvent(final ExtBusEvent killbillEvent) {
        logger.info("Received event {} for object id {} of type {}",
                    killbillEvent.getEventType(),
                    killbillEvent.getObjectId(),
                    killbillEvent.getObjectType());

        final TenantContext context = new PluginTenantContext(killbillEvent.getAccountId(), killbillEvent.getTenantId());
        switch (killbillEvent.getEventType()) {
            //
            // Handle ACCOUNT_CREATION and ACCOUNT_CHANGE only for demo purpose and just print the account
            //
            case ACCOUNT_CREATION:
                logger.info("Trying to upload catalog");
                final CallContext callContext = new PluginCallContext(HelloWorldActivator.PLUGIN_NAME, DateTime.now(), killbillEvent.getAccountId(), killbillEvent.getTenantId());
                try {
                    osgiKillbillAPI.getSecurityApi().login("admin", "password");
                    osgiKillbillAPI.getCatalogUserApi().uploadCatalog(getCatalogXml(), callContext);
                } catch (final CatalogApiException e) {
                    logger.warn("Error in uploading catalog", e);
                } finally {
                    osgiKillbillAPI.getSecurityApi().logout();
                }

            case ACCOUNT_CHANGE:
                try {
                    final Account account = osgiKillbillAPI.getAccountUserApi().getAccountById(killbillEvent.getAccountId(), context);
                    logger.info("Account information: " + account);
                } catch (final AccountApiException e) {
                    logger.warn("Unable to find account", e);
                }
                break;

            // Nothing
            default:
                break;

        }
    }

    public String getCatalogXml() {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                           "<catalog xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                           "         xsi:noNamespaceSchemaLocation=\"https://docs.killbill.io/latest/catalog.xsd\">\n" +
                           "    <effectiveDate>2020-01-01T00:00:00+00:00</effectiveDate>\n" +
                           "    <catalogName>ExampleCatalog</catalogName>\n" +
                           "    <recurringBillingMode>IN_ADVANCE</recurringBillingMode>\n" +
                           "    <currencies>\n" +
                           "        <currency>USD</currency>\n" +
                           "    </currencies>\n" +
                           "    <products>\n" +
                           "        <product name=\"Standard\">\n" +
                           "            <category>BASE</category>\n" +
                           "        </product>\n" +
                           "    </products>\n" +
                           "    <rules>\n" +
                           "        <changePolicy>\n" +
                           "            <changePolicyCase>\n" +
                           "                <policy>END_OF_TERM</policy>\n" +
                           "            </changePolicyCase>\n" +
                           "        </changePolicy>\n" +
                           "        <cancelPolicy>\n" +
                           "            <cancelPolicyCase>\n" +
                           "                <policy>END_OF_TERM</policy>\n" +
                           "            </cancelPolicyCase>\n" +
                           "        </cancelPolicy>\n" +
                           "    </rules>\n" +
                           "    <plans>\n" +
                           "        <plan name=\"standard-monthly\">\n" +
                           "            <product>Standard</product>\n" +
                           "            <initialPhases>\n" +
                           "            </initialPhases>\n" +
                           "            <finalPhase type=\"EVERGREEN\">\n" +
                           "                <duration>\n" +
                           "                    <unit>UNLIMITED</unit>\n" +
                           "                </duration>\n" +
                           "                <recurring>\n" +
                           "                    <billingPeriod>MONTHLY</billingPeriod>\n" +
                           "                    <recurringPrice>\n" +
                           "                        <price>\n" +
                           "                            <currency>USD</currency>\n" +
                           "                            <value>30</value>\n" +
                           "                        </price>\n" +
                           "                    </recurringPrice>\n" +
                           "                </recurring>\n" +
                           "            </finalPhase>\n" +
                           "        </plan>\n" +
                           "    </plans>\n" +
                           "    <priceLists>\n" +
                           "        <defaultPriceList name=\"DEFAULT\">\n" +
                           "            <plans>\n" +
                           "                <plan>standard-monthly</plan>\n" +
                           "            </plans>\n" +
                           "        </defaultPriceList>\n" +
                           "    </priceLists>\n" +
                           "</catalog>";
        return xml;
    }

}
