/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.commerce.discount.internal;

import com.liferay.commerce.context.CommerceContext;
import com.liferay.commerce.currency.model.CommerceCurrency;
import com.liferay.commerce.currency.model.CommerceMoney;
import com.liferay.commerce.currency.model.CommerceMoneyFactory;
import com.liferay.commerce.discount.CommerceDiscountCalculation;
import com.liferay.commerce.discount.CommerceDiscountValue;
import com.liferay.commerce.discount.internal.search.CommerceDiscountIndexer;
import com.liferay.commerce.discount.model.CommerceDiscount;
import com.liferay.commerce.discount.model.CommerceDiscountRule;
import com.liferay.commerce.discount.rule.type.CommerceDiscountRuleType;
import com.liferay.commerce.discount.rule.type.CommerceDiscountRuleTypeRegistry;
import com.liferay.commerce.discount.service.CommerceDiscountLocalService;
import com.liferay.commerce.discount.service.CommerceDiscountRuleLocalService;
import com.liferay.commerce.discount.target.CommerceDiscountTarget.Type;
import com.liferay.commerce.model.CommerceOrder;
import com.liferay.commerce.product.model.CPInstance;
import com.liferay.commerce.product.service.CPInstanceLocalService;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.search.BaseModelSearchResult;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.QueryConfig;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import java.io.Serializable;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Marco Leo
 */
