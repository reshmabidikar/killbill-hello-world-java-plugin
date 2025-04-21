/*
 * Copyright 2020-2022 Equinix, Inc
 * Copyright 2014-2022 The Billing Project, LLC
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.invoice.api.DryRunArguments;
import org.killbill.billing.invoice.api.DryRunType;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.invoice.api.InvoiceApiException;
import org.killbill.billing.invoice.api.InvoiceItem;
import org.killbill.billing.invoice.api.InvoiceItemType;
import org.killbill.billing.invoice.api.boilerplate.DryRunArgumentsImp;
import org.killbill.billing.invoice.plugin.api.AdditionalItemsResult;
import org.killbill.billing.invoice.plugin.api.InvoiceContext;
import org.killbill.billing.invoice.plugin.api.PriorInvoiceResult;
import org.killbill.billing.invoice.plugin.api.boilerplate.plugin.InvoiceContextImp;
import org.killbill.billing.invoice.plugin.api.boilerplate.plugin.PriorInvoiceResultImp;
import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.osgi.libs.killbill.OSGIConfigPropertiesService;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;
import org.killbill.billing.payment.api.PluginProperty;
import org.killbill.billing.plugin.api.invoice.PluginInvoiceItem;
import org.killbill.billing.plugin.api.invoice.PluginInvoicePluginApi;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.clock.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HelloWorldInvoicePluginApi extends PluginInvoicePluginApi implements OSGIKillbillEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldInvoicePluginApi.class);
    private final OSGIKillbillAPI osgiKillbillAPI;

    public HelloWorldInvoicePluginApi(final OSGIKillbillAPI killbillAPI, final OSGIConfigPropertiesService configProperties,
                                      final Clock clock) {
        super(killbillAPI, configProperties, clock);
        this.osgiKillbillAPI = killbillAPI;
    }
    @Override
    public PriorInvoiceResult priorCall(final InvoiceContext context, final Iterable<PluginProperty> properties) {

        final PriorInvoiceResultImp.Builder builder = new PriorInvoiceResultImp.Builder();
        //final DryRunArguments dryRunArgs = new DryRunArgumentsImp.Builder().withDryRunType(DryRunType.UPCOMING_INVOICE).build();
        final DryRunArguments dryRunArgs = new DryRunArgumentsImp.Builder().withDryRunType(DryRunType.TARGET_DATE).build();
        try {
            if (!context.isDryRun()) {
                osgiKillbillAPI.getSecurityApi().login("admin", "password");
                final InvoiceContext context2 = new InvoiceContextImp.Builder<>().withInvoice(context.getInvoice()).withExistingInvoices(context.getExistingInvoices()).withAccountId(context.getAccountId()).withComments(context.getComments()).withIsRescheduled(true).withCallOrigin(context.getCallOrigin()).withCreatedDate(context.getCreatedDate()).withIsDryRun(true).withReasonCode(context.getReasonCode()).withTargetDate(context.getTargetDate()).withTenantId(context.getTenantId()).withUpdatedDate(context.getUpdatedDate()).withUserName(context.getUserName()).withUserType(context.getUserType()).withUserToken(context.getUserToken()).build();
                //final Invoice upcomingInvoice = osgiKillbillAPI.getInvoiceUserApi().triggerDryRunInvoiceGeneration(context.getAccountId(), null, dryRunArgs, Collections.emptyList(), context2);
                final Invoice upcomingInvoice = osgiKillbillAPI.getInvoiceUserApi().triggerDryRunInvoiceGeneration(context.getAccountId(), context.getTargetDate(), dryRunArgs, Collections.emptyList(), context2);
                //osgiKillbillAPI.getSecurityApi().logout();
                if (upcomingInvoice != null) { // upcoming invoice is present, abort payment
                    BigDecimal balance = upcomingInvoice.getBalance();
                    logger.info("upcoming invoice is present with balance {}", balance);
                    if(balance.compareTo(BigDecimal.TEN) <= 0){
                        logger.info("balance {} is less than 10, aborting invoice generation ", balance);
                        builder.withIsAborted(true);
                    }
                    //if required, we can have an additional check here for invoice balance

                }
            }
        } catch (final InvoiceApiException e) {
            // no upcoming invoice so generate invoice as per date, do nothing
        }
        return builder.build();
    }


    /**
     * Returns additional invoice items to be added to invoice
     * <p>
     * This method produces two types of invoice items a. Tax Item on Invoice Item
     * b. Adjustment Item on only the very first Historical Invoice Tax Item
     * automatically ( just for Demo purpose )
     *
     * @param newInvoice     The invoice that is being created.
     * @param dryRun         Whether it is dryRun or not
     * @param properties     Any user-specified plugin properties, coming straight out
     *                       of the API request that has triggered this code to run.
     * @param invoiceContext The context in which this code is running.
     * @return A new immutable list of new tax items, or adjustments on existing tax
     * items.
     */
    @Override
    public AdditionalItemsResult getAdditionalInvoiceItems(final Invoice newInvoice, final boolean dryRun,
                                                           final Iterable<PluginProperty> properties, final InvoiceContext invoiceContext) {

        logger.info("Throwing exception from getAdditionalInvoiceItems");
        //        throw new InvoicePluginApiRetryException(List.of(Period.minutes(1),
        //                                                         Period.minutes(5)));
        final UUID accountId = newInvoice.getAccountId();
        final Account account = getAccount(accountId, invoiceContext);
        final Set<Invoice> allInvoices = getAllInvoicesOfAccount(account, newInvoice, invoiceContext);
        final List<InvoiceItem> additionalItems = new LinkedList<InvoiceItem>();

        // Creating tax item for first Item of new Invoice
        final List<InvoiceItem> newInvoiceItems = newInvoice.getInvoiceItems();
        final InvoiceItem newInvoiceItem = newInvoiceItems.get(0);
        BigDecimal charge = new BigDecimal("80");
        final InvoiceItem taxItem = PluginInvoiceItem.createTaxItem(newInvoiceItem, newInvoiceItem.getInvoiceId(),
                                                                    newInvoice.getInvoiceDate(), null, charge, "Tax Item");
        //additionalItems.add(taxItem);

        //        // Creating External Charge for first Item of new Invoice
        //        final InvoiceItem externalItem = PluginInvoiceItem.create(newInvoiceItem, newInvoiceItem.getInvoiceId(),
        //                                                                  newInvoice.getInvoiceDate(), null, charge, "External Item", InvoiceItemType.EXTERNAL_CHARGE);
        //        additionalItems.add(externalItem);

        // Adding adjustment invoice item to the first historical invoice, if it does not have the adjustment item
        //        for (final Invoice invoice : allInvoices) {
        //            if (!invoice.getId().equals(newInvoice.getId())) {
        //                final List<InvoiceItem> invoiceItems = invoice.getInvoiceItems();
        //                // Check for if any adjustment item exists for Historical Invoice
        //                if (checkforAdjustmentItem(invoiceItems)) {
        //                    break;
        //                }
        //                for (final InvoiceItem item : invoiceItems) {
        //                    charge = new BigDecimal("-30");
        //                    final InvoiceItem adjItem = PluginInvoiceItem.createAdjustmentItem(item, item.getInvoiceId(),
        //                                                                                       newInvoice.getInvoiceDate(), newInvoice.getInvoiceDate(), charge, "Adjustment Item");
        //                    additionalItems.add(adjItem);
        //                    break;
        //                }
        //                break;
        //            }
        //        }

        return new AdditionalItemsResult() {
            @Override
            public List<InvoiceItem> getAdditionalItems() {
                return additionalItems;
            }

            @Override
            public Iterable<PluginProperty> getAdjustedPluginProperties() {
                return null;
            }
        };
    }

    /**
     * This method returns all invoices of account
     *
     * @param account    The account to consider.
     * @param newInvoice New Invoice Item to be added to existing invoices of the
     *                   account
     * @param tenantCtx
     * @return All invoices of account
     */
    private Set<Invoice> getAllInvoicesOfAccount(final Account account, final Invoice newInvoice, final TenantContext tenantCtx) {
        final Set<Invoice> invoices = new HashSet<Invoice>();
        invoices.addAll(getInvoicesByAccountId(account.getId(), tenantCtx));
        invoices.add(newInvoice);
        return invoices;
    }

    /**
     * Check whether adjustment item is already present in invoice Item of Invoice
     *
     * @param invoiceItems
     * @return
     */
    private boolean checkforAdjustmentItem(final List<InvoiceItem> invoiceItems) {
        boolean adjustmentItemPresent = false;
        for (final InvoiceItem invoiceItem : invoiceItems) {
            if (invoiceItem.getInvoiceItemType().equals(InvoiceItemType.ITEM_ADJ)) {
                adjustmentItemPresent = true;
                break;
            }
        }
        return adjustmentItemPresent;
    }

    protected boolean isTaxItem(final InvoiceItem invoiceItem) {
        return InvoiceItemType.TAX.equals(invoiceItem.getInvoiceItemType());
    }

    @Override
    public void handleKillbillEvent(final ExtBusEvent killbillEvent) {
    }

}