@Component(service = CommerceDiscountCalculation.class)
public class CommerceDiscountCalculationImpl
	implements CommerceDiscountCalculation {

	@Override
	public CommerceDiscountValue getOrderShippingCommerceDiscountValue(
			CommerceOrder commerceOrder, CommerceContext commerceContext)
		throws PortalException {

		if (commerceOrder == null) {
			return null;
		}

		SearchContext searchContext = buildSearchContext(
			commerceOrder.getCompanyId(), commerceOrder.getSiteGroupId(), 0, 0,
			commerceOrder.getCommerceOrderId(),
			commerceContext.getCommerceUserSegmentEntryIds(),
			commerceContext.getCouponCode(), Type.APPLY_TO_SHIPPING);

		return _getCommerceDiscountValue(
			commerceOrder.getShippingAmount(), commerceContext, searchContext);
	}

	@Override
	public CommerceDiscountValue getOrderSubtotalCommerceDiscountValue(
			CommerceOrder commerceOrder, CommerceContext commerceContext)
		throws PortalException {

		if (commerceOrder == null) {
			return null;
		}

		SearchContext searchContext = buildSearchContext(
			commerceOrder.getCompanyId(), commerceOrder.getSiteGroupId(), 0, 0,
			commerceOrder.getCommerceOrderId(),
			commerceContext.getCommerceUserSegmentEntryIds(),
			commerceContext.getCouponCode(), Type.APPLY_TO_SUBTOTAL);

		return _getCommerceDiscountValue(
			commerceOrder.getSubtotal(), commerceContext, searchContext);
	}

	@Override
	public CommerceDiscountValue getOrderTotalCommerceDiscountValue(
			CommerceOrder commerceOrder, CommerceContext commerceContext)
		throws PortalException {

		if (commerceOrder == null) {
			return null;
		}

		SearchContext searchContext = buildSearchContext(
			commerceOrder.getCompanyId(), commerceOrder.getSiteGroupId(), 0, 0,
			commerceOrder.getCommerceOrderId(),
			commerceContext.getCommerceUserSegmentEntryIds(),
			commerceContext.getCouponCode(), Type.APPLY_TO_TOTAL);

		return _getCommerceDiscountValue(
			commerceOrder.getTotal(), commerceContext, searchContext);
	}

	@Override
	public CommerceDiscountValue getProductCommerceDiscountValue(
			long cpInstanceId, int quantity, BigDecimal productUnitPrice,
			CommerceContext commerceContext)
		throws PortalException {

		CPInstance cpInstance = _cpInstanceLocalService.getCPInstance(
			cpInstanceId);

		SearchContext searchContext = buildSearchContext(
			cpInstance.getCompanyId(), cpInstance.getGroupId(),
			cpInstance.getCPDefinitionId(), cpInstanceId, 0,
			commerceContext.getCommerceUserSegmentEntryIds(),
			commerceContext.getCouponCode(), Type.APPLY_TO_PRODUCT);

		return _getCommerceDiscountValue(
			productUnitPrice, commerceContext, searchContext);
	}

	protected SearchContext buildSearchContext(
		long companyId, long groupId, long cpDefinitionId, long cpInstanceId,
		long commerceOrderId, long[] commerceUserSegmentEntryIds,
		String couponCode, Type commerceDiscountTargetType) {

		SearchContext searchContext = new SearchContext();

		Map<String, Serializable> attributes = new HashMap<>();

		attributes.put(Field.STATUS, WorkflowConstants.STATUS_APPROVED);
		attributes.put(CommerceDiscountIndexer.FIELD_ACTIVE, true);
		attributes.put(CommerceDiscountIndexer.FIELD_COUPON_CODE, couponCode);
		attributes.put(
			CommerceDiscountIndexer.FIELD_TARGET_TYPE,
			commerceDiscountTargetType.toString());
		attributes.put("commerceOrderId", commerceOrderId);
		attributes.put(
			"commerceUserSegmentEntryIds", commerceUserSegmentEntryIds);
		attributes.put("cpDefinitionId", cpDefinitionId);
		attributes.put("cpInstanceId", cpInstanceId);

		searchContext.setAttributes(attributes);

		searchContext.setCompanyId(companyId);
		searchContext.setStart(QueryUtil.ALL_POS);
		searchContext.setEnd(QueryUtil.ALL_POS);
		searchContext.setGroupIds(new long[] {groupId});

		QueryConfig queryConfig = searchContext.getQueryConfig();

		queryConfig.setHighlightEnabled(false);
		queryConfig.setScoreEnabled(false);

		return searchContext;
	}

	private CommerceDiscountValue _getCommerceDiscountValue(
			BigDecimal amount, CommerceContext commerceContext,
			SearchContext searchContext)
		throws PortalException {

		BaseModelSearchResult<CommerceDiscount> baseModelSearchResult =
			_commerceDiscountLocalService.searchCommerceDiscounts(
				searchContext);

		List<CommerceDiscountValue> commerceDiscountValues = new ArrayList<>();

		CommerceCurrency commerceCurrency =
			commerceContext.getCommerceCurrency();

		for (CommerceDiscount commerceDiscount :
				baseModelSearchResult.getBaseModels()) {

			if (_isValidDiscount(commerceContext, commerceDiscount)) {
				commerceDiscountValues.add(
					_getCommerceDiscountValue(
						commerceDiscount, amount, commerceCurrency));
			}
		}

		BigDecimal currentDiscountAmount = BigDecimal.ZERO;

		CommerceDiscountValue selectedDiscount = null;

		for (CommerceDiscountValue commerceDiscountValue :
				commerceDiscountValues) {

			CommerceMoney discountAmount =
				commerceDiscountValue.getDiscountAmount();

			if (currentDiscountAmount.compareTo(discountAmount.getPrice()) <
					0) {

				currentDiscountAmount = discountAmount.getPrice();
				selectedDiscount = commerceDiscountValue;
			}
		}

		return selectedDiscount;
	}

	private CommerceDiscountValue _getCommerceDiscountValue(
		CommerceDiscount commerceDiscount, BigDecimal amount,
		CommerceCurrency commerceCurrency) {

		BigDecimal[] values = new BigDecimal[4];

		if (commerceDiscount.isUsePercentage()) {
			values[0] = commerceDiscount.getLevel1();
			values[1] = commerceDiscount.getLevel2();
			values[2] = commerceDiscount.getLevel3();
			values[3] = commerceDiscount.getLevel4();
		}

		BigDecimal currentDiscountAmount = BigDecimal.ZERO;

		BigDecimal discountedAmount = amount;

		if (commerceDiscount.isUsePercentage()) {
			currentDiscountAmount = _getDiscountAmount(
				discountedAmount, commerceDiscount.getLevel1());

			discountedAmount = discountedAmount.subtract(currentDiscountAmount);

			currentDiscountAmount = _getDiscountAmount(
				discountedAmount, commerceDiscount.getLevel2());

			discountedAmount = discountedAmount.subtract(currentDiscountAmount);

			currentDiscountAmount = _getDiscountAmount(
				discountedAmount, commerceDiscount.getLevel3());

			discountedAmount = discountedAmount.subtract(currentDiscountAmount);

			currentDiscountAmount = amount.subtract(discountedAmount);

			BigDecimal maximumDiscountAmount =
				commerceDiscount.getMaximumDiscountAmount();

			if ((maximumDiscountAmount.compareTo(BigDecimal.ZERO) > 0) &&
				(currentDiscountAmount.compareTo(maximumDiscountAmount) > 0)) {

				currentDiscountAmount =
					commerceDiscount.getMaximumDiscountAmount();
			}
		}
		else {
			currentDiscountAmount = commerceDiscount.getLevel1();

			discountedAmount = discountedAmount.subtract(currentDiscountAmount);
		}

		BigDecimal discountPercentage = discountedAmount.divide(
			amount, RoundingMode.valueOf(commerceCurrency.getRoundingMode()));

		discountPercentage = discountPercentage.multiply(_ONE_HUNDRED);

		CommerceMoney discountAmount = _commerceMoneyFactory.create(
			commerceCurrency, currentDiscountAmount);

		return new CommerceDiscountValue(
			commerceDiscount.getCommerceDiscountId(), discountAmount,
			_ONE_HUNDRED.subtract(discountPercentage), values);
	}

	private BigDecimal _getDiscountAmount(
		BigDecimal amount, BigDecimal percentage) {

		if (percentage == null) {
			return BigDecimal.ZERO;
		}

		BigDecimal discountedAmount = amount.multiply(percentage);

		return discountedAmount.divide(_ONE_HUNDRED);
	}

	private boolean _isValidDiscount(
			CommerceContext commerceContext, CommerceDiscount commerceDiscount)
		throws PortalException {

		List<CommerceDiscountRule> commerceDiscountRules =
			_commerceDiscountRuleLocalService.getCommerceDiscountRules(
				commerceDiscount.getCommerceDiscountId(), QueryUtil.ALL_POS,
				QueryUtil.ALL_POS, null);

		for (CommerceDiscountRule commerceDiscountRule :
				commerceDiscountRules) {

			CommerceDiscountRuleType commerceDiscountRuleType =
				_commerceDiscountRuleTypeRegistry.getCommerceDiscountRuleType(
					commerceDiscountRule.getType());

			if (!commerceDiscountRuleType.evaluate(
					commerceDiscountRule, commerceContext)) {

				return false;
			}
		}

		return true;
	}

	private static final BigDecimal _ONE_HUNDRED = BigDecimal.valueOf(100);

	@Reference
	private CommerceDiscountLocalService _commerceDiscountLocalService;

	@Reference
	private CommerceDiscountRuleLocalService _commerceDiscountRuleLocalService;

	@Reference
	private CommerceDiscountRuleTypeRegistry _commerceDiscountRuleTypeRegistry;

	@Reference
	private CommerceMoneyFactory _commerceMoneyFactory;

	@Reference
	private CPInstanceLocalService _cpInstanceLocalService;

}